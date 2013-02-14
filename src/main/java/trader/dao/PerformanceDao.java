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

import trader.domain.PerformanceData;

@Repository
public class PerformanceDao extends NamedParameterJdbcDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());
        
    @Autowired
    public PerformanceDao(DataSource dataSource) {
    	setDataSource(dataSource);
    }

	public List<PerformanceData> getPerformanceData(String accountId) {
        logger.info("Getting performance data: " + accountId);
        List<PerformanceData> performanceData = getNamedParameterJdbcTemplate().query(
        		" SELECT curr.*," +
        		"        prev.value bnav," +
        		"        curr.value - (prev.value - curr.withdrawals + curr.deposits) performance" +
    			" FROM   account_hist curr," +
    			"        account_hist prev" +
    			" WHERE  prev.account_id = curr.account_id" +
    			" AND    LAST_DAY(prev.date) = LAST_DAY(DATE_ADD(curr.date, INTERVAL -1 MONTH))" +
    			" AND    curr.account_id = :accountId" +
    			" ORDER BY curr.date",
                new MapSqlParameterSource().addValue("accountId", accountId),
                new PerformanceDataMapper());
        return performanceData;
	}
	
    private static class PerformanceDataMapper implements ParameterizedRowMapper<PerformanceData> {

        public PerformanceData mapRow(ResultSet rs, int rowNum) throws SQLException {
        	PerformanceData performanceData = new PerformanceData();
        	performanceData.setDate(rs.getDate("date"));
        	performanceData.setBnav(rs.getDouble("bnav"));
        	performanceData.setDeposits(rs.getDouble("deposits"));
        	performanceData.setWithdrawals(rs.getDouble("withdrawals"));
        	performanceData.setEnav(rs.getDouble("value"));
        	performanceData.setPerformance(rs.getDouble("performance"));
            return performanceData;
        }

    }

}
