package trader.service;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import trader.domain.Account;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.EWrapperMsgGenerator;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.UnderComp;
import com.ib.controller.OrderStatus;
import com.ib.controller.OrderType;

@Service
public class TwsApiService implements EWrapper {
	
	private static final String	ACCOUNT_VALUE_KEY_NET_LIQUIDATION = "NetLiquidation";
	
    protected final Log logger = LogFactory.getLog(getClass());

    @Resource
    private AccountService accountService;

    @Resource
    private ContractService contractService;

	@Resource
	private MessageService messageService;
	
	@Resource
	private OrderService orderService;
	
	private EClientSocket client = new EClientSocket(this);	
	
	@Value("${tws.port}")
	private int twsPort;
	
	private Integer nextOrderId = 0;

	/**
	 * Connect to TWS and request account updates
	 */
	public void connect() {
		
		orderService.clearOpenOrders();
    	orderService.clearOpenPositions();

    	client.eConnect(null, twsPort, 0);
        
        if (client.isConnected()) {
        	messageService.addInfoMessage("Connected to TWS server version " + client.serverVersion());
        	
        	reqAccountUpdates();
        	
        	client.reqAllOpenOrders();
        	client.reqAutoOpenOrders(true);
        }
    }

	/**
	 * Disconnect from TWS
	 */
    public void disconnect() {
        if (client.isConnected()) {
            client.eDisconnect();
            messageService.addInfoMessage("Disconnected from TWS");
        }
    }

    /**
     * Place an order
     * @param contract
     * @param order
     */
    public void placeOrder (Contract contract, Order order) {
    	if (order.m_orderType.equals(OrderType.MKT)) {
    		messageService.addInfoMessage("Placing order to " + order.m_action + " " + order.m_totalQuantity + " " + contract.m_symbol + " @ MKT for " + order.m_account);
    	} else {
    		messageService.addInfoMessage("Placing order to " + order.m_action + " " + order.m_totalQuantity + " " + contract.m_symbol + " @ " + order.m_auxPrice + " for " + order.m_account);
    	}
         
    	if (order.m_orderId == 0) {
            order.m_orderId = nextOrderId;
            nextOrderId++;
    	}
    	
         client.placeOrder(order.m_orderId, contract, order);        
    }

    /**
     * Cancel an order
     * @param contract
     * @param order
     */
    public void cancelOrder (Contract contract, Order order) {
    	if (order.m_orderType.equals(OrderType.MKT)) {
    		messageService.addInfoMessage("Canceling order to " + order.m_action + " " + order.m_totalQuantity + " " + contract.m_symbol + " @ MKT for " + order.m_account);
	 	} else {
	 		messageService.addInfoMessage("Canceling order to " + order.m_action + " " + order.m_totalQuantity + " " + contract.m_symbol + " @ " + order.m_auxPrice + " for " + order.m_account);
	 	}
         
         client.cancelOrder(order.m_orderId);
    }
    
    /**
     * Request account updates for all managed accounts
     */
    private void reqAccountUpdates() {
    	for (Account account : accountService.getAccounts()) {
            client.reqAccountUpdates(true, account.getAccountId());
    	}
    }
    
    // EWrapper methods

    public void tickPrice(int tickerId, int tickType, double price, int canAutoExecute) {
    	String msg = EWrapperMsgGenerator.tickPrice(tickerId, tickType, price, canAutoExecute);
    	messageService.addDataMessage(msg);
    }

    public void tickSize(int tickerId, int tickType, int size) {
    	String msg = EWrapperMsgGenerator.tickSize(tickerId, tickType, size);
    	messageService.addDataMessage(msg);
    }

    public void tickGeneric(int tickerId, int tickType, double value) {
    	String msg = EWrapperMsgGenerator.tickGeneric(tickerId, tickType, value);
    	messageService.addDataMessage(msg);
    }

    public void tickString(int tickerId, int tickType, String value) {
    	String msg = EWrapperMsgGenerator.tickString(tickerId, tickType, value);
    	messageService.addDataMessage(msg);
    }

    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureExpiry, double dividendImpact, double dividendsToExpiry) {
    	String msg = EWrapperMsgGenerator.tickEFP(tickerId, tickType, basisPoints, formattedBasisPoints, impliedFuture, holdDays, futureExpiry, dividendImpact, dividendsToExpiry);
    	messageService.addDataMessage(msg);
    }

    public void tickSnapshotEnd(int reqId) {
    	String msg = EWrapperMsgGenerator.tickSnapshotEnd(reqId);
    	messageService.addDataMessage(msg);
    }

    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
    	String msg = EWrapperMsgGenerator.orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
    	messageService.addDataMessage(msg);
    	
    	if (status.equals(OrderStatus.Cancelled.toString()) || status.equals(OrderStatus.Filled.toString())) {
        	orderService.removeOpenOrder(orderId);
    	}
    }

    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
    	String msg = EWrapperMsgGenerator.openOrder(orderId, contract, order, orderState);
    	messageService.addDataMessage(msg);
    	
    	// Contract expiries from TWS have the exact day of the expiry, not the contract month
    	// And the expiry is sometimes in the month prior to the contract month!

    	// Strip off the day component
    	contract.m_expiry = contract.m_expiry.substring(0, 6);
    	
    	if (contractService.hasPriorMonthExpiry(contract.m_symbol)) {
    		String year = contract.m_expiry.substring(0, 4);
    		String month = contract.m_expiry.substring(4, 6);
    		
    		// Increment the expiry month
    		if (month.equals("12")) {
    			Integer y = Integer.parseInt(year) + 1;
    			year = y.toString();
    			month = "01";
    		} else {
        		Integer m = Integer.parseInt(month) + 1;
        		month = m.toString();
           		// Left pad with a zero if necessary
        		if (month.length() == 1) {
         			month = "0" + month;
        		}        		
    		}
    		
    		contract.m_expiry = year + month;  		
    	} 

    	// Changing the expiry and also having a contract ID set causes a conflict... 
    	contract.m_conId = 0;

    	orderService.addOpenOrder(contract, order);
    	
        // Update nextOrderId if TWS has messed it up somehow
        if (orderId >= nextOrderId) {
        	nextOrderId = orderId + 1;
        } 
        
        // If no order ID (i.e, the order was entered manually), re-enter it
        // The original order will have to be deleted manually
        if (orderId == 0) {
        	messageService.addInfoMessage("Copying order with orderId = 0.  Delete the original in TWS.");
        	placeOrder(contract, order);
        }
    }

    public void openOrderEnd() {
    	String msg = EWrapperMsgGenerator.openOrderEnd();
    	messageService.addDataMessage(msg);
    	messageService.addInfoMessage("Open orders downloaded");
    }

    public void updateAccountValue(String key, String value, String currency, String accountName) {
    	String msg = EWrapperMsgGenerator.updateAccountValue(key, value, currency, accountName);
    	messageService.addDataMessage(msg);
    	
    	// Update the account value in the database
    	try {
	        if (key.equals(ACCOUNT_VALUE_KEY_NET_LIQUIDATION)) {
	        	accountService.updateAccountValue(accountName, Double.parseDouble(value));
	        }
    	} catch (Exception ex) {
        	messageService.addInfoMessage(ex.toString());
    	}
    }

    public void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
    	String msg = EWrapperMsgGenerator.updatePortfolio(contract, position, marketPrice, marketValue, averageCost, unrealizedPNL, realizedPNL, accountName);
    	messageService.addDataMessage(msg);
    	
		// Apparently the contract doesn't always have the exchange set
		contract.m_exchange = contractService.getExchange(contract.m_symbol);

    	orderService.updatePosition(contract, accountName, position);
    }

    public void updateAccountTime(String timeStamp) {
        String msg = EWrapperMsgGenerator.updateAccountTime(timeStamp);
    	messageService.addDataMessage(msg);
	}

    public void accountDownloadEnd(String accountName) {
    	String msg = EWrapperMsgGenerator.accountDownloadEnd(accountName);
    	messageService.addDataMessage(msg);
    	messageService.addInfoMessage("Account data downloaded");
    }

    public void nextValidId(int orderId) {
    	String msg = EWrapperMsgGenerator.nextValidId(orderId);
    	messageService.addDataMessage(msg);
    	
    	// Set the next order ID
        if (orderId > nextOrderId) {
        	this.nextOrderId = orderId;
        }
    }

    public void contractDetails(int reqId, ContractDetails contractDetails) {
    	String msg = EWrapperMsgGenerator.contractDetails(reqId, contractDetails);
    	messageService.addDataMessage(msg);
    }

    public void bondContractDetails(int reqId, ContractDetails contractDetails) {
    	String msg = EWrapperMsgGenerator.bondContractDetails(reqId, contractDetails);
    	messageService.addDataMessage(msg);
    }

    public void contractDetailsEnd(int reqId) {
    	String msg = EWrapperMsgGenerator.contractDetailsEnd(reqId);
    	messageService.addDataMessage(msg);
    }

    public void execDetails(int reqId, Contract contract, Execution execution) {
    	String msg = EWrapperMsgGenerator.execDetails(reqId, contract, execution);
    	messageService.addDataMessage(msg);
    }

    public void execDetailsEnd(int reqId) {
    	String msg = EWrapperMsgGenerator.execDetailsEnd(reqId);
    	messageService.addDataMessage(msg);
    }

    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size) {
    	String msg = EWrapperMsgGenerator.updateMktDepth(tickerId, position, operation, side, price, size);
    	messageService.addDataMessage(msg);
    }

    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, int size) {
    	String msg = EWrapperMsgGenerator.updateMktDepthL2(tickerId, position, marketMaker, operation, side, price, size);
    	messageService.addDataMessage(msg);
    }

    public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
    	String msg = EWrapperMsgGenerator.updateNewsBulletin(msgId, msgType, message, origExchange);
    	messageService.addDataMessage(msg);
    }

    public void managedAccounts(String accountsList) {
    	String msg = EWrapperMsgGenerator.managedAccounts(accountsList);
    	messageService.addDataMessage(msg);
    }

    public void receiveFA(int faDataType, String xml) {
    	String msg = EWrapperMsgGenerator.receiveFA(faDataType, xml);
    	messageService.addDataMessage(msg);
    }

    public void historicalData(int reqId, String date, double open, double high, double low, double close, int volume, int count, double WAP, boolean hasGaps) {
    	String msg = EWrapperMsgGenerator.historicalData(reqId, date, open, high, low, close, volume, count, WAP, hasGaps);
    	messageService.addDataMessage(msg);
    }
    
    public void scannerParameters(String xml) {
    	String msg = EWrapperMsgGenerator.scannerParameters(xml);
    	messageService.addDataMessage(msg);
    }

    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
    	String msg = EWrapperMsgGenerator.scannerData(reqId, rank, contractDetails, distance, benchmark, projection, legsStr);
    	messageService.addDataMessage(msg);
    }

    public void scannerDataEnd(int reqId) {
    	String msg = EWrapperMsgGenerator.scannerDataEnd(reqId);
    	messageService.addDataMessage(msg);
    }

    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count) {
    	String msg = EWrapperMsgGenerator.realtimeBar(reqId, time, open, high, low, close, volume, wap, count);
    	messageService.addDataMessage(msg);
    }

    public void currentTime(long time) {
    	String msg = EWrapperMsgGenerator.currentTime(time);
    	messageService.addDataMessage(msg);
    }

    public void fundamentalData(int reqId, String data) {
    	String msg = EWrapperMsgGenerator.fundamentalData(reqId, data);
    	messageService.addDataMessage(msg);
    }

    public void deltaNeutralValidation(int reqId, UnderComp underComp) {
    	String msg = EWrapperMsgGenerator.deltaNeutralValidation(reqId, underComp);
    	messageService.addDataMessage(msg);
    }

    public void error(Exception e) {
    	messageService.addInfoMessage(e.toString());
    }

    public void error(String str) {
    	String msg = EWrapperMsgGenerator.error(str);
    	messageService.addInfoMessage(msg);
    }

    public void error(int id, int errorCode, String errorMsg) {
    	String msg = EWrapperMsgGenerator.error(id, errorCode, errorMsg);
    	messageService.addInfoMessage(msg);
    }

    public void connectionClosed() {
    	String msg = EWrapperMsgGenerator.connectionClosed();
    	messageService.addInfoMessage(msg);
    }

	public void tickOptionComputation(int tickerId, int field, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
    	String msg = EWrapperMsgGenerator.tickOptionComputation(tickerId, field, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice);
    	messageService.addDataMessage(msg);
	}

	public void marketDataType(int reqId, int marketDataType) {
    	String msg = EWrapperMsgGenerator.marketDataType(reqId, marketDataType);
    	messageService.addDataMessage(msg);
	}

	public void commissionReport(CommissionReport commissionReport) {
    	String msg = EWrapperMsgGenerator.commissionReport(commissionReport);
    	messageService.addDataMessage(msg);
	}

	public void accountSummary(int reqId, String account, String tag, String value, String currency) {
    	String msg = EWrapperMsgGenerator.accountSummary(reqId, account, tag, value, currency);
    	messageService.addDataMessage(msg);
	}

	public void accountSummaryEnd(int reqId) {
    	String msg = EWrapperMsgGenerator.accountSummaryEnd(reqId);
    	messageService.addDataMessage(msg);
	}

	public void position(String account, Contract contract, int position, double avgCost) {
    	String msg = EWrapperMsgGenerator.position(account, contract, position, avgCost);
    	messageService.addDataMessage(msg);
	}

	public void positionEnd() {
    	String msg = EWrapperMsgGenerator.positionEnd();
    	messageService.addDataMessage(msg);
	}

	@Override
	public void verifyMessageAPI(String apiData) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void verifyCompleted(boolean isSuccessful, String errorText) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void displayGroupList(int reqId, String groups) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void displayGroupUpdated(int reqId, String contractInfo) {
		// TODO Auto-generated method stub		
	}
}
