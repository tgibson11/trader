package trader.service;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import trader.dao.ContractDao;

@Service
public class ContractService {
	
	@Resource
	private ContractDao contractDao;
	
	/**
	 * Returns the exchange for the specified symbol
	 * @param symbol
	 * @return
	 */
	public String getExchange(String symbol) {
		return contractDao.getExchange(symbol);
	}
	
	/**
	 * Returns the price factor for the specified symbol.
	 * I.e, the value that when multiplied by the Trading Blox/CSI price gives the equivalent IB price 
	 * @param symbol
	 * @return
	 */
	public Double getPriceFactor(String symbol) {
		return contractDao.getPriceFactor(symbol);
	}
	
	/**
	 * Returns true if the specified symbol's actual expiry date is in the month prior to the "contract month"
	 * @param symbol
	 * @return
	 */
	public boolean hasPriorMonthExpiry(String symbol) {
		return contractDao.hasPriorMonthExpiry(symbol);
	}
	
	/**
	 * Returns the contract multiplier for the specified symbol.
	 * <p>
	 * Used to uniquely identify a contract when symbol and exchange are insufficient.
	 * 
	 * @param symbol
	 * @return
	 */
	public Integer getMultiplier(String symbol) {
		return contractDao.getMultiplier(symbol);
	}

	/**
	 * Returns the minimum price increment for the specified symbol.
	 * 
	 * @param symbol
	 * @return
	 */
	public Double getPriceIncrement(String symbol) {
		return contractDao.getPriceIncrement(symbol);
	}
	
}
