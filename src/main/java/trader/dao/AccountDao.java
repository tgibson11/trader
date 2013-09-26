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

import trader.domain.Account;

@Repository
public class AccountDao extends NamedParameterJdbcDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired
    public AccountDao(DataSource dataSource) {
    	setDataSource(dataSource);
    }

    public List<Account> getAccounts() {
        logger.info("Getting accounts");
        List<Account> accounts = getNamedParameterJdbcTemplate().query(
        		" SELECT account_id, account_name, default_flag" +
    			" FROM   account" +
    			" ORDER BY default_flag DESC, account_id",
    			new MapSqlParameterSource(),
                new AccountMapper());
        return accounts;
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

    private class AccountMapper implements ParameterizedRowMapper<Account> {
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
        	Account account = new Account();
        	account.setAccountId(rs.getString("account_id"));
        	account.setAccountName(rs.getString("account_name"));
        	account.setDefaultFlag(rs.getBoolean("default_flag"));
        	return account;
        }
    }
        
}
