package trader.controller;

import static org.apache.commons.lang.StringUtils.isBlank;
import static trader.constants.Constants.CSS_CLASS_SELECTED;

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
	
	private static final String VIEW = "home";
	private static final String	ACTION_CONNECT = "connect";
	private static final String	ACTION_DISCONNECT = "disconnect";
	private static final String	ACTION_CLEAR = "clear messages";
	private static final String	ACTION_IMPORT = "import orders";
	private static final String	ACTION_SUBMIT = "submit action items";
	
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
    	
    	model.addAttribute("homeClass", CSS_CLASS_SELECTED);
    	model.addAttribute("accounts", accountService.getAccounts());
    	model.addAttribute("messages", messages);
    	model.addAttribute("actionItems", orderService.getActionItems());
    	
    	return VIEW;
    }

	@RequestMapping(value = "home", method = RequestMethod.POST)
    public String onSubmit(
    		@RequestParam(value="action") String action,
    		@RequestParam(value="account") String account,
    		@RequestParam(value="file") MultipartFile file,
    		@ModelAttribute(value="command") ActionItemsCommand command) throws IOException {
    	
		if (isBlank(action)) {
			
    		logger.info("No action specified...");
    		
		} else if (action.equalsIgnoreCase(ACTION_CONNECT)) {
    		
			logger.info("Connecting to TWS...");
    		twsApiService.connect();
    		
		} else if (action.equalsIgnoreCase(ACTION_DISCONNECT)) {
    		
			logger.info("Disconnecting from TWS...");
    		twsApiService.disconnect();
    		
		} else if (action.equalsIgnoreCase(ACTION_CLEAR)) {
    		
			logger.info("Clearing messages...");
    		messageService.clearInfoMessages();
    		
		} else if (action.equalsIgnoreCase(ACTION_IMPORT)) {
    		
			logger.info("Import requested...");
    		importOrders(account, file);
    		
    	} else if (action.equalsIgnoreCase(ACTION_SUBMIT)) {
    		
    		logger.info("Processing action items...");
    		orderService.processActionItems(command.getActionItems());
    	}
    	
    	logger.info("Returning to " + VIEW);   	
        return "redirect:" + VIEW;
    }
	
	/**
	 * Import orders from file into account
	 * @param account
	 * @param file
	 * @throws IOException
	 */
	private void importOrders(String account, MultipartFile file) throws IOException {
		logger.info("account=" + account);
		if (file != null && !file.isEmpty()) {
			orderService.importOrders(file, account);
		}
	}
    
}
