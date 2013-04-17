package trader.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDao extends NamedParameterJdbcDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired
    public AccountDao(DataSource dataSource) {
    	setDataSource(dataSource);
    }

    public List<String> getAccounts() {
        logger.info("Getting accounts");
        List<String> accounts = getNamedParameterJdbcTemplate().query(
        		" SELECT account_id" +
    			" FROM   account",
    			new MapSqlParameterSource(),
                new AccountMapper());
        return accounts;
    }
    
    public Double getAccountValue(String accountId) {
    	logger.info("Getting value of account" + accountId);
        return getNamedParameterJdbcTemplate().queryForObject(
        		" SELECT value" +
    			" FROM   account_hist" +
    			" WHERE  account_id = :accountId" +
    			" AND    date =" +
    			"   (SELECT MAX(date)" +
    			"    FROM   account_hist" +
    			"    WHERE account_id = :accountId)",
                new MapSqlParameterSource()
        			.addValue("accountId", accountId),
				Double.class);
    }
    
    public void insertAccountHist(String accountId, Double value) {   	
        logger.info("Inserting accountHist: " + accountId + ", " + value);
        int count = getNamedParameterJdbcTemplate().update(
        		" INSERT INTO account_hist (account_id, date, value)" +
                " VALUES (:accountId, NOW(), :value)",
            new MapSqlParameterSource()
        		.addValue("accountId", accountId)
                .addValue("value", value));
        logger.info("Rows affected: " + count);
    }

    public int updateAccountHist(String accountId, Date date, Double value, Double deposits, Double withdrawals) {   	
        logger.info("Updating accountHist: " + accountId + ", " + value);
        int count = getNamedParameterJdbcTemplate().update(
        		" UPDATE account_hist " +
        		" SET value = IFNULL(:value, value)," +
        		"     deposits = IFNULL(:deposits, deposits)," +
        		"     withdrawals = IFNULL(:withdrawals, withdrawals)," +
        		"     date = :date" +
        		" WHERE account_id = :accountId" +
        		" AND LAST_DAY(date) = LAST_DAY(:date)",
            new MapSqlParameterSource()
        		.addValue("accountId", accountId)
                .addValue("value", value)
                .addValue("deposits", deposits)
                .addValue("withdrawals", withdrawals)
                .addValue("date", date));
        logger.info("Rows affected: " + count);
        return count;
    }

    public int getUnitSize(String accountId, String symbol) {
        logger.info("Getting unit size: " + accountId + ", " + symbol);
        int unitSize = getNamedParameterJdbcTemplate().queryForInt(
        		" SELECT unit_size" +
    			" FROM   account_contract" +
    			" WHERE  account_id = :accountId" +
    			" AND    symbol = :symbol",
                new MapSqlParameterSource()
        			.addValue("accountId", accountId)
                	.addValue("symbol", symbol));
        return unitSize;
    }

    public void updateUnitSize(String accountId, String symbol, int unitSize) {
        logger.info("Updating accountContract: " + accountId + ", " + symbol);
        int count = getNamedParameterJdbcTemplate().update(
        		" UPDATE account_contract" +
                " SET    unit_size = :unitSize" +
                " WHERE  account_id = :accountId" +
                " AND    symbol = :symbol",
            new MapSqlParameterSource()
        		.addValue("unitSize", unitSize)
                .addValue("accountId", accountId)
                .addValue("symbol", symbol));
        logger.info("Rows affected: " + count);
    }
    
    public void adjustUnitSizes(String accountId) {
		// Set unit size to 0 when an open trade exists in another contract for the same commodity
    	logger.info("Adjusting unit sizes (1): " + accountId);
    	int count = getNamedParameterJdbcTemplate().update(
    			" UPDATE account_contract ac" +
    			" SET    unit_size = 0" +
    			" WHERE  account_id = :accountId" +
    			" AND    unit_size > 0" +
    			" AND EXISTS" +
    			"   (SELECT 1" +
    			"    FROM   contract c1," +
    			"           contract c2," +
    			"           trade t" +
    			"    WHERE  c1.symbol = ac.symbol" +
    			"    AND    c2.underlying_id = c1.underlying_id" +
    			"    AND    c2.symbol <> c1.symbol" +
    			"    AND    t.account_id = ac.account_id" +
    			"    AND    t.symbol = c2.symbol" +
    			"    AND    t.exit_dt IS NULL)",
    			new MapSqlParameterSource().addValue("accountId", accountId));
    	logger.info("Rows affected: " + count);
    	// Set unit size to 0 when a larger contract for the same commodity has unit size > 0
    	logger.info("Adjusting unit sizes (2): " + accountId);
    	count = getNamedParameterJdbcTemplate().update(
    			" UPDATE account_contract ac1" +
    			" SET    unit_size = 0" +
    			" WHERE  account_id = :accountId" +
    			" AND    unit_size > 0" +
    			" AND EXISTS" +
    			"   (SELECT 1" +
    			"    FROM   contract c1," +
    			"           contract c2," +
    			"           (SELECT symbol" +
    			"            FROM   account_contract" +
    			"            WHERE  account_id = :accountId" +
    			"            AND    unit_size > 0) ac2" +
    			"    WHERE  c1.symbol = ac1.symbol" +
    			"    AND    c2.underlying_id = c1.underlying_id" +
    			"    AND    c2.symbol <> c1.symbol" +
    			"    AND    c2.multiplier > c1.multiplier" +
    			"    AND    ac2.symbol = c2.symbol)",
    			new MapSqlParameterSource().addValue("accountId", accountId));
    	logger.info("Rows affected: " + count);
    }
    
    private class AccountMapper implements ParameterizedRowMapper<String> {
        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        	return rs.getString("account_id");
        }
    }
        
}
