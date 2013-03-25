package trader.service;

import static trader.constants.Constants.TICK_TYPE_VOLUME;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import trader.domain.Account;
import trader.domain.ExtContract;
import trader.domain.Rollover;

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

@Service
public class ClientService implements EWrapper {
	
	@Resource
    private AccountService accountService;

	@Resource
	private ContractService contractService;
	
	@Resource
	private ExecutionService executionService;
	
	@Resource
	private MessageService messageService;
	
	@Resource
	private OrderService orderService;
	
	@Resource
	private ParameterService parameterService;
	
	@Resource
	private TradeService tradeService;
	
	private EClientSocket client = new EClientSocket(this);	
	private Integer nextOrderId;
	
	private Map<Integer, ExtContract> rolloverContracts;
	
   public void connect() {
        client.eConnect(null, parameterService.getIntParameter("TWS_PORT"), 0);
        if (client.isConnected()) {
        	messageService.addInfoMessage("Connected to TWS server version " + client.serverVersion() + 
            		" at " + client.TwsConnectionTime());
        	contractService.initHistDataContracts();
        	reqAccountUpdates();
            client.reqOpenOrders();
            client.reqAutoOpenOrders(true);
        } else {
        	messageService.addInfoMessage("Failed to connect to TWS server");
        }
    }

    public void disconnect() {
        if (client.isConnected()) {
            client.eDisconnect();
            messageService.addInfoMessage("Disconnected from TWS");
        }
    }
    
    public void startPositionSizing() {
        reqHistoricalData(contractService.getContracts(), true);
    }
    
	public void checkForRollovers() throws InterruptedException {
    	List<Rollover> rollovers = contractService.initRollovers();
		rolloverContracts = new HashMap<Integer, ExtContract>();
		
    	int tickId = 0;
    	for (Rollover rollover : rollovers) {
    		
    		client.reqMktData(++tickId, rollover.getCurrentContract(), null, true);
    		rolloverContracts.put(tickId, rollover.getCurrentContract());
    		
    		client.reqMktData(++tickId, rollover.getNextContract(), null, true);
    		rolloverContracts.put(tickId, rollover.getNextContract());
    		
    		// IB limits snapshot requests to 100 per second
    		if (tickId % 100 == 0) {
    			Thread.sleep(1000);
    		}
    	}
	}
	
    public void reqHistoricalData(ExtContract contract) {
    	List<ExtContract> list = new ArrayList<ExtContract>();
    	list.add(contract);
    	reqHistoricalData(list, false);
    }

    public void reqHistoricalData (List<ExtContract> contracts, boolean calculateAtr) {

    	messageService.addInfoMessage("Requesting historical data...");

    	contractService.setCalculateAtr(calculateAtr);
        contractService.addHistDataContracts(contracts);

        String endDate = new SimpleDateFormat("yyyyMMdd").format(new Date()) + " 16:00:00";
        String duration = parameterService.getStringParameter("ENTRY_BREAKOUT_DAYS") + " D";

        for (ListIterator<ExtContract> it = contracts.listIterator(); it.hasNext();) {
            ExtContract contract = it.next();
            contract.getHistoricalData().clear();
            client.reqHistoricalData(it.previousIndex(), contract, endDate, duration, "1 day", "TRADES", 1, 1);
        }
    }
    
    public void placeOrder (Contract contract, Order order) {
    	order.m_auxPrice = contractService.convertPriceFromDollars(contract.m_symbol, order.m_auxPrice);
        String msg = "Placing order to " + order.m_action +
                    " " + order.m_totalQuantity + 
                    " " + contract.m_symbol + 
                    " @ " + order.m_auxPrice +
                    " for " + order.m_account;
        messageService.addInfoMessage(msg);
        
    	if (order.m_orderId == 0) {
            order.m_orderId = ++this.nextOrderId;
    	}
        client.placeOrder(order.m_orderId, contract, order);
        
    	order.m_auxPrice = contractService.convertPriceToDollars(contract.m_symbol, order.m_auxPrice);
        orderService.putOrder(contract.m_symbol, order);
    }

    public void cancelOrder (Contract contract, Order order) {
    	Double price = contractService.convertPriceFromDollars(contract.m_symbol, order.m_auxPrice);
        String msg = "Canceling order to " + order.m_action +
        		" " + order.m_totalQuantity + 
        		" " + contract.m_symbol + 
        		" @ " + price +
        		" for " + order.m_account;
        messageService.addInfoMessage(msg);
        client.cancelOrder(order.m_orderId);
    }
    
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
    	
    	if (tickType == TICK_TYPE_VOLUME) {
    		rolloverContracts.get(tickerId).setVolume(size);
    	}
    }

    public void tickOptionComputation(int tickerId, int tickType, double impliedVol, double delta, double modelPrice, double pvDividend) {
        throw new UnsupportedOperationException("tickOptionComputation is not supported");
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
        throw new UnsupportedOperationException("tickEFP is not supported");
    }

    public void tickSnapshotEnd(int reqId) {
    	String msg = EWrapperMsgGenerator.tickSnapshotEnd(reqId);
    	messageService.addDataMessage(msg);
    }

    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {

    	String msg = EWrapperMsgGenerator.orderStatus(
                orderId, status, filled, remaining, avgFillPrice, permId,
                parentId, lastFillPrice, clientId, whyHeld);
    	messageService.addDataMessage(msg);
        
        if (status.equals("Cancelled") || status.equals("Filled")) {
            orderService.removeOrder(orderId);
        } 
    }

    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
    	String msg = EWrapperMsgGenerator.openOrder(
                orderId, contract, order, orderState);
    	messageService.addDataMessage(msg);

        ExtContract c = contractService.getContract(contract.m_symbol);
        if (c == null) {
        	msg = "Contract not found for open order: " + contract.m_symbol;
        	messageService.addInfoMessage(msg);
        } else {
            order.m_auxPrice = contractService.convertPriceToDollars(contract.m_symbol, order.m_auxPrice);
            orderService.putOrder(contract.m_symbol, order);
        }
        
        // Adjust nextOrderId if TWS has messed it up somehow
        if (orderId >= nextOrderId) {
        	nextOrderId = orderId + 1;
        }
    }

    public void openOrderEnd() {
    	String msg = EWrapperMsgGenerator.openOrderEnd();
    	messageService.addDataMessage(msg);
    	orderService.validateOpenOrders();
	    orderService.updateOrders();
    }

    public void updateAccountValue(String key, String value, String currency, String accountName) {
    	String msg = EWrapperMsgGenerator.updateAccountValue(
                key, value, currency, accountName);
    	messageService.addDataMessage(msg);
    	try {
	        if (key.equals("NetLiquidation")) {
	        	accountService.updateAccountValue(accountName, Double.parseDouble(value));
	        }
    	} catch (Exception ex) {
        	messageService.addInfoMessage(ex.toString());
            disconnect();
    	}
    }

    public void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
    	String msg = EWrapperMsgGenerator.updatePortfolio(contract, position, marketPrice, marketValue, averageCost,
                unrealizedPNL, realizedPNL, accountName);
    	messageService.addDataMessage(msg);
    	
    	tradeService.validatePosition(accountName, contract.m_symbol, position);
    }

    public void updateAccountTime(String timeStamp) {
        String msg = EWrapperMsgGenerator.updateAccountTime(timeStamp);
    	messageService.addDataMessage(msg);
	}

    public void accountDownloadEnd(String accountName) {
    	String msg = EWrapperMsgGenerator.accountDownloadEnd(accountName);
    	messageService.addDataMessage(msg);
    }

    public void nextValidId(int orderId) {
    	String msg = EWrapperMsgGenerator.nextValidId(orderId);
    	messageService.addDataMessage(msg);
        this.nextOrderId = orderId;
    }

    public void contractDetails(int reqId, ContractDetails contractDetails) {
        throw new UnsupportedOperationException("contractDetails is not supported");
    }

    public void bondContractDetails(int reqId, ContractDetails contractDetails) {
        throw new UnsupportedOperationException("bondContractDetails is not supported");
    }

    public void contractDetailsEnd(int reqId) {
        throw new UnsupportedOperationException("contractDetailsEnd is not supported");
    }

    public void execDetails(int reqId, Contract contract, Execution execution) {
    	String msg = EWrapperMsgGenerator.execDetails(reqId, contract, execution);
    	messageService.addDataMessage(msg);
    	
    	executionService.handleExecution(contract, execution);
    }

    public void execDetailsEnd(int reqId) {
        throw new UnsupportedOperationException("execDetailsEnd is not supported");
    }

    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size) {
        throw new UnsupportedOperationException("updateMktDepth is not supported");
    }

    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, int size) {
        throw new UnsupportedOperationException("updateMktDepthL2 is not supported");
    }

    public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
        throw new UnsupportedOperationException("updateNewsBulletin is not supported");
    }

    public void managedAccounts(String accountsList) {
    	String msg = EWrapperMsgGenerator.managedAccounts(accountsList);
    	messageService.addDataMessage(msg);
    	
    	accountService.validateManagedAccounts(accountsList);
    }

    public void receiveFA(int faDataType, String xml) {
        throw new UnsupportedOperationException("receiveFA is not supported");
    }

    public void historicalData(int reqId, String date, double open, double high, double low, double close, int volume, int count, double WAP, boolean hasGaps) {
    	String msg = EWrapperMsgGenerator.historicalData(reqId, date, open, high, low, close, volume, count, WAP, hasGaps);
    	messageService.addDataMessage(msg);
    	
	    contractService.processHistData(reqId, date, high, low, close);
    }
    
    public void scannerParameters(String xml) {
        throw new UnsupportedOperationException("scannerParameters is not supported");
    }

    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
        throw new UnsupportedOperationException("scannerData is not supported");
    }

    public void scannerDataEnd(int reqId) {
        throw new UnsupportedOperationException("scannerDataEnd is not supported");
    }

    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count) {
        throw new UnsupportedOperationException("realtimeBar is not supported");
    }

    public void currentTime(long time) {
        throw new UnsupportedOperationException("currentTime is not supported");
    }

    public void fundamentalData(int reqId, String data) {
        throw new UnsupportedOperationException("fundamentalData is not supported");
    }

    public void deltaNeutralValidation(int reqId, UnderComp underComp) {
        throw new UnsupportedOperationException("deltaNeutralValidation is not supported");
    }

    public void error(Exception e) {
    	messageService.addInfoMessage(e.toString());
		disconnect();
    }

    public void error(String str) {
    	String msg = EWrapperMsgGenerator.error(str);
    	messageService.addInfoMessage(msg);
    }

    public void error(int id, int errorCode, String errorMsg) {
    	switch (errorCode) {
    		// Messages to ignore
    		case  165:
    		case 1100:
    		case 1102:
    		case 2103:
    		case 2104:
    		case 2105:
    		case 2106:
    		case 2107: 
    			break;
    		// Messages with special handling
    		case 2100:
    			client.reqAccountUpdates(true, null);
    			// Allow this fall through for now so I see when it happens
    			// break;
    		default: 
    			String msg = EWrapperMsgGenerator.error(id, errorCode, errorMsg);
    	    	messageService.addInfoMessage(msg);
    	}
    }

    public void connectionClosed() {
    	String msg = EWrapperMsgGenerator.connectionClosed();
    	messageService.addInfoMessage(msg);
    }

	public void tickOptionComputation(int tickerId, int field,
			double impliedVol, double delta, double optPrice,
			double pvDividend, double gamma, double vega, double theta,
			double undPrice) {
        throw new UnsupportedOperationException("tickOptionComputation is not supported");		
	}

	public void marketDataType(int reqId, int marketDataType) {
        throw new UnsupportedOperationException("marketDataType is not supported");		
	}

	public void commissionReport(CommissionReport commissionReport) {
    	String msg = EWrapperMsgGenerator.commissionReport(commissionReport);
    	messageService.addDataMessage(msg);
	}
}
