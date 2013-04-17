package trader.command;

import java.io.Serializable;

import org.springframework.web.multipart.MultipartFile;

public class ImportOrdersCommand implements Serializable {

	private static final long serialVersionUID = 1619020106350325733L;
	
	private String action;
	private String account;
	private MultipartFile file;

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public MultipartFile getFile() {
		return file;
	}

	public void setFile(MultipartFile file) {
		this.file = file;
	}

}
