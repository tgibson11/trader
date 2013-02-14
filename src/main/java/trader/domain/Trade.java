package trader.domain;

import java.util.Date;

public class Trade {

	private Integer tradeId;
	private String accountId;
	private String symbol;
	private Date entryDate;
	private Date exitDate;
	private Integer quantity;
	private Double entryPrice;
	private Double stopPrice;
	private Double exitPrice;
	
	public void setTradeId(Integer tradeId) {
		this.tradeId = tradeId;
	}
	public Integer getTradeId() {
		return tradeId;
	}
	public String getAccountId() {
		return accountId;
	}
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getSymbol() {
		return symbol;
	}
	public Date getEntryDate() {
		return entryDate;
	}
	public void setEntryDate(Date entryDate) {
		this.entryDate = entryDate;
	}
	public void setExitDate(Date exitDate) {
		this.exitDate = exitDate;
	}
	public Date getExitDate() {
		return exitDate;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	public Double getEntryPrice() {
		return entryPrice;
	}
	public void setEntryPrice(Double entryPrice) {
		this.entryPrice = entryPrice;
	}
	public Double getStopPrice() {
		return stopPrice;
	}
	public void setStopPrice(Double stopPrice) {
		this.stopPrice = stopPrice;
	}
	public void setExitPrice(Double exitPrice) {
		this.exitPrice = exitPrice;
	}
	public Double getExitPrice() {
		return exitPrice;
	}
	
}
