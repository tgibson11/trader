package trader.controller;

import static trader.constants.Constants.CSS_CLASS_SELECTED;
import static trader.constants.Constants.HOME_VIEW;
import static trader.constants.Constants.ROLLOVER_VIEW;

import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import trader.command.RolloverCommand;
import trader.service.ContractService;
import trader.service.TradeService;
import trader.validator.RolloverValidator;

@Controller
public class RolloverController {
	
    protected final Log logger = LogFactory.getLog(getClass());
    
    @Resource
    private ContractService contractService;
    
    @Resource
    private TradeService tradeService;
   
	@Resource
	private RolloverValidator validator;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
	}
	
	@ModelAttribute("command")
    public RolloverCommand formBackingObject() {
        return new RolloverCommand();
    }
    
	@RequestMapping(value = "rollover", method = RequestMethod.GET)
    public String showForm(Map<String, Object> model) throws Exception {
		model.put("rolloverClass", CSS_CLASS_SELECTED);
		model.put("contracts", contractService.getContracts());
		model.put("expiries", contractService.getExpiries());
		model.put("rollovers", contractService.getRollovers());
    	return ROLLOVER_VIEW;
    }
    
	@RequestMapping(value = "rollover", method = RequestMethod.POST)
    public String onSubmit(@ModelAttribute("command") @Validated RolloverCommand command) throws Exception {

    	String symbol = command.getSymbol();
    	String expiry = command.getExpiry();
   	   	
        logger.info("Rolling over " + symbol + " to " + expiry);
		tradeService.rollover(symbol, expiry);

        logger.info("returning from Rollover view to " + HOME_VIEW);
        return "redirect:" + HOME_VIEW;
    }

}
