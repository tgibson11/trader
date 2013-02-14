package trader.command;

import java.util.Date;

public class PerformanceCommand {

    private String accountId;
    private Date date;
    private Double deposits;
    private Double withdrawals;
    private Double nav;
    
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	public String getAccountId() {
		return accountId;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public Double getDeposits() {
		return deposits;
	}
	public void setDeposits(Double deposits) {
		this.deposits = deposits;
	}
	public Double getWithdrawals() {
		return withdrawals;
	}
	public void setWithdrawals(Double withdrawals) {
		this.withdrawals = withdrawals;
	}
	public Double getNav() {
		return nav;
	}
	public void setNav(Double nav) {
		this.nav = nav;
	}

}
