package trader.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import trader.domain.Message;
import trader.service.MessageService;

@Controller
public class LogController {

    private static final String VIEW_NAME = "log"; 
    
    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired
    private MessageService messageService;
    
	@RequestMapping("log")
    protected String handleRequestInternal(Model model) throws Exception {
    	
    	// Use a new list to avoid concurrentModificationException
    	List<Message> dataMessages = new ArrayList<Message>();
    	dataMessages.addAll(messageService.getDataMessages());

    	model.addAttribute("data", dataMessages);
    	
    	return VIEW_NAME;
    }
    
}
