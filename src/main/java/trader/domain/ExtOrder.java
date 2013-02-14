package trader.domain;

import com.ib.client.Order;

public class ExtOrder extends Order {

	private String symbol;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	
}
