package trader.controller;

import static org.apache.commons.lang.StringUtils.isBlank;
import static trader.constants.Constants.CSS_CLASS_SELECTED;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import trader.domain.PerformanceData;
import trader.service.AccountService;
import trader.service.PerformanceService;

@Controller
public class PerformanceController {

	private static final String FORM_VIEW = "performance";
	
    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private PerformanceService performanceService;

    @InitBinder
    protected void initBinder(ServletRequestDataBinder binder) {
    	binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("MM/dd/yyyy"), false));
	}

    @RequestMapping(value="performance", method=RequestMethod.GET)
    protected String showForm(@RequestParam(value="accountId", required=false) String accountId, Model model) {
    	
    	if (isBlank(accountId)) {
    		accountId = accountService.getAccounts().get(0).getAccountId();
    	}
    	
        List<PerformanceData> performanceData = performanceService.getPerformanceData(accountId);
        model.addAttribute("performanceClass", CSS_CLASS_SELECTED);
        model.addAttribute("performanceSummary", performanceService.getPerformanceSummary(performanceData));
        if (!performanceData.isEmpty()) {
        	model.addAttribute("cagr", performanceData.get(performanceData.size()-1).getCagr());        	
        }
        model.addAttribute("vamiChartData", performanceService.getVamiChartData(performanceData));
        // Reverse so records are ordered from newest to oldest
        // Do this last because the methods above require it from oldest to newest
        Collections.reverse(performanceData);
        model.addAttribute("performanceData", performanceData);
        model.addAttribute("accounts", accountService.getAccounts());
        model.addAttribute("selectedAccountId", accountId);
    	return FORM_VIEW;
    }
    
    private boolean isFormChangeRequest(HttpServletRequest request) {
    	return request.getParameter("_update") == null;
    }
    
   @RequestMapping(value="performance", method=RequestMethod.POST)
    public String onSubmit(HttpServletRequest request,
    		@RequestParam(value="accountId") String accountId,
    		@RequestParam(value="date") Date date,
    		@RequestParam(value="nav") Double nav,
    		@RequestParam(value="deposits") Double deposits,
    		@RequestParam(value="withdrawals") Double withdrawals,
    		Model model) {
    	
    	if (isFormChangeRequest(request)) {
    		return showForm(accountId, model);
    	}

    	// Set date to last day of month
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(date);
    	cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
    	cal.set(Calendar.HOUR, 0);
    	cal.set(Calendar.MINUTE, 0);
    	cal.set(Calendar.SECOND, 0);
    	cal.set(Calendar.MILLISECOND, 0);
    	
    	accountService.updateAccountValue(accountId, cal.getTime(), nav, deposits, withdrawals);       	       	
    	
        return "redirect:performance?account=" + accountId;
    }

}
