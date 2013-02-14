package trader.service;

import static trader.constants.Constants.LONG;
import static trader.constants.Constants.SHORT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import trader.domain.Account;
import trader.domain.ExtContract;
import trader.domain.ExtOrder;
import trader.domain.Trade;

import com.ib.client.Order;

@Service
public class OrderService {
	
    protected final Log logger = LogFactory.getLog(getClass());

    @Resource
	private AccountService accountService;
	
	@Resource
	private ClientService clientService;
	
	@Resource
	private ContractService contractService;
	
	@Resource 
	private MessageService messageService;
	
	@Resource
	private TradeService tradeService;
	
	private Map<Integer, ExtOrder> openOrders = new HashMap<Integer, ExtOrder>();

	public ExtOrder getOrder(int orderId) {
		return openOrders.get(orderId);
	}
	
	public void putOrder(String symbol, Order order) {
		ExtOrder o = new ExtOrder();
		o.m_orderId = order.m_orderId;
        o.m_action = order.m_action;
        o.m_totalQuantity = order.m_totalQuantity;
        o.m_orderType = order.m_orderType;
        o.m_tif = order.m_tif;
        o.m_auxPrice = order.m_auxPrice;
        o.m_account = order.m_account;
		o.setSymbol(symbol);
		openOrders.put(o.m_orderId, o);
	}
 	
	public void removeOrder(int orderId) {
		openOrders.remove(orderId);
	}

	public List<Order> getOpenOrders(String accountId, String symbol) {
		logger.info("Getting open orders");
		List<Order> orders = new ArrayList<Order>();
		for (ExtOrder order : openOrders.values()) {
			if (order.getSymbol().equals(symbol) && order.m_account.equals(accountId)) {
				orders.add(order);
			}
		}
		return orders;
	}
	
	public boolean isOrderFilled(Integer orderId, Integer cumQty) {
		Order order = openOrders.get(orderId);
		return (cumQty.equals(order.m_totalQuantity));
	}
	
	public void updateOrders() {
		for (Account account : accountService.getAccounts()) {
			updateOrders(account.getAccountId());
		}
	}
	
	public void updateOrders(String accountId) {
		accountService.calcUnitSizes(accountId);
		for (ExtContract contract : contractService.getContracts()) {
			updateOrders(accountId, contract);
		}		
		messageService.addInfoMessage("Orders updated for account " + accountId);
	}
	
	public void cancelOrders(String accountId, ExtContract contract) {
		cancelOrders(accountId, contract, null);
	}
	
	/**
	 * Validates that all open positions from TRADE table
	 * have at least one corresponding open order.
	 */
	public void validateOpenOrders() {
		List<Trade> openTrades = tradeService.getOpenTrades(null, null);
		for (Trade trade : openTrades) {
			String accountId = trade.getAccountId();
			String symbol = trade.getSymbol();
			if (getOpenOrders(accountId, symbol).isEmpty()) {
				String msg = "No order found! Account: " + accountId + "Symbol: " + symbol;
				messageService.addInfoMessage(msg);
			}
		}
	}
	
	private void updateOrders(String accountId, ExtContract contract) {
		
		if (tradeService.getOpenTrades(accountId, contract.m_symbol).isEmpty()) {
			if (accountService.getUnitSize(accountId, contract.m_symbol) == 0) {
				cancelOrders(accountId, contract);
			} else {
				if (tradeService.isLoaded(accountId, contract, LONG)) {
					cancelOrders(accountId, contract, "BUY");
				} else {
					updateEntryOrder(accountId, contract, "BUY");
				}
				if (tradeService.isLoaded(accountId, contract, SHORT)) {
					cancelOrders(accountId, contract, "SELL");
				} else {
					updateEntryOrder(accountId, contract, "SELL");
				}
			}
		} else {
			if (tradeService.getPosition(accountId, contract.m_symbol) > 0) {
				if (tradeService.isLoaded(accountId, contract, LONG)) {
					cancelOrders(accountId, contract, "BUY");
				}
				updateStopOrder(accountId, contract, "SELL");
			} else {
				if (tradeService.isLoaded(accountId, contract, SHORT)) {
					cancelOrders(accountId, contract, "SELL");
				}
				updateStopOrder(accountId, contract, "BUY");
			}
		}		
	}
	
	private void updateEntryOrder(String accountId, ExtContract contract, String action) {

		Double price;
		if (action.equals("BUY")) {
			price = contract.getEntryHigh() + contract.getTickSize();
		} else {
			price = contract.getEntryLow() - contract.getTickSize();
		}
		
		int unitSize = accountService.getUnitSize(accountId, contract.m_symbol);
		
		boolean orderExists = false;
		for (Order order : getOpenOrders(accountId, contract.m_symbol)) {
			if (order.m_action.equals(action)) {
				if (order.m_totalQuantity == unitSize
						&& Math.abs(order.m_auxPrice - price) < contract.getTickSize()) {
					orderExists = true;
				} else {
					clientService.cancelOrder(contract, order);
					removeOrder(order.m_orderId);
				}
			}
		}

		if (!orderExists) {
            Order order = new Order();
            order.m_account = accountId;
            order.m_action = action;
            order.m_totalQuantity = unitSize;
            order.m_orderType = "STP";
            order.m_tif = "GTC";
            order.m_auxPrice = price;            
			clientService.placeOrder(contract, order);
		}
	}
	
	private void updateStopOrder(String accountId, ExtContract contract, String action) {
		logger.info("Updating stop order");
		Double price;
		if (action.equals("BUY")) {
			price = contract.getExitHigh() + contract.getTickSize();
		} else {
			price = contract.getExitLow() - contract.getTickSize();
		}

		int quantity = 0;
		for (Order order : getOpenOrders(accountId, contract.m_symbol)) {
			if (order.m_action.equals(action)) {
				if ((action.equals("BUY") && (order.m_auxPrice - price) > contract.getTickSize())
						|| (action.equals("SELL") && (price - order.m_auxPrice) > contract.getTickSize())) {
					quantity += order.m_totalQuantity;
					clientService.cancelOrder(contract, order);
					removeOrder(order.m_orderId);
				}
			}
		}
		
		if (quantity > 0) {
            Order order = new Order();
            order.m_account = accountId;
            order.m_action = action;
            order.m_totalQuantity = quantity;
            order.m_orderType = "STP";
            order.m_tif = "GTC";
            order.m_auxPrice = price;            
			clientService.placeOrder(contract, order);
		}
	}
	
	private void cancelOrders(String accountId, ExtContract contract, String action) {
		logger.info("Canceling " + action + " orders");
		for (Order order : getOpenOrders(accountId, contract.m_symbol)) {
			if (order.m_action.equals(action) || action == null) {
				clientService.cancelOrder(contract, order);
				removeOrder(order.m_orderId);
			}
		}
	}
	
}
