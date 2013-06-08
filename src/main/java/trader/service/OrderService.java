package trader.service;

import static org.apache.commons.lang.StringUtils.isBlank;
import static trader.constants.Constants.CONTRACT_CURRENCY_USD;
import static trader.constants.Constants.CONTRACT_SEC_TYPE_FUTURE;
import static trader.constants.Constants.FUTURES_MONTHS;
import static trader.constants.Constants.ORDER_ACTION_BUY;
import static trader.constants.Constants.ORDER_ACTION_SELL;
import static trader.constants.Constants.ORDER_TIF_GTC;
import static trader.constants.Constants.ORDER_TYPE_MKT;
import static trader.constants.Constants.ORDER_TYPE_STOP;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang.math.Fraction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import trader.domain.ActionItem;
import trader.domain.ExtOrder;
import trader.domain.Position;

import com.ib.client.Contract;
import com.ib.client.Order;

@Service
public class OrderService {
	
	private static final String		FIELD_SEPARATOR 			= ",";
	private static final Integer	FIELD_INDEX_ORDER_TYPE		= 0; 
	private static final Integer	FIELD_INDEX_QUANTITY		= 1;
	private static final Integer	FIELD_INDEX_BROKER_SYMBOL	= 3;
	private static final Integer	FIELD_INDEX_MONTH			= 4;
	private static final Integer	FIELD_INDEX_ORDER_PRICE		= 6;
	private static final Integer	FIELD_INDEX_ROLL_INFO		= 7;
//	private static final Integer	FIELD_INDEX_POSITION		= 12;
//	private static final Integer	FIELD_INDEX_LAST_DATE 		= 15;
	
	// Orders file field values
	private static final String		ORDER_TYPE_LONG_ENTRY		= "Long Entry";
	private static final String		ORDER_TYPE_LONG_EXIT		= "Long Exit";
	private static final String		ORDER_TYPE_SHORT_ENTRY		= "Short Entry";
	private static final String		ORDER_TYPE_SHORT_EXIT		= "Short Exit";
	
	private static final OrderComparator	orderComparator 	= new OrderComparator();
	
    protected final Log logger = LogFactory.getLog(getClass());
    
    private Map<Integer, ExtOrder> openOrders = new HashMap<Integer, ExtOrder>();
    private Map<String, Position> openPositions = new HashMap<String, Position>();
    
    private List<ExtOrder> generatedOrders = new ArrayList<ExtOrder>();
    private Set<String> rollovers = new HashSet<String>();
    
    private List<ActionItem> actionItems = new ArrayList<ActionItem>();
    
    @Resource
    private ContractService contractService;
    
    @Resource
    private TwsApiService twsApiService;

	/**
	 * Import orders from file
	 * @param file
	 * @param account
	 * @throws IOException 
	 */
	public List<ActionItem> importOrders(MultipartFile file, String account) throws IOException {
		
		actionItems.clear();
		
		readOrdersFile(file, account);
		List<ExtOrder> openOrders = new ArrayList<ExtOrder>(this.openOrders.values());
		
		// New orders = generated minus open
		List<ExtOrder> newOrders = new ArrayList<ExtOrder>(generatedOrders);
		newOrders.removeAll(openOrders);
		
		// Cancelled orders = open minus generated
		List<ExtOrder> cancelledOrders = new ArrayList<ExtOrder>(openOrders);
		cancelledOrders.removeAll(generatedOrders);
		
		// Combine and sort orders
		List<ExtOrder> updatedOrders = new ArrayList<ExtOrder>(newOrders);
		updatedOrders.addAll(cancelledOrders);
		Collections.sort(updatedOrders, orderComparator);
				
		// Build list of action items
		for (ExtOrder order : updatedOrders) {
			ActionItem actionItem = new ActionItem();
			
			Contract contract = order.getContract();
			String symbol = contract.m_symbol;
			String expiry = contract.m_expiry;
			boolean isNewOrder = (order.m_orderId == 0);
			
			String action = "Cancel order to ";
			if (isNewOrder) {
				action = "Place order to ";
			}
			
			// Handle rollovers
			if (rollovers.contains(symbol) && isNewOrder) {
				ActionItem rollover = new ActionItem();
				rollover.setDescription("Rollover " + symbol + " to " + expiry);

				Position position = openPositions.get(symbol);
				ExtOrder rolloverExit = createRolloverExit(position.getContract(), position.getQuantity(), account);
				ExtOrder rolloverEntry = createRolloverEntry(contract, position.getQuantity(), account);

				rollover.addOrder(rolloverExit);
				rollover.addOrder(rolloverEntry);
				
				actionItems.add(rollover);
				
				rollovers.remove(symbol);
			}
			
			actionItem.setDescription(action + order.m_action + " " + order.m_totalQuantity 
					+ " " + symbol + " " + expiry + " " + " @ " + order.m_auxPrice);
			
			actionItem.addOrder(order);
			
			actionItems.add(actionItem);
		}
		return actionItems;
	}
	
	/**
	 * For each action item referenced by an actionItemIndices key,
	 * if the value is true, process the action item's orders. 
	 * If value is false, remove the action item from the list.
	 * 
	 * When processing orders, if the order has a non-zero orderId, 
	 * it is an existing order to be cancelled.  If the orderId is 0, 
	 * it is a new order to be placed. 
	 * 
	 * @param actionItemIndices
	 */
	public void processActionItems(Map<Integer, Boolean> actionItemIndices) {
	
		List<ActionItem> processedActionItems = new ArrayList<ActionItem>(); 
		
		for (Entry<Integer, Boolean> entry : actionItemIndices.entrySet()) {
			
			int actionItemIndex = entry.getKey();
			boolean isExecute = entry.getValue();
			ActionItem actionItem = actionItems.get(actionItemIndex);
			
			if (isExecute) {
				for (ExtOrder order : actionItem.getOrders()) {
					if (order.m_orderId == 0) {
						// This is a new order...place it
						twsApiService.placeOrder(order.getContract(), order);
					} else {
						// This is an existing order...cancel it
						twsApiService.cancelOrder(order.getContract(), order);
					}
				}
			}
			processedActionItems.add(actionItem);
		}
		
		actionItems.removeAll(processedActionItems);
	}
	
	/**
	 * Reads records from file to generate a list of ContractOrder objects
	 * @param file
	 * @param account
	 * @return
	 * @throws IOException 
	 */
	private void readOrdersFile(MultipartFile file, String account) throws IOException {

		generatedOrders.clear();
		rollovers.clear();
		
        DataInputStream in = new DataInputStream(file.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        	        
        String line;
	    while ((line = br.readLine()) != null) {
	    	
	    	String[] fields = line.split(FIELD_SEPARATOR);

            ExtOrder order = new ExtOrder();

            // Order Type
	    	String orderType = fields[FIELD_INDEX_ORDER_TYPE];
	    	boolean exitOrder = false;
	    	if (orderType.equals(ORDER_TYPE_LONG_ENTRY)) {
	    		order.m_action = ORDER_ACTION_BUY;
	    	} else if (orderType.equals(ORDER_TYPE_LONG_EXIT)) {
	    		order.m_action = ORDER_ACTION_SELL;
	    		exitOrder = true;
	    	}  else if (orderType.equals(ORDER_TYPE_SHORT_ENTRY)) {
	    		order.m_action = ORDER_ACTION_SELL;
	    	}  else if (orderType.equals(ORDER_TYPE_SHORT_EXIT)) {
	    		order.m_action = ORDER_ACTION_BUY;
	    		exitOrder = true;
	    	} else {
	    		// Probably the header row: skip it
	    		continue;
	    	}
	    	
	    	Contract contract = new Contract();
            contract.m_secType = CONTRACT_SEC_TYPE_FUTURE;
            contract.m_currency = CONTRACT_CURRENCY_USD;
	    	contract.m_symbol = fields[FIELD_INDEX_BROKER_SYMBOL];
	    	contract.m_exchange = contractService.getExchange(contract.m_symbol);
	    	contract.m_expiry = convertMonthToExpiry(fields[FIELD_INDEX_MONTH]);
	    	
	    	order.m_account = account;
            order.m_orderType = ORDER_TYPE_STOP;
            order.m_tif = ORDER_TIF_GTC;
	    	
	    	// Quantity
	    	Position position = openPositions.get(contract.m_symbol);
	    	if (exitOrder && position == null) {
	    		// No position to exit: ignore this order
	    		continue;
	    	} else if (exitOrder) {
	    		order.m_totalQuantity = Math.abs(position.getQuantity());
	    	} else if (!exitOrder && position != null) {
	    		// Already have a position: in which direction?
	    		if (position.getQuantity() > 0 && order.m_action.equals(ORDER_ACTION_BUY)) {
	    			// Already have a long position: ignore this order
	    			continue;
	    		} else if (position.getQuantity() < 0 && order.m_action.equals(ORDER_ACTION_SELL)) {
	    			// Already have a short position: ignore this order
	    			continue;
	    		} else {
			    	order.m_totalQuantity = Integer.parseInt(fields[FIELD_INDEX_QUANTITY]);
	    		}
	    	} else {
		    	order.m_totalQuantity = Integer.parseInt(fields[FIELD_INDEX_QUANTITY]);
	    	}
	    		    	
	    	// Order Price
	    	Double priceFactor = contractService.getPriceFactor(contract.m_symbol);
	    	order.m_auxPrice = parsePrice(fields[FIELD_INDEX_ORDER_PRICE]) * priceFactor;
	    	
	    	order.setContract(contract);
	    	
	    	// Roll Info
	    	if (!isBlank(fields[FIELD_INDEX_ROLL_INFO])) {
	    		Position openPosition = openPositions.get(contract.m_symbol);
	    		// Is there an open position?
	    		if (openPosition != null) {
	    			// Does the open position needs to be rolled over?
		    		String openPositionExpiry = openPosition.getContract().m_expiry;
		    		if (!openPositionExpiry.equals(contract.m_expiry)) {
			    		rollovers.add(contract.m_symbol);
		    		}
	    		}
	    	}
	    	
	    	generatedOrders.add(order);
	    }
        in.close();

	}
	
	/**
	 * Creates an order to exit a position based 
	 * @param order
	 * @return
	 */
	private ExtOrder createRolloverExit(Contract contract, Integer position, String account) {
        ExtOrder rolloverExit = new ExtOrder();
        rolloverExit.m_account = account;
        rolloverExit.m_orderType = ORDER_TYPE_MKT;
        rolloverExit.m_tif = ORDER_TIF_GTC;

        if (position > 0) {
            rolloverExit.m_action = ORDER_ACTION_SELL;
        } else {
            rolloverExit.m_action = ORDER_ACTION_BUY;
        }
        
        rolloverExit.m_totalQuantity = Math.abs(position);        
    	rolloverExit.setContract(contract);    	
    	return rolloverExit;
    }
	
	/**
	 * Creates an order to exit a position based 
	 * @param order
	 * @return
	 */
	private ExtOrder createRolloverEntry(Contract contract, Integer position, String account) {
        ExtOrder rolloverEntry = new ExtOrder();
        rolloverEntry.m_account = account;
        rolloverEntry.m_orderType = ORDER_TYPE_MKT;
        rolloverEntry.m_tif = ORDER_TIF_GTC;
        
        if (position > 0) {
        	rolloverEntry.m_action = ORDER_ACTION_BUY;
        } else {
        	rolloverEntry.m_action = ORDER_ACTION_SELL;
        }
        
        rolloverEntry.m_totalQuantity = Math.abs(position);        
        rolloverEntry.setContract(contract);    	
    	return rolloverEntry;
    }
	
	/**
	 * Convert a standard futures month (e.g., K3) to a TWS contract expiry string (YYYYMM)
	 * @param futuresMonth
	 * @return
	 */
	private String convertMonthToExpiry(String futuresMonth) {
    	String monthLetter = futuresMonth.substring(0,1);
		Integer yearLastDigit = Integer.parseInt(futuresMonth.substring(1));
		
		Integer year = Calendar.getInstance().get(Calendar.YEAR);		
		
		while(year % 10 != yearLastDigit) {
			year++;
		}
		
		return year.toString() + FUTURES_MONTHS.get(monthLetter);
	}
	
	/**
	 * Converts String s, which may consist of a decimal value or a fraction, to a Double.
	 * @param s
	 * @return
	 */
	private Double parsePrice(String s) {
		Double price = null;
		if (s.contains("/")) {
			// String may contain leading spaces, which getFraction() doesn't like
			s = s.trim();
			// String may also look like "140 14.0/32h"
			s = s.replace(".",	"");
			s = s.replace("h", "0");
			Fraction fraction = Fraction.getFraction(s);
			price = fraction.doubleValue();
		} else {
			price = Double.parseDouble(s);
		}
		return price;
	}
	
	/**
	 * Clears the open orders map
	 */
	public void clearOpenOrders() {
		openOrders.clear();
	}
	
	/**
	 * Builds an ExtOrder object from contract and order,
	 * and adds that object to the open orders map
	 * @param contract
	 * @param order
	 * @return
	 */
	public ExtOrder addOpenOrder(Contract contract, Order order) {
		ExtOrder extOrder = new ExtOrder(contract, order);
		return openOrders.put(order.m_orderId, extOrder);
	}
	
	/**
	 * Removes the specified order from the open orders map
	 * @param orderId
	 * @return
	 */
	public ExtOrder removeOpenOrder(Integer orderId) {
		return openOrders.remove(orderId);
	}
	
	/**
	 * Builds a Position object from contract and quantity,
	 * and adds that object to the open positions map.  Or, if
	 * quantity = 0, it will remove the position from the map.
	 * @param contract
	 * @param quantity
	 * @return
	 */
	public Position updatePosition(Contract contract, Integer quantity) {
		Position position = null;
		if (quantity != 0) {
			position = new Position();
			position.setContract(contract);
			position.setQuantity(quantity);
			openPositions.put(contract.m_symbol, position);
		} else {
			position = openPositions.remove(contract.m_symbol);
		}
		return position;
	}
	
	/**
	 * Returns the current list of action items
	 * @return
	 */
	public List<ActionItem> getActionItems() {
		return actionItems;
	}
	
	/**
	 * Sorts ExtOrders by symbol, action, and the absolute value of orderId (descending).
	 * For orderId, non-zero values are first, indicating orders to be canceled.
	 * Using the absolute value because orders entered directly in TWS will have a negative orderId.
	 * @author Todd
	 *
	 */
	private static class OrderComparator implements Comparator<ExtOrder> {

		@Override
		public int compare(ExtOrder o1, ExtOrder o2) {
						
			if (o1.getContract() == null) {
				if (o2.getContract() != null) {
					return -1;
				}
			} else if (o2.getContract() == null) {
				return 1;
			} else if (o1.getContract().m_symbol == null) {
				if (o2.getContract().m_symbol != null) {
					return -1;
				}
			} else {
				int result = o1.getContract().m_symbol.compareTo(o2.getContract().m_symbol);
				if (result != 0) {
					return result;
				} else if (o1.m_action == null) {
					if (o2.m_action != null) {
						return -1;
					}
				} else { 
					result = o1.m_action.compareTo(o2.m_action);
					if (result != 0) {
						return result;
					}
				}
			}
			return Integer.valueOf(Math.abs(o2.m_orderId)).compareTo(Math.abs(o1.m_orderId));
		}
		
	}
	
}
