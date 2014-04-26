package trader.command;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ActionItemsCommand implements Serializable {

	private static final long serialVersionUID = 1619020106350325733L;
	
	private Map<Integer, Boolean> actionItems = new HashMap<Integer, Boolean>();

	public Map<Integer, Boolean> getActionItems() {
		return actionItems;
	}

	public void setActionItems(Map<Integer, Boolean> actionItems) {
		this.actionItems = actionItems;
	}

}
