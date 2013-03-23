package trader.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import trader.dao.ContractDao;
import trader.domain.ExtContract;
import trader.domain.HistoricalData;
import trader.domain.Rollover;

@Service
public class ContractService {
	
	private static final String EXPIRY_DATE_PATTERN = "yyyyMM";

	@Resource
	private ClientService clientService;
	
	@Resource
	private ContractDao contractDao;
	
	@Resource
	private ExecutionService executionService;
	
	@Resource
	private OrderService orderService;
	
	@Resource
	private ParameterService parameterService;
		
    private Map<Integer, ExtContract> histDataContracts = new HashMap<Integer, ExtContract>();
    private List<Rollover> rollovers = new ArrayList<Rollover>();
    private boolean calculateAtr;
    
	public List<ExtContract> getContracts() {
		return contractDao.getContracts();
	}
	
	public ExtContract getContract(String symbol) {
		return contractDao.getContract(symbol);
	}
	
	public boolean isValidContract(String symbol) {
    	try {
    		getContract(symbol);
    	} catch (Exception ex) {
    		return false;
    	}
		return true;
	}
	
	public void initHistDataContracts() {
		histDataContracts.clear();
	}
	
	public void addHistDataContracts(List<ExtContract> histDataContracts) {
        for (ListIterator<ExtContract> it = histDataContracts.listIterator(); it.hasNext();) {
            ExtContract contract = it.next();
			this.histDataContracts.put(it.previousIndex(), contract);
		}
	}

	public void processHistData(int reqId, String date, double high, double low, double close) {

    	boolean finished = false;
    	ExtContract contract = histDataContracts.get(reqId);
    	
        if (!date.startsWith("finished")) {        	
        	SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            HistoricalData histData = new HistoricalData();
    		try {
				histData.setDate(formatter.parse(date));
			} catch (ParseException e) {
				clientService.error(e);
			}
            histData.setHigh(convertPriceToDollars(contract.m_symbol, high));
            histData.setLow(convertPriceToDollars(contract.m_symbol, low));
            histData.setClose(convertPriceToDollars(contract.m_symbol, close));
            contract.getHistoricalData().add(histData);         	
        } else {
        	// Finished current contract 
        	if (calculateAtr) {
                calcAtr(contract);
        	}
            calcStopPrices(contract);            
			contractDao.updateContract(contract);
			histDataContracts.remove(reqId);
            if (histDataContracts.isEmpty()) {
            	finished = true;
            } 
        }
        
    	if (finished) {
        	executionService.setRolloverSymbol(null);
        	orderService.updateOrders();
    	}

    }

    public Double convertPriceToDollars(String symbol, Double price) {
    	Double dollars;
    	if (getContract(symbol).getPricedInCents()) {
    		dollars = price / 100;
    	} else {
    		dollars = price;
    	}
    	return dollars;
    }
    
    public Double convertPriceFromDollars(String symbol, Double dollars) {
    	Double price;
    	if (getContract(symbol).getPricedInCents()) {
    		price = dollars * 100;
    	} else {
    		price = dollars;
    	}
    	return price;
    }

	public void calcAtr(ExtContract contract) {
		
		List<HistoricalData> historicalData = contract.getHistoricalData();
		Integer historicalDataSize = historicalData.size();
		HistoricalData histData1 = historicalData.get(historicalDataSize - 1);
		HistoricalData histData2 = historicalData.get(historicalDataSize - 2);
		
		Double trueRange = Math.max(Math.max(
				histData1.getHigh() - histData1.getLow(), 
				histData1.getHigh() - histData2.getClose()), 
				histData2.getClose() - histData1.getLow());
		
		Integer atrConstant = parameterService.getIntParameter("ATR_CONSTANT");
		Double atr = ((atrConstant - 1) * contract.getAtr() + trueRange) / atrConstant;

		contract.setAtr(atr);
	}
	
	public void calcStopPrices(ExtContract contract) {
		
		List<HistoricalData> historicalData = contract.getHistoricalData();
		Double high = 0.0;
		Double low = Double.POSITIVE_INFINITY;
		int i = 1;
		
		// Loop backwards through historical data
		for (ListIterator<HistoricalData> it = historicalData.listIterator(historicalData.size()); it.hasPrevious(); i++) {
			HistoricalData histData = it.previous();
			
			if (histData.getHigh() > high) {
				high = histData.getHigh();
			}			
			if (histData.getLow() < low) {
				low = histData.getLow();
			}
			Integer exitBreakoutDays = parameterService.getIntParameter("EXIT_BREAKOUT_DAYS");
			if (i == exitBreakoutDays
					|| (i < exitBreakoutDays && !it.hasPrevious())) {
				contract.setExitHigh(high);
				contract.setExitLow(low);
			}
			Integer entryBreakoutDays = parameterService.getIntParameter("ENTRY_BREAKOUT_DAYS");
			if (i == entryBreakoutDays
					|| (i < entryBreakoutDays && !it.hasPrevious())) {
				contract.setEntryHigh(high);
				contract.setEntryLow(low);
			}
			
		}
	}
	
    public List<String> getCorrelatedSymbols(String symbol, String type) {
    	return contractDao.getCorrelatedSymbols(symbol, type);
    }
    
    public void updateContract(ExtContract contract) {
    	contractDao.updateContract(contract);
    }

	public List<String> getExpiries() {
		List<String> expiries = new ArrayList<String>();
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat expiryDateFormat = new SimpleDateFormat(EXPIRY_DATE_PATTERN);
		for (int i = 0; i < 12; i++) {
			String expiry = expiryDateFormat.format(calendar.getTime());
			expiries.add(expiry);
			calendar.add(Calendar.MONTH, 1);
		}
		return expiries;
	}
	
	public List<Rollover> initRollovers() {
		rollovers.clear();
		List<ExtContract> contracts = contractDao.getAllContractMonths();

		// At this point the list is sorted by symbol, then month
		// This will sort it by symbol, then expiry, taking the year into account
		Collections.sort(contracts);
		
		String symbol = null;
		int count = 0;
		Rollover rollover = null;
        for (ExtContract contract : contracts) {
            if (contract.getSymbol().equals(symbol)) {
                count++;
                if (count == 2) {
                	// This is the next expiry for the current symbol
                	rollover.setNextContract(contract);
                	rollovers.add(rollover);
                }
            } else {
            	// This is the current expiry for a new symbol
            	symbol = contract.getSymbol();
            	count = 1;
            	rollover = new Rollover();
            	rollover.setCurrentContract(contract);
            }
        }
        return rollovers;
	}

	public List<Rollover> getRollovers() {
		List<Rollover> rollovers = new ArrayList<Rollover>();
		
		for (Rollover rollover : this.rollovers) {
			Integer currentVolume = rollover.getCurrentContract().getVolume();
			Integer nextVolume = rollover.getNextContract().getVolume();
			if (currentVolume == null || nextVolume == null|| nextVolume > currentVolume) {
				rollovers.add(rollover);
			}
		}
		
		return rollovers;
	}
	
	public boolean isCalculateAtr() {
		return calculateAtr;
	}
	
	public void setCalculateAtr(boolean calculateAtr) {
		this.calculateAtr = calculateAtr;
	}
		
}
