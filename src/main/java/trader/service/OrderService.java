package trader.service;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
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

import trader.constants.DeliveryMonth;
import trader.domain.ExtOrder;
import trader.domain.Position;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.contracts.FutContract;
import com.ib.controller.OrderType;
import com.ib.controller.Types.Action;
import com.ib.controller.Types.TimeInForce;

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
	
    protected final Log logger = LogFactory.getLog(getClass());
    
    private Map<Integer, ExtOrder> openOrders = new HashMap<Integer, ExtOrder>();
    private Map<String, Position> openPositions = new HashMap<String, Position>();
    
    private List<ExtOrder> generatedOrders = new ArrayList<ExtOrder>();
    private Set<String> rollovers = new HashSet<String>();
    
    private List<ExtOrder> actionItems = new ArrayList<ExtOrder>();
    
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
	public List<ExtOrder> importOrders(MultipartFile file, String account) throws IOException {
		
		logger.info("Clearing action items");
		actionItems.clear();
		
		readOrdersFile(file, account);
		List<ExtOrder> openOrders = new ArrayList<ExtOrder>();
		for (ExtOrder order : this.openOrders.values()) {
			if (order.m_account.equals(account)) {
				openOrders.add(order);
			}
		}
		
		// New orders = generated minus open
		List<ExtOrder> newOrders = new ArrayList<ExtOrder>(generatedOrders);
		newOrders.removeAll(openOrders);
		
		// Cancelled orders = open minus generated
		List<ExtOrder> cancelledOrders = new ArrayList<ExtOrder>(openOrders);
		cancelledOrders.removeAll(generatedOrders);
		
		// Combine and sort orders
		List<ExtOrder> updatedOrders = new ArrayList<ExtOrder>(newOrders);
		updatedOrders.addAll(cancelledOrders);
				
		// Build list of action items
		for (ExtOrder order : updatedOrders) {
			
			Contract contract = order.getContract();
			String symbol = contract.m_symbol;
			boolean isNewOrder = (order.m_orderId == 0);
			
			// Handle rollovers
			if (rollovers.contains(symbol) && isNewOrder) {

				Position position = openPositions.get(account + symbol);
				ExtOrder rolloverExit = createRolloverExit(position.getContract(), position.getQuantity(), account);
				ExtOrder rolloverEntry = createRolloverEntry(contract, position.getQuantity(), account);

				actionItems.add(rolloverExit);
				actionItems.add(rolloverEntry);
				
				rollovers.remove(symbol);
			}
			
			actionItems.add(order);
		}
		logger.info("Action items size=" + actionItems.size());
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
	
		List<ExtOrder> processedActionItems = new ArrayList<ExtOrder>(); 
		
		for (Entry<Integer, Boolean> entry : actionItemIndices.entrySet()) {
			
			int actionItemIndex = entry.getKey();
			boolean isExecute = entry.getValue();
			ExtOrder actionItem = actionItems.get(actionItemIndex);
			
			if (isExecute) {
				if (actionItem.m_orderId == 0) {
					// This is a new order...place it
					twsApiService.placeOrder(actionItem.getContract(), actionItem);
				} else {
					// This is an existing order...cancel it
					twsApiService.cancelOrder(actionItem.getContract(), actionItem);
				}
			}
			processedActionItems.add(actionItem);
		}
		
		logger.info("Removing processed action items");
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
	    		order.m_action = Action.BUY.getApiString();
	    	} else if (orderType.equals(ORDER_TYPE_LONG_EXIT)) {
	    		order.m_action = Action.SELL.getApiString();
	    		exitOrder = true;
	    	}  else if (orderType.equals(ORDER_TYPE_SHORT_ENTRY)) {
	    		order.m_action = Action.SELL.getApiString();
	    	}  else if (orderType.equals(ORDER_TYPE_SHORT_EXIT)) {
	    		order.m_action = Action.BUY.getApiString();
	    		exitOrder = true;
	    	} else {
	    		// Probably the header row: skip it
	    		continue;
	    	}
	    	
	    	String symbol = fields[FIELD_INDEX_BROKER_SYMBOL];
	    	String expiry = convertMonthToExpiry(fields[FIELD_INDEX_MONTH]);
	    	Contract contract = new FutContract(symbol, expiry);
	    	contract.m_exchange = contractService.getExchange(contract.m_symbol);
	    	
	    	order.m_account = account;
            order.m_orderType = OrderType.STP.getApiString();
            order.m_tif = TimeInForce.GTC.getApiString();
	    	
	    	// Quantity
	    	Position position = openPositions.get(account + contract.m_symbol);
	    	if (exitOrder && position == null) {
	    		// No position to exit: ignore this order
	    		continue;
	    	} else if (exitOrder) {
	    		order.m_totalQuantity = Math.abs(position.getQuantity());
	    	} else if (!exitOrder && position != null) {
	    		// Already have a position: in which direction?
	    		if (position.getQuantity() > 0 && order.m_action.equals(Action.BUY.getApiString())) {
	    			// Already have a long position: ignore this order
	    			continue;
	    		} else if (position.getQuantity() < 0 && order.m_action.equals(Action.SELL.getApiString())) {
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
	    		Position openPosition = openPositions.get(account + contract.m_symbol);
	    		// Is there an open position?
	    		if (openPosition != null) {
	    			// Does the open position needs to be rolled over?
		    		String openPositionExpiry = openPosition.getContract().m_expiry;
		    		if (!openPositionExpiry.equals(contract.m_expiry)) {
			    		rollovers.add(contract.m_symbol);
		    		}
	    		}
	    	}
	    	
	    	logger.info("Adding generated order to " + order.m_action + " " + order.getContract().m_symbol);
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
        rolloverExit.m_orderType = OrderType.MKT.getApiString();
        rolloverExit.m_tif = TimeInForce.GTC.getApiString();

        if (position > 0) {
            rolloverExit.m_action = Action.SELL.getApiString();
        } else {
            rolloverExit.m_action = Action.BUY.getApiString();
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
        rolloverEntry.m_orderType = OrderType.MKT.getApiString();
        rolloverEntry.m_tif = TimeInForce.GTC.getApiString();
        
        if (position > 0) {
        	rolloverEntry.m_action = Action.BUY.getApiString();
        } else {
        	rolloverEntry.m_action = Action.SELL.getApiString();
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
		
		return year.toString() + DeliveryMonth.valueOf(monthLetter).getTwoDigitMonth();
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
	 * @param account
	 * @param quantity
	 * @return
	 */
	public Position updatePosition(Contract contract, String account, Integer quantity) {
		Position position = null;
		if (quantity != 0) {
			position = new Position();
			position.setContract(contract);
			position.setQuantity(quantity);
			openPositions.put(account + contract.m_symbol, position);
		} else {
			position = openPositions.remove(account + contract.m_symbol);
		}
		return position;
	}
	
	/**
	 * Returns the current list of action items
	 * @return
	 */
	public List<ExtOrder> getActionItems() {
		logger.info("actionItems size=" + actionItems.size());
		return actionItems;
	}
	
}
