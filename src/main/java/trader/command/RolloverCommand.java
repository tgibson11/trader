package trader.command;


public class RolloverCommand {

    private String symbol;
    private String expiry;
    
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setExpiry(String expiry) {
		this.expiry = expiry;
	}

	public String getExpiry() {
		return expiry;
	}

}
