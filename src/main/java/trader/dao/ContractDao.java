package trader.dao;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.stereotype.Repository;

@Repository
public class ContractDao extends NamedParameterJdbcDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());

    @Autowired
    public ContractDao(DataSource dataSource) {
    	setDataSource(dataSource);
    }
    
	/**
	 * Returns the exchange for the specified symbol
	 * @param symbol
	 * @return
	 */	
    public String getExchange(String symbol) {
    	try {
			return getNamedParameterJdbcTemplate().queryForObject(
					" SELECT exchange" +
					" FROM trd.contract" +
					" WHERE symbol = :symbol", 
					new MapSqlParameterSource("symbol", symbol), 
					String.class);
		} catch (EmptyResultDataAccessException e) {
			// Legacy contacts may not be defined in the contract table
			return null;
		}
	}
    
	/**
	 * Returns the price factor for the specified symbol.
	 * I.e, the value that when multiplied by the Trading Blox/CSI price gives the equivalent IB price 
	 * @param symbol
	 * @return
	 */
	public Double getPriceFactor(String symbol) {
		return getNamedParameterJdbcTemplate().queryForObject(
				" SELECT price_factor" +
				" FROM trd.contract" +
				" WHERE symbol = :symbol", 
				new MapSqlParameterSource("symbol", symbol), 
				Double.class);
	}
	
	/**
	 * Returns true if the specified symbol's actual expiry date is in the month prior to the "contract month"
	 * @param symbol
	 * @return
	 */
	public boolean hasPriorMonthExpiry(String symbol) {
		try {
			return getNamedParameterJdbcTemplate().queryForObject(
					" SELECT prior_month_expiry" +
					" FROM trd.contract" +
					" WHERE symbol = :symbol", 
					new MapSqlParameterSource("symbol", symbol), 
					Boolean.class);
		} catch (EmptyResultDataAccessException e) {
			// Legacy contacts may not be defined in the contract table
			return false;
		}
	}

}
