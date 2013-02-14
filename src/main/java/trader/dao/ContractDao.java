package trader.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.stereotype.Repository;

import trader.domain.ExtContract;

@Repository
public class ContractDao extends NamedParameterJdbcDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());

    @Autowired
    public ContractDao(DataSource dataSource) {
    	setDataSource(dataSource);
    }

    public List<ExtContract> getContracts() {
        logger.info("Getting contracts");
        List<ExtContract> contracts = getNamedParameterJdbcTemplate().query(
    			" SELECT c.*, " +
    			"        e.exchange_cd," +
    			"        u.description," +
    			"        (SELECT SUM(quantity)" +
    			"         FROM   trade" +
    			"         WHERE  symbol = c.symbol" +
    			"         AND    exit_dt IS NULL) open_position_size" +
    			" FROM   contract c," +
    			"        exchange e," +
    			"        underlying u" +
    			" WHERE  e.exchange_id = c.exchange_id" +
    			" AND    u.underlying_id = c.underlying_id" +
    			" AND    c.active_flag = 1" +
    			" ORDER BY c.symbol",
    			new MapSqlParameterSource(),
                new ContractMapper());
        return contracts;
    }
    
    public ExtContract getContract(String symbol) {
        logger.info("Getting contract: " + symbol);
        ExtContract contract = getNamedParameterJdbcTemplate().queryForObject(
    			" SELECT c.*, " +
    			"        e.exchange_cd," +
    			"        u.description," +
    			"        (SELECT SUM(quantity)" +
    			"         FROM   trade" +
    			"         WHERE  symbol = c.symbol" +
    			"         AND    exit_dt IS NULL) open_position_size" +
    			" FROM   contract c," +
    			"        exchange e," +
    			"        underlying u" +
    			" WHERE  e.exchange_id = c.exchange_id" +
    			" AND    u.underlying_id = c.underlying_id" +
    			" AND    c.symbol = :symbol" +
    			" AND    c.active_flag = 1",
                new MapSqlParameterSource().addValue("symbol", symbol),
                new ContractMapper());
        return contract;
    }
    
    public void updateContract(ExtContract contract) {
        logger.info("Updating contract: " + contract.m_symbol);
        int count = getNamedParameterJdbcTemplate().update(
        		" UPDATE contract" +
                " SET    expiry = :expiry," +
                "        atr = :atr," +
                "        entry_high = :entryHigh," +
                "        entry_low = :entryLow," +
                "        exit_high = :exitHigh," +
                "        exit_low = :exitLow" +
                " WHERE  symbol = :symbol",
            new MapSqlParameterSource().addValue("expiry", contract.m_expiry)
                .addValue("atr", contract.getAtr())
                .addValue("entryHigh", contract.getEntryHigh())
                .addValue("entryLow", contract.getEntryLow())
                .addValue("exitHigh", contract.getExitHigh())
                .addValue("exitLow", contract.getExitLow())
                .addValue("symbol", contract.m_symbol));
        logger.info("Rows affected: " + count);
    }

    private static class ContractMapper implements ParameterizedRowMapper<ExtContract> {

        public ExtContract mapRow(ResultSet rs, int rowNum) throws SQLException {
            ExtContract contract = new ExtContract();
            contract.m_symbol = rs.getString("symbol");
            contract.m_secType = "FUT";
            contract.m_expiry = rs.getString("expiry");
            contract.m_exchange = rs.getString("exchange_cd");
            contract.m_currency = "USD";
            contract.setDescription(rs.getString("description"));
            contract.setMultiplier(rs.getInt("multiplier"));
            contract.setTickSize(rs.getDouble("tick_size"));
            contract.setPricedInCents(rs.getInt("cents_flag") != 0);
            contract.setOpenInterestUrl(rs.getString("open_interest_url"));
            contract.setAtr(rs.getDouble("atr"));
            contract.setEntryHigh(rs.getDouble("entry_high"));
            contract.setEntryLow(rs.getDouble("entry_low"));
            contract.setExitHigh(rs.getDouble("exit_high"));
            contract.setExitLow(rs.getDouble("exit_low"));
            contract.setOpenPosition(rs.getInt("open_position_size") != 0);
            return contract;
        }

    }
    
    public List<ExtContract> getAllContractMonths() {
        logger.info("Getting contract months");
        List<ExtContract> contracts = getNamedParameterJdbcTemplate().query(
    			" SELECT c.*, " +
    			"        e.exchange_cd," +
    			"        u.description," +
    			"        (SELECT SUM(quantity)" +
    			"         FROM   trade" +
    			"         WHERE  symbol = c.symbol" +
    			"         AND    exit_dt IS NULL) open_position_size," +
    			"        cm.month" +
    			" FROM   contract c," +
    			"        exchange e," +
    			"        underlying u," +
    			"        contract_month cm" +
    			" WHERE  e.exchange_id = c.exchange_id" +
    			" AND    u.underlying_id = c.underlying_id" +
    			" AND    cm.symbol = c.symbol" +
    			" AND    c.active_flag = 1" +
    			" ORDER BY c.symbol, cm.month",
    			new MapSqlParameterSource(),
                new ContractMonthMapper());
        return contracts;
    }
    
    private static class ContractMonthMapper extends ContractMapper {
    	
        public ExtContract mapRow(ResultSet rs, int rowNum) throws SQLException {
        	ExtContract contract = super.mapRow(rs, rowNum);
        	
        	Integer currentYear = Integer.parseInt(contract.m_expiry.substring(0,4));
        	Integer currentMonth = Integer.parseInt(contract.m_expiry.substring(4,6));
        	
           	String newYear = currentYear.toString();
            String newMonth = rs.getString("month");
      	
        	if (currentMonth > Integer.parseInt(newMonth)) {
        		newYear = String.valueOf((currentYear + 1));
        	}
        	
        	contract.m_expiry = newYear + newMonth;
        	return contract;
        }
    }

    public List<String> getCorrelatedSymbols(String symbol, String type) {
    	logger.info("Getting correlated symbols: " + symbol + ", " + type);
    	List<String> symbols = getNamedParameterJdbcTemplate().query(
        		" SELECT correlated_symbol" +
    			" FROM   correlation" +
    			" WHERE  symbol = :symbol" +
    			" AND    correlation_type = :type",
                new MapSqlParameterSource().addValue("symbol", symbol)
                		.addValue("type", type),
        		new CorrelationMapper());
    	return symbols;
    }

    private static class CorrelationMapper implements ParameterizedRowMapper<String> {

        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        	return rs.getString("correlated_symbol");
        }
    }

}
