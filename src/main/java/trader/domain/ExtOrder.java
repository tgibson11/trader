package trader.domain;

import trader.utils.MathUtils;

import com.ib.client.Contract;
import com.ib.client.Order;

public class ExtOrder extends Order {
	
	private Contract contract;

	public ExtOrder() {

	}
	
	public ExtOrder(Contract contract, Order order) {
		m_orderId = order.m_orderId;
        m_action = order.m_action;
        m_totalQuantity = order.m_totalQuantity;
        m_orderType = order.m_orderType;
        m_tif = order.m_tif;
        m_auxPrice = order.m_auxPrice;
        m_account = order.m_account;
		this.contract = contract;	
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_action == null) ? 0 : m_action.hashCode())
				+ Integer.valueOf(m_totalQuantity).hashCode()
				+ ((contract == null) ? 0 : ((contract.m_symbol == null) ? 0 : contract.m_symbol.hashCode()))
				+ ((contract == null) ? 0 : ((contract.m_expiry == null) ? 0 : contract.m_expiry.hashCode()))
				+ Double.valueOf(m_auxPrice).hashCode()
				+ ((m_account == null) ? 0 : m_account.hashCode())
				+ ((m_orderType == null) ? 0 : m_orderType.hashCode())
				+ ((m_tif == null) ? 0 : m_tif.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExtOrder other = (ExtOrder) obj;
		if (m_action == null) {
			if (other.m_action != null) {
				return false;
			}
		} else if (!m_action.equals(other.m_action)) 
			return false;
		if (m_totalQuantity != other.m_totalQuantity) 
			return false;
		if (!MathUtils.equals(m_auxPrice, other.m_auxPrice)) 
			return false;
		if (m_account == null) {
			if (other.m_account != null) {
				return false;
			}
		} else if (!m_account.equals(other.m_account)) 
			return false;
		if (m_orderType == null) {
			if (other.m_orderType != null) {
				return false;
			}
		} else if (!m_orderType.equals(other.m_orderType)) 
			return false;
		if (m_tif == null) {
			if (other.m_tif != null) {
				return false;
			}
		} else if (!m_tif.equals(other.m_tif)) 
			return false;
		if (contract == null) {
			if (other.contract != null)
				return false;
		} else if (contract.m_symbol == null) {
			if (other.contract.m_symbol != null) {
				return false;
			}
		} else if (other.contract == null) {
			return false;
		} else if (!contract.m_symbol.equals(other.contract.m_symbol)) {
			return false;
		} else if (contract.m_expiry == null) {
			if (other.contract.m_expiry != null) {
				return false;
			}
		} else if (!contract.m_expiry.equals(other.contract.m_expiry)) {
			return false;
		}
		return true;
	}
	
}
