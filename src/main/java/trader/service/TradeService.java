package trader.service;

import static trader.constants.Constants.HIGH_CORRELATION;
import static trader.constants.Constants.LONG;
import static trader.constants.Constants.LOW_CORRELATION;
import static trader.constants.Constants.SHORT;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import trader.dao.TradeDao;
import trader.domain.ExtContract;
import trader.domain.Trade;

@Service
public class TradeService {
	
	@Resource
	private ContractService contractService;
	
	@Resource
	private MessageService messageService;
	
	@Resource
	private ParameterService parameterService;
	
	@Resource
	private TradeDao tradeDao;

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
	
	public void sortTrades(List<Trade> trades) {
		// Sort trades by entry date, descending
		Collections.sort(trades, new Comparator<Trade>() {
		    public int compare(Trade o1, Trade o2) {
		        return -(o1.getEntryDate().compareTo(o2.getEntryDate()));
		    }});
	}
	
}
