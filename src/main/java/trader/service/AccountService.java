package trader.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import trader.dao.AccountDao;
import trader.domain.Account;
import trader.domain.ExtContract;

@Service
public class AccountService {
	
	@Resource
	private ContractService contractService;
	
	@Resource
	private MessageService messageService;
	
	@Resource
	private ParameterService parameterService;
	
	@Resource
	private AccountDao accountDao;
	
	public List<Account> getAccounts() {
		return accountDao.getAccounts();
	}
	
	public Account getAccount(String accountId) {
		return accountDao.getAccount(accountId);
	}
	
	public boolean isValidAccount(String accountId) {
    	try {
    		getAccount(accountId);
    	} catch (Exception ex) {
    		return false;
    	}
    	return true;
	}
	
	public void validateManagedAccounts(String accountIds) {
		
		// Validate that each account from the database matches an IB managed account
		for (Account account : getAccounts()) {
			if (!accountIds.contains(account.getAccountId())) {
				messageService.addInfoMessage("Account " + account.getAccountId() + " is not an IB managed account!");
			}
		}
		
		// Validate that each IB managed account is defined in the database
		String advisorAccountId = parameterService.getStringParameter("ADVISOR_ACCOUNT_ID");
		for (String accountId : accountIds.split(",")) {
			if (!accountId.equals(advisorAccountId) && !isValidAccount(accountId)) {
				messageService.addInfoMessage("IB managed account " + accountId + " is not configured!");
			}
		}
		
	}
	
	public void updateAccountValue(String accountId, Double value) throws SQLException {
		updateAccountValue(accountId, new Date(), value, null, null);
	}
	
	public void updateAccountValue(String accountId, Date date, Double value, Double deposits, Double withdrawals) 
			throws SQLException {
		if (accountDao.updateAccountHist(accountId, date, value, deposits, withdrawals) == 0) {
			accountDao.insertAccountHist(accountId, value);
		}
	}
	
	public int getUnitSize(String account, String symbol) {
		return accountDao.getUnitSize(account, symbol);
	}
	
	public void updateUnitSize(String account, String symbol, int unitSize) {
		accountDao.updateUnitSize(account, symbol, unitSize);
	}
	
	public Double getAccountValue(String accountId) {
		return accountDao.getAccountValue(accountId);
	}
	
	public void calcUnitSizes(String accountId) {
		for (ExtContract contract : contractService.getContracts()) {
			updateUnitSize(accountId, contract.m_symbol, calcUnitSize(accountId, contract));
		}
		accountDao.adjustUnitSizes(accountId);
	}
	
	private Integer calcUnitSize(String accountId, ExtContract contract) {
		Double stopAtrMultiple = parameterService.getDoubleParameter("STOP_ATR_MULTIPLE");
		Double riskPerUnit = parameterService.getDoubleParameter("PERCENT_RISK_PER_UNIT");
		return (int) Math.floor(riskPerUnit * getAccountValue(accountId)
				/ (stopAtrMultiple * contract.getAtr() * contract.getMultiplier()));
		
	}
	
}
