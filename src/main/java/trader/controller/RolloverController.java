package trader.controller;

import static trader.constants.Constants.CSS_CLASS_SELECTED;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
	
	private static final String FORM_VIEW = "rollover";
	private static final String SUCCESS_VIEW = "redirect:home";
	
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
    public String showForm(Model model) {
		model.addAttribute("rolloverClass", CSS_CLASS_SELECTED);
		model.addAttribute("contracts", contractService.getContracts());
		model.addAttribute("expiries", contractService.getExpiries());
		model.addAttribute("rollovers", contractService.getRollovers());
    	return FORM_VIEW;
    }
    
	@RequestMapping(value = "rollover", method = RequestMethod.POST)
    public String onSubmit(@ModelAttribute("command") @Validated RolloverCommand command, BindingResult bindingResult, Model model) throws Exception {

		if (bindingResult.hasErrors()) {
			return showForm(model);
		}
		
    	String symbol = command.getSymbol();
    	String expiry = command.getExpiry();
   	   	
        logger.info("Rolling over " + symbol + " to " + expiry);
		tradeService.rollover(symbol, expiry);

        return SUCCESS_VIEW;
    }

}
