package trader.controller;

import static org.apache.commons.lang.StringUtils.isBlank;
import static trader.constants.Constants.CSS_CLASS_SELECTED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	private static final String	ACTION_CLEAR = "clear";
	private static final String	ACTION_IMPORT = "import";
	
    protected final Log logger = LogFactory.getLog(getClass());
    
    @Resource
    private AccountService accountService;
    
    @Resource
    private MessageService messageService;
    
    @Resource
    private OrderService orderService;
    
    @Resource
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
    	
		if (isBlank(command.getAction())) {
    		logger.info("No action specified...");
		} else if (command.getAction().equalsIgnoreCase(ACTION_CONNECT)) {
    		logger.info("Connecting to TWS...");
    		twsApiService.connect();
		} else if (command.getAction().equalsIgnoreCase(ACTION_DISCONNECT)) {
    		logger.info("Disconnecting from TWS...");
    		twsApiService.disconnect();
		} else if (command.getAction().equalsIgnoreCase(ACTION_CLEAR)) {
    		logger.info("Clearing messages...");
    		messageService.clearInfoMessages();
		} else if (command.getAction().equalsIgnoreCase(ACTION_IMPORT)) {
    		logger.info("Import requested...");
    		importOrders(command, bindingResult);
    	}
    	
    	logger.info("Returning to " + VIEW);   	
        return showForm(command, model);
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
