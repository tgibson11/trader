package trader.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import trader.dao.AccountDao;

@Service
public class AccountService {
	
	@Resource
	private AccountDao accountDao;
	
	public List<String> getAccounts() {
		return accountDao.getAccounts();
	}
	
	public void updateAccountValue(String accountId, Double value) throws SQLException {
		updateAccountValue(accountId, new Date(), value, null, null);
	}
	
	public void updateAccountValue(String accountId, Date date, Double value, Double deposits, Double withdrawals) throws SQLException {
		if (accountDao.updateAccountHist(accountId, date, value, deposits, withdrawals) == 0) {
			accountDao.insertAccountHist(accountId, value);
		}
	}
	
}
