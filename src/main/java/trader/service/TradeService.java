package trader.service;

import static trader.constants.Constants.HIGH_CORRELATION;
import static trader.constants.Constants.LONG;
import static trader.constants.Constants.LOW_CORRELATION;
import static trader.constants.Constants.SHORT;
import static trader.util.MathUtils.round;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.Order;

import trader.dao.TradeDao;
import trader.domain.Account;
import trader.domain.ExtContract;
import trader.domain.Trade;

@Service
public class TradeService {
	
	@Resource
	private AccountService accountService;
	
	@Resource
	private ClientService clientService;
	
	@Resource
	private ContractService contractService;
	
	@Resource
	private MessageService messageService;
	
	@Resource
	private OrderService orderService;
	
	@Resource
	private ParameterService parameterService;
	
	@Resource
	private TradeDao tradeDao;

	private String rolloverSymbol;
	private List<Order> rolloverOrders = new ArrayList<Order>();
	private Map<String, Execution> rolloverExecutions = new HashMap<String, Execution>();
	
	public String getRolloverSymbol() {
		return rolloverSymbol;
	}
	
	public void setRolloverSymbol(String symbol) {
		rolloverSymbol = symbol;
	}
	
	/**
	 * Returns all open trades from the TRADE table
	 * for the specified account and symbol (if specified)
	 * @param accountId pass null to ignore
	 * @param symbol pass null to ignore
	 * @return
	 */
	public List<Trade> getOpenTrades(String accountId, String symbol) {
		return tradeDao.getOpenTrades(accountId, symbol);
	}
	
	public void updateTrade(Trade trade) {
		tradeDao.updateTrade(trade);
	}
	
	public Trade insertTrade(String accountId, String symbol, Trade trade) {
		return tradeDao.insertTrade(accountId, symbol, trade);
	}
	
	public Integer getPosition(String accountId, String symbol) {
		return tradeDao.getPosition(accountId, symbol);
	}
	
	public void validatePosition(String accountId, String symbol, Integer position) {
    	Integer calculatedPosition = getPosition(accountId, symbol);
		if (position == 0) {
			// Do nothing: IB sometimes sends bad data after market hours 
		} else if (!contractService.isValidContract(symbol)) {
			// Contract does not exist in database
        	messageService.addInfoMessage("Contract not found for position: " + symbol);
		} else if (!calculatedPosition.equals(position)) {
        	// Position is out of sync with trade history
    		messageService.addInfoMessage(symbol + " calculated position=" + calculatedPosition + 
        			", actual position=" + position);
		}
	}
	
	public void handleExecution(Contract contract, Execution execution) {
		
    	// Only process executions for sub accounts (not the master)
    	if (!accountService.isValidAccount(execution.m_acctNumber)) {
    		return;
    	}
		
    	// Wait for a complete fill before doing anything
		if (!orderService.isOrderFilled(execution.m_orderId, execution.m_cumQty)) {
			return;
		}
		
    	messageService.addInfoMessage(execution.m_side + " " + execution.m_cumQty + " " + 
    			contract.m_symbol + " @ " + execution.m_avgPrice + " for " + execution.m_acctNumber);

    	// Convert prices to dollars (if necessary)
        execution.m_avgPrice = contractService.convertPriceToDollars(contract.m_symbol, execution.m_avgPrice);
		execution.m_price = contractService.convertPriceToDollars(contract.m_symbol, execution.m_price);
		
		// Determine if this is a rollover or a normal execution
		if (rolloverSymbol == null || !rolloverSymbol.equals(contract.m_symbol)) {		
			handleNormalExecution(contract.m_symbol, execution);
		} else {			
 			handleRolloverExecution(contract.m_symbol, execution);
		}

	}
	
    public void rollover(String symbol, String expiry) throws SQLException {
    	
    	// Initialize class variables
    	rolloverSymbol = symbol;
    	rolloverOrders.clear();
		rolloverExecutions.clear();
		
		boolean openPositions = false;
    	
		for (Account account : accountService.getAccounts()) {
			
			ExtContract contract = contractService.getContract(symbol);
			
	      	Integer position = getPosition(account.getAccountId(), symbol);
	      	if (position != 0) {
	      		openPositions = true;
	      		for (Order order : orderService.getOpenOrders(account.getAccountId(), symbol)) {
	      			rolloverOrders.add(order);
	      		}
	      	}     	
	      	orderService.cancelOrders(account.getAccountId(), contract);
	      	
	      	if (position != 0) {
		        Order order = new Order();
		        order.m_account = account.getAccountId();
		        order.m_totalQuantity = Math.abs(position);
		        order.m_orderType = "MKT";
		        order.m_tif = "GTC";
	          	if (position > 0) {
	                order.m_action = "SELL";
	          	} else {
	                order.m_action = "BUY";
	          	}
	    		clientService.placeOrder(contract, order);
	      	}
	    		
	    	contract.m_expiry = expiry;
	    	
	      	if (position != 0) {
		    	Order order = new Order();
		    	order.m_account = account.getAccountId();
		        order.m_totalQuantity = Math.abs(position);
		        order.m_orderType = "MKT";
		        order.m_tif = "GTC";
	         	if (position > 0) {
	                order.m_action = "BUY";
	          	} else {
	                order.m_action = "SELL";
	          	}
	    		clientService.placeOrder(contract, order);
	      	}
	    }
		
		// Update the contract expiry in the database
		ExtContract contract = contractService.getContract(symbol);
    	contract.m_expiry = expiry;    	
    	contractService.updateContract(contract);
    	
    	// Request new historical data if there are no open positions for this contract
    	// If the are new open positions, this will be done after they are rolled over
    	if (!openPositions) {
    		clientService.reqHistoricalData(contract);
    	}
    }

	public boolean isLoaded(String accountId, ExtContract contract, Integer direction) {
		
		Integer units;
		
		// Single contract
		units = getOpenTrades(accountId, contract.m_symbol).size();
		if (units >= parameterService.getIntParameter("MAX_UNITS_PER_CONTRACT")) {
			return true;
		}
		
		// High correlation
		units = getCorrelatedUnits(accountId, contract, direction, HIGH_CORRELATION);
		if (units >= parameterService.getIntParameter("MAX_HIGH_CORRELATED_UNITS")) {
			return true;
		}
		
		// Low correlation
		units = getCorrelatedUnits(accountId, contract, direction, LOW_CORRELATION);
		if (units >= parameterService.getIntParameter("MAX_LOW_CORRELATED_UNITS")) {
			return true;
		}
		
		// Same direction
		units = 0;
		for (ExtContract c : contractService.getContracts()) {
			int position = getPosition(accountId, c.m_symbol);
			if ((direction == LONG && position > 0)
					|| (direction == SHORT && position < 0)) {
				units += getOpenTrades(accountId, c.m_symbol).size();
			}
		}
		if (units >= parameterService.getIntParameter("MAX_UNITS_PER_DIRECTION")) {
			return true;
		} else {
			return false;		
		}
	}
	
	private Integer getCorrelatedUnits(String accountId, ExtContract contract, Integer direction, String degree) {

		Integer units = getOpenTrades(accountId, contract.m_symbol).size();
		
		for (String correlatedSymbol : contractService.getCorrelatedSymbols(contract.m_symbol, degree)) {
			int position = getPosition(accountId, correlatedSymbol);
			if ((direction == LONG && position > 0)
					|| (direction == SHORT && position < 0)) {
				units += getOpenTrades(accountId, correlatedSymbol).size();
			}
		}
		return units;	
	}
	
	private void handleNormalExecution(String symbol, Execution execution) {
		ExtContract contract = contractService.getContract(symbol);
		Order order = orderService.getOrder(execution.m_orderId);
		
		orderService.removeOrder(execution.m_orderId);		
		
		if ((isEntryExecution(execution.m_side, getPosition(execution.m_acctNumber, symbol)))) {
			handleEntryExecution(contract, execution, order);			
			orderService.updateOrders(execution.m_acctNumber);   	
		} else {
			handleExitExecution(contract, execution);
			clientService.reqHistoricalData(contract);
		}
		
	}
	
	private boolean isEntryExecution(String side, Integer position) {
		return ((side.equals("BOT") && position >= 0) || (side.equals("SLD") && position <= 0));
	}
		    
	private void handleEntryExecution(ExtContract contract, Execution execution, Order order) {

		// Set local variables
		int quantity = execution.m_cumQty;
		String stopAction;
		String addAction;
		Double addPrice;
        Double addDistance = contract.getAtr() * parameterService.getDoubleParameter("ADD_UNIT_ATR_MULTIPLE");
        Integer direction;
        int previousPosition = getPosition(execution.m_acctNumber, contract.m_symbol);

        if (execution.m_side.equals("BOT")) {
			stopAction = "SELL";
			addAction = "BUY";
			addPrice = round(execution.m_avgPrice + addDistance, contract.getTickSize());
			direction = LONG;
		} else {
			quantity = -quantity;
			stopAction = "BUY";
			addAction = "SELL";
			addPrice = round(execution.m_avgPrice - addDistance, contract.getTickSize()); 
			direction = SHORT;
		}
        
		// Calculate stop price
		double stopPrice = calcInitialStopPrice(contract, execution);
		
		// Record the trade
		Trade trade = new Trade();
		trade.setEntryDate(new Date());
		trade.setQuantity(quantity);
		trade.setEntryPrice(execution.m_avgPrice);
		trade.setStopPrice(stopPrice);
		trade = insertTrade(execution.m_acctNumber, contract.m_symbol, trade);
		
		quantity = Math.abs(quantity);
		
        if (previousPosition == 0) {
        	// Cancel all existing orders
        	orderService.cancelOrders(execution.m_acctNumber, contract);
        } else if (!isSlippage(contract, order, execution)) {
			// Cancel the nearest existing stop order (if any)
			Order o = null;
			for (Order stopOrder : orderService.getOpenOrders(execution.m_acctNumber, contract.m_symbol)) {
				if (stopOrder.m_action.equals(stopAction)) {
					if (o == null 
							|| (execution.m_side.equals("BOT") && stopOrder.m_auxPrice > o.m_auxPrice)
							|| (execution.m_side.equals("SLD") && stopOrder.m_auxPrice < o.m_auxPrice)) {
						o = stopOrder;
					}						
				}
			}
			quantity = quantity + o.m_totalQuantity;
			clientService.cancelOrder(contract, o);
			orderService.removeOrder(o.m_orderId);
		}
		
		// Place new stop order
        Order stopOrder = new Order();
        stopOrder.m_account = execution.m_acctNumber;
        stopOrder.m_action = stopAction;
        stopOrder.m_totalQuantity = quantity;
        stopOrder.m_orderType = "STP";
        stopOrder.m_tif = "GTC";
        stopOrder.m_auxPrice = stopPrice;            
		clientService.placeOrder(contract, stopOrder);

		// Place next entry order
		int unitSize = accountService.getUnitSize(execution.m_acctNumber, contract.m_symbol);
		if (!isLoaded(execution.m_acctNumber, contract, direction) && unitSize > 0) {					
            Order entryOrder = new Order();
            entryOrder.m_account = execution.m_acctNumber;
            entryOrder.m_action = addAction;
            entryOrder.m_totalQuantity = unitSize;
            entryOrder.m_orderType = "STP";
            entryOrder.m_tif = "GTC";
            entryOrder.m_auxPrice = addPrice;            
			clientService.placeOrder(contract, entryOrder);					
		}
	}
	
	private Double calcInitialStopPrice(ExtContract contract, Execution execution) {
		Double stopPrice;
	    Double stopDistance = parameterService.getDoubleParameter("STOP_ATR_MULTIPLE") * contract.getAtr();
	    if (execution.m_side.equals("BOT")) {
	    	stopPrice = execution.m_avgPrice - stopDistance;
	    } else {
	    	stopPrice = execution.m_avgPrice + stopDistance;
	    }
	    return round(stopPrice, contract.getTickSize());
	}
	
	private boolean isSlippage(ExtContract contract, Order order, Execution execution) {
		Double slipAtrMultiple = parameterService.getDoubleParameter("SLIP_ATR_MULTIPLE");
		if (Math.abs(execution.m_avgPrice - order.m_auxPrice) < slipAtrMultiple) {
			return false;
		} else {
			return true;
		}
	}
	
	private void handleExitExecution(ExtContract contract, Execution execution) {
		
		// Sort trades by entry date, descending
		List<Trade> trades = getOpenTrades(execution.m_acctNumber, contract.m_symbol);
		sortTrades(trades);
		
		int quantityUpdated = 0;
		for (ListIterator<Trade> it = trades.listIterator(); it.hasNext();) {			
			Trade trade = it.next();
			
			if (trade.getExitDate() == null) {
				
				trade.setExitDate(new Date());
				trade.setExitPrice(execution.m_avgPrice);

				it.remove();
				updateTrade(trade);
				
				quantityUpdated += Math.abs(trade.getQuantity());				
				if (quantityUpdated >= execution.m_cumQty) {
					break;
				}
			}
		}
	}
	
	private void sortTrades(List<Trade> trades) {
		// Sort trades by entry date, descending
		Collections.sort(trades, new Comparator<Trade>() {
		    public int compare(Trade o1, Trade o2) {
		        return -(o1.getEntryDate().compareTo(o2.getEntryDate()));
		    }});
	}
	
	private void handleRolloverExecution (String symbol, Execution execution) {
		Execution rolloverExecution = rolloverExecutions.get(execution.m_acctNumber);
		if (rolloverExecution == null) {
			rolloverExecutions.put(execution.m_acctNumber, execution);
		} else {
	    	Double adjustmentFactor = 0D;   	    		
			if (rolloverExecution.m_orderId < execution.m_orderId) {
				// rolloverExecution closed out the position in the old contract
				// execution opened a position in the new contract
				adjustmentFactor = execution.m_avgPrice - rolloverExecution.m_avgPrice;
			} else {
				// execution closed out the position in the old contract
				// rolloverExecution opened a position in the new contract
				adjustmentFactor = rolloverExecution.m_avgPrice - execution.m_avgPrice;
			}			

			ExtContract contract = contractService.getContract(symbol);
			for (Iterator<Order> it = rolloverOrders.iterator(); it.hasNext();) {
				Order o = it.next();
				if (o.m_account.equals(execution.m_acctNumber)) {
		            Order order = new Order();
		            order.m_account = execution.m_acctNumber;
		            order.m_action = o.m_action;
		            order.m_totalQuantity = o.m_totalQuantity;
		            order.m_orderType = o.m_orderType;
		            order.m_tif = o.m_tif;
		            order.m_auxPrice = round(o.m_auxPrice + adjustmentFactor, contract.getTickSize());            
					clientService.placeOrder(contract, order);
					it.remove();
				}
			}			
			for (Trade trade : getOpenTrades(execution.m_acctNumber, symbol)) {
				trade.setEntryPrice(trade.getEntryPrice() + adjustmentFactor);
				trade.setStopPrice(trade.getStopPrice() + adjustmentFactor);
				updateTrade(trade);
			}
			if (rolloverOrders.isEmpty()) {
				clientService.reqHistoricalData(contract);
			}
		}		
	}
    
}
