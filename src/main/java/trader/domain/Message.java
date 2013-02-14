package trader.domain;

import java.util.Date;

public class Message {
	
	private Date date;
	private String text;
	
	public Message(String text) {
		this.date = new Date();
		this.text = text;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	public Date getDate() {
		return date;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getText() {
		return text;
	}

}
