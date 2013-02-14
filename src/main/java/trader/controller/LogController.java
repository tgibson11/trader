package trader.controller;

import static trader.constants.Constants.CSS_CLASS_SELECTED;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import trader.domain.Message;
import trader.service.MessageService;

public class LogController extends AbstractController {

    protected final Log logger = LogFactory.getLog(getClass());
    
    @Resource
    private MessageService messageService;
    
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	ModelAndView mav = new ModelAndView("log");
    	
    	// Use a new list to avoid concurrentModificationException
    	List<Message> dataMessages = new ArrayList<Message>();
    	dataMessages.addAll(messageService.getDataMessages());

    	mav.addObject("logClass", CSS_CLASS_SELECTED);
    	mav.addObject("data", dataMessages);
    	
    	return mav;
    }
    
}
