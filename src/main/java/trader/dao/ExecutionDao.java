package trader.dao;

import static trader.constants.Constants.DATE_TIME_PATTERN;

import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.stereotype.Repository;

import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.Order;

@Repository
public class ExecutionDao extends NamedParameterJdbcDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());

    @Autowired
    public ExecutionDao(DataSource dataSource) {
    	setDataSource(dataSource);
    }

    /**
     * Inserts a record to the execution table
     * @param execution
     * @param contract
     * @param order may be null
     * @throws ParseException
     */
    public void insertExecution(Execution execution, Contract contract, Order order) throws ParseException {
        
    	logger.info("Inserting execution: " + execution.m_execId);

    	// Convert execution date from string to Date
    	Date executionDt = new SimpleDateFormat(DATE_TIME_PATTERN).parse(execution.m_time);
    	
    	// Check for a null order
    	Double orderPrice = null;
    	if (order != null && order.m_auxPrice > 0) {
    		orderPrice = order.m_auxPrice;
    	}
        
        int count = getNamedParameterJdbcTemplate().update(
        		" INSERT INTO execution (" +
                "	execution_dt," +
                "   account_id," +
                "	order_id," +
                "	symbol," +
                "	action," +
                "	quantity," +
                "	price," +
                "	order_price)" +
                " VALUES (" +
                "	:executionDt," +
                "	:accountId," +
                "	:orderId," +
                "	:symbol," +
                "	:action," +
                "	:quantity," +
                "	:price," +
                "	:orderPrice)",
            new MapSqlParameterSource()
        		.addValue("executionDt", executionDt, Types.TIMESTAMP)
        		.addValue("accountId", execution.m_acctNumber)
                .addValue("orderId", execution.m_orderId)
                .addValue("symbol", contract.m_symbol)
                .addValue("action", execution.m_side)
                .addValue("quantity", execution.m_cumQty)
                .addValue("price", execution.m_avgPrice)
                .addValue("orderPrice", orderPrice));
        
        logger.info("Rows inserted: " + count);        
    }

}
