package trader.controller;

import static trader.constants.Constants.CSS_CLASS_SELECTED;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

import trader.command.PerformanceCommand;
import trader.domain.PerformanceData;
import trader.service.AccountService;
import trader.service.PerformanceService;

public class PerformanceController extends SimpleFormController {

    protected final Log logger = LogFactory.getLog(getClass());
    
    @Resource
    private AccountService accountService;
    
    @Resource
    private PerformanceService performanceService;
    
    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
    	PerformanceCommand command = new PerformanceCommand();
    	String accountId = request.getParameter("account");
    	if (accountId != null) {
    		command.setAccountId(accountId);
    	} else {
        	command.setAccountId(accountService.getAccounts().get(0));
    	}
    	return command;
    }
    
    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
    	binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("MM/dd/yyyy"), false));
	}
    
    @Override
    protected Map<String, Object> referenceData(HttpServletRequest request, Object command, Errors errors) throws Exception {
    	Map<String, Object> map = new HashMap<String, Object>();
    	PerformanceCommand cmd = (PerformanceCommand) command;
        List<PerformanceData> performanceData = performanceService.getPerformanceData(cmd.getAccountId());
        map.put("performanceClass", CSS_CLASS_SELECTED);
        map.put("performanceSummary", performanceService.getPerformanceSummary(performanceData));
        map.put("cagr", performanceData.get(performanceData.size()-1).getCagr());
        map.put("vamiChartData", performanceService.getVamiChartData(performanceData));
        // Reverse so records are ordered from newest to oldest
        // Do this last because the methods above require it from oldest to newest
        Collections.reverse(performanceData);
        map.put("performanceData", performanceData);
        map.put("accounts", accountService.getAccounts());
    	return map;
    }
    
    @Override
    protected boolean isFormChangeRequest(HttpServletRequest request, Object command) {
    	return request.getParameter("_update") == null;
    }
    
    @Override
    public ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

    	PerformanceCommand cmd = (PerformanceCommand) command;
    	
    	// Set date to last day of month
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(cmd.getDate());
    	cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
    	cal.set(Calendar.HOUR, 0);
    	cal.set(Calendar.MINUTE, 0);
    	cal.set(Calendar.SECOND, 0);
    	cal.set(Calendar.MILLISECOND, 0);
    	
    	accountService.updateAccountValue(cmd.getAccountId(), cal.getTime(), cmd.getNav(), cmd.getDeposits(), cmd.getWithdrawals());       	       	
    	
    	logger.info("Returning " + getSuccessView() + " view");
        return new ModelAndView(new RedirectView("performance?account=" + cmd.getAccountId()));
    }

}
