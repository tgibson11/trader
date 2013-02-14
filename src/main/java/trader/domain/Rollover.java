package trader.domain;

public class Rollover {
	
	private ExtContract currentContract;
	private ExtContract nextContract;
	
	public ExtContract getCurrentContract() {
		return currentContract;
	}
	
	public void setCurrentContract(ExtContract currentContract) {
		this.currentContract = currentContract;
	}
	
	public ExtContract getNextContract() {
		return nextContract;
	}
	
	public void setNextContract(ExtContract nextContract) {
		this.nextContract = nextContract;
	}
	
}
