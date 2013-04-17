package trader.domain;

import java.util.ArrayList;
import java.util.List;

public class ActionItem {
	
	private String description;
	private List<ExtOrder> orders = new ArrayList<ExtOrder>();
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ExtOrder> getOrders() {
		return orders;
	}
	
	public void setOrders(List<ExtOrder> orders) {
		this.orders = orders;
	}
	
	public boolean addOrder(ExtOrder order) {
		return orders.add(order);
	}

}
