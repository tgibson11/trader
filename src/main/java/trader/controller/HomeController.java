package trader.controller;

import static trader.constants.Constants.CSS_CLASS_SELECTED;
import static trader.constants.Constants.HOME_VIEW;
import static trader.constants.Constants.ROLLOVER_VIEW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import trader.domain.Message;
import trader.service.ClientService;
import trader.service.ContractService;
import trader.service.MessageService;

@Controller
public class HomeController {
	
    protected final Log logger = LogFactory.getLog(getClass());
    
    @Resource
    private ClientService clientService;
    
    @Resource
    private ContractService contractService;
    
    @Resource
    private MessageService messageService;
    
	@RequestMapping(value = "home", method = RequestMethod.GET)
    public String showForm(Map<String, Object> model) {

    	// Use a new list to avoid concurrentModificationException
    	List<Message> infoMessages = new ArrayList<Message>();    	
    	infoMessages.addAll(messageService.getInfoMessages());
    	
    	model.put("homeClass", CSS_CLASS_SELECTED);
    	model.put("info", infoMessages);
    	
    	return HOME_VIEW;
    }

	@RequestMapping(value = "home", method = RequestMethod.POST)
    public String onSubmit(HttpServletRequest request) throws Exception {
    	
    	if (request.getParameter("_connectTWS") != null) {
    		logger.info("Connecting to TWS...");
    		clientService.connect();
    	} else if (request.getParameter("_disconnectTWS") != null) {
    		logger.info("Disconnecting from TWS...");
    		clientService.disconnect();
    	} else if (request.getParameter("_checkRollovers") != null) {
    		logger.info("Checking for contracts to rollover...");
    		clientService.checkForRollovers();
        	logger.info("Redirecting to " + ROLLOVER_VIEW);   	
    		return "redirect:" + ROLLOVER_VIEW;
    	} else if (request.getParameter("_positionSizing") != null) {
    		logger.info("Starting position sizing...");
    		clientService.startPositionSizing();
    	}
    	
    	logger.info("Returning to " + HOME_VIEW);   	
        return "redirect:" + HOME_VIEW;
    }
    
}
