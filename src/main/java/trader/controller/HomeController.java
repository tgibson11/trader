package trader.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import trader.command.ActionItemsCommand;
import trader.domain.Message;
import trader.service.AccountService;
import trader.service.MessageService;
import trader.service.OrderService;
import trader.service.TwsApiService;

@Controller
public class HomeController {
	
	private static final String FORM_VIEW = "home";
	private static final String SUCCESS_VIEW = "redirect:" + FORM_VIEW;
	
    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TwsApiService twsApiService;
    
	@RequestMapping(value = "home", method = RequestMethod.GET)
    public String showForm(Model model) {

    	// Use a new list to avoid concurrentModificationException
    	List<Message> messages = new ArrayList<Message>();    	
    	messages.addAll(messageService.getInfoMessages());
    	
     	model.addAttribute("accounts", accountService.getAccounts());
    	model.addAttribute("messages", messages);
    	model.addAttribute("actionItems", orderService.getActionItems());
    	
    	return FORM_VIEW;
    }
	
	@RequestMapping(value="home", method=RequestMethod.POST, params={ "connect" })
	public String connect() {
		logger.info("Connecting to TWS...");
		twsApiService.connect();		
        return SUCCESS_VIEW;
	}

	@RequestMapping(value="home", method=RequestMethod.POST, params={ "disconnect" })
	public String disconnect() {
		logger.info("Disconnecting from TWS...");
		twsApiService.disconnect();
        return SUCCESS_VIEW;
	}

	@RequestMapping(value="home", method=RequestMethod.POST, params={ "clear" })
	public String clearMessages() {
		logger.info("Clearing messages...");
		messageService.clearInfoMessages();
        return SUCCESS_VIEW;
	}

	@RequestMapping(value="home", method=RequestMethod.POST, params={ "import" })
	public String importOrders(
			@RequestParam(value="account") String account,
    		@RequestParam(value="file") MultipartFile file) throws IOException {
		
		logger.info("Importing orders for account " + account);
		if (file != null && !file.isEmpty()) {
			orderService.importOrders(file, account);
		}
		
        return SUCCESS_VIEW;
	}

	@RequestMapping(value="home", method=RequestMethod.POST, params={ "submit" })
	public String submitActionItems(@ModelAttribute(value="command") ActionItemsCommand command) {
		logger.info("Processing action items...");
		orderService.processActionItems(command.getActionItems());
        return SUCCESS_VIEW;
	}

}
