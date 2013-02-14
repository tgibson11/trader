package trader.domain;

import java.util.Date;

public class PerformanceData {

    private Date date;
    private Double bnav;
    private Double deposits;
    private Double withdrawals;
    private Double enav;
    private Double performance;
    private Double ror;
    private Double vami;
    private Double peakVami;
    private Double drawdown;
    private Double cagr;
    private Double expectedAnnualPerformance;
    
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public Double getBnav() {
		return bnav;
	}
	public void setBnav(Double bnav) {
		this.bnav = bnav;
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
	public Double getEnav() {
		return enav;
	}
	public void setEnav(Double enav) {
		this.enav = enav;
	}
	public Double getPerformance() {
		return performance;
	}
	public void setPerformance(Double performance) {
		this.performance = performance;
	}
	public Double getRor() {
		return ror;
	}
	public void setRor(Double ror) {
		this.ror = ror;
	}
	public Double getVami() {
		return vami;
	}
	public void setVami(Double vami) {
		this.vami = vami;
	}
	public Double getPeakVami() {
		return peakVami;
	}
	public void setPeakVami(Double peakVami) {
		this.peakVami = peakVami;
	}
	public Double getDrawdown() {
		return drawdown;
	}
	public void setDrawdown(Double drawdown) {
		this.drawdown = drawdown;
	}
	public Double getCagr() {
		return cagr;
	}
	public void setCagr(Double cagr) {
		this.cagr = cagr;
	}
	public Double getExpectedAnnualPerformance() {
		return expectedAnnualPerformance;
	}
	public void setExpectedAnnualPerformance(Double expectedAnnualPerformance) {
		this.expectedAnnualPerformance = expectedAnnualPerformance;
	}
    
}
