package trader.dao;

import java.sql.Date;
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

import trader.domain.Trade;

@Repository
public class TradeDao extends NamedParameterJdbcDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());

    @Autowired
    public TradeDao(DataSource dataSource) {
    	setDataSource(dataSource);
    }

	/**
	 * Returns all open trades from the TRADE table
	 * for the specified account and symbol (if specified)
	 * @param accountId pass null to ignore
	 * @param symbol pass null to ignore
	 * @return
	 */
    public List<Trade> getOpenTrades(String accountId, String symbol) {
        logger.info("Getting open trades: " + accountId + ", " + symbol);
        List<Trade> trades = getNamedParameterJdbcTemplate().query(
        		" SELECT *" +
    			" FROM   trade" +
    			" WHERE  (account_id = :accountId OR :accountId is null)" +
    			" AND    (symbol = :symbol OR :symbol is null)" +
    			" AND    exit_dt IS NULL",
                new MapSqlParameterSource()
        			.addValue("accountId", accountId)
                	.addValue("symbol", symbol),
        		new TradeMapper());
        return trades;
    }
    
    public Integer getPosition(String accountId, String symbol) {
    	logger.info("Getting position: " + accountId + ", " + symbol);
    	Integer position = getNamedParameterJdbcTemplate().queryForInt(
    			" SELECT SUM(quantity)" +
    			" FROM   trade" +
    			" WHERE  account_id = :accountId" +
    			" AND    symbol = :symbol" +
    			" AND    exit_dt IS NULL", 
    			new MapSqlParameterSource()
    				.addValue("accountId", accountId)
    				.addValue("symbol", symbol));
    	return position;
    }

    public Trade insertTrade(String accountId, String symbol, Trade trade) {
        logger.info("Inserting trade: " + trade.getTradeId());
        int count = getNamedParameterJdbcTemplate().update(
        		" INSERT INTO trade" +
                " (account_id," +
                "  entry_dt," +
                "  symbol," +
                "  quantity," +
                "  entry_price," +
                "  stop_price)" +
                " VALUES " +
                " (:accountId," +
                "  :entryDt," +
                "  :symbol," +
                "  :quantity," +
                "  :entryPrice," +
                "  :stopPrice)",
            new MapSqlParameterSource().addValue("accountId", accountId)
                .addValue("entryDt", new Date(trade.getEntryDate().getTime()))
                .addValue("symbol", symbol)
                .addValue("quantity", trade.getQuantity())
                .addValue("entryPrice", trade.getEntryPrice())
                .addValue("stopPrice", trade.getStopPrice()));
        logger.info("Rows affected: " + count);
        
    	trade.setTradeId(getInsertedTradeId());
    	
    	return trade;
        
    }

    public void updateTrade(Trade trade) {
        logger.info("Updating trade: " + trade.getTradeId());
        Date exitDate = null;
    	if (trade.getExitDate() != null) {
            exitDate = new Date(trade.getExitDate().getTime());
    	}
        int count = getNamedParameterJdbcTemplate().update(
        		" UPDATE trade" +
                " SET    entry_price = :entryPrice," +
                "        stop_price = :stopPrice," +
                "        exit_dt = :exitDt," +
                "        exit_price = :exitPrice" +
                " WHERE  trade_id = :tradeId",
            new MapSqlParameterSource().addValue("entryPrice", trade.getEntryPrice())
                .addValue("stopPrice", trade.getStopPrice())
                .addValue("exitDt", exitDate)
                .addValue("exitPrice", trade.getExitPrice())
                .addValue("tradeId", trade.getTradeId()));
        logger.info("Rows affected: " + count);
    }

    private int getInsertedTradeId() {
        logger.info("Getting inserted tradeId");
        return getNamedParameterJdbcTemplate().queryForInt(
        		"SELECT LAST_INSERT_ID()", 
        		new MapSqlParameterSource());
    }

    private static class TradeMapper implements ParameterizedRowMapper<Trade> {

        public Trade mapRow(ResultSet rs, int rowNum) throws SQLException {
        	Trade trade = new Trade();
            trade.setTradeId(rs.getInt("trade_id"));
            trade.setAccountId(rs.getString("account_id"));
            trade.setSymbol(rs.getString("symbol"));
            trade.setEntryDate(rs.getDate("entry_dt"));
            trade.setQuantity(rs.getInt("quantity"));
            trade.setEntryPrice(rs.getDouble("entry_price"));
            trade.setStopPrice(rs.getDouble("stop_price"));
            return trade;
        }

    }
    
}
