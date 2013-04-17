package trader.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import trader.domain.Message;

@Service
public class MessageService {

    private List<Message> dataMessages = new ArrayList<Message>();
    private List<Message> infoMessages = new ArrayList<Message>();
    
    public void addDataMessage(String msg) {
    	dataMessages.add(0, new Message(msg));
    }
    
    public void addInfoMessage(String msg) {
    	infoMessages.add(0, new Message(msg));
    }
    
	public List<Message> getDataMessages() {
		return dataMessages;
	}

	public List<Message> getInfoMessages() {
		return infoMessages;
	}
	
	public void clearDataMessages() {
		dataMessages.clear();
	}
	
	public void clearInfoMessages() {
		infoMessages.clear();
	}
	
}
