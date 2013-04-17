package trader.domain;

import com.ib.client.Contract;

public class Position {
	
	private Contract contract;
	private Integer quantity;
	
	public Contract getContract() {
		return contract;
	}
	
	public void setContract(Contract contract) {
		this.contract = contract;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

}
