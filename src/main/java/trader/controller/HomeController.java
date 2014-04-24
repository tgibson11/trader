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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import trader.command.ImportOrdersCommand;
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
    public String showForm(@ModelAttribute("command") ImportOrdersCommand command, Model model) {

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
    public String onSubmit(@ModelAttribute("command") ImportOrdersCommand command, BindingResult bindingResult, Model model) throws IOException {
    	
		String action = command.getAction();
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
    		importOrders(command, bindingResult);
    		
    	} else if (action.equalsIgnoreCase(ACTION_SUBMIT)) {
    		
    		logger.info("Processing action items...");
    		orderService.processActionItems(command.getSubmittedActionItems());
    	}
    	
    	logger.info("Returning to " + VIEW);   	
        return "redirect:" + VIEW;
    }
	
	/**
	 * Do simple validation and call service method to import orders
	 * @param command
	 * @param bindingResult
	 * @return 
	 * @throws IOException 
	 */
	private void importOrders(ImportOrdersCommand command, BindingResult bindingResult) throws IOException {
		logger.info("account=" + command.getAccount());
		MultipartFile file = command.getFile();
		if (file == null || file.isEmpty()) {
			Object[] args = { "File" };
			bindingResult.rejectValue("file", "error.required", args, "Required");
		} else {
			orderService.importOrders(file, command.getAccount());
		}
	}
    
}
