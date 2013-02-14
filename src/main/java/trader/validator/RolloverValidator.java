package trader.validator;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import trader.command.RolloverCommand;
import trader.domain.ExtContract;
import trader.service.ContractService;

@Component
public class RolloverValidator implements Validator {

    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());
    
    @Resource
    private ContractService contractService;
    
    private int EXPIRY_LENGTH = 6;

	public boolean supports(Class<?> clazz) {
        return RolloverCommand.class.equals(clazz);
    }

    public void validate(Object obj, Errors errors) {
        RolloverCommand rollover = (RolloverCommand) obj;
        String symbol = rollover.getSymbol();
        String expiry = rollover.getExpiry();
        
        ExtContract contract = contractService.getContract(symbol);
        
        // Validate symbol
        if (symbol.trim().isEmpty()) {
            errors.rejectValue("symbol", "error.not-specified", null, "Value required");
        } else {
            logger.info("Validating with " + rollover + ": " + symbol);
            if (contract == null) {
            	errors.rejectValue("symbol", "error.invalid", null, "Invalid value");
            }
        }
        
        // Validate expiry
        if (expiry.trim().isEmpty()) {
        	errors.rejectValue("expiry", "error.not-specified", null, "Value required");
        } else {
            logger.info("Validating with " + rollover + ": " + expiry);
            if (expiry.length() != EXPIRY_LENGTH
            		|| Integer.parseInt(expiry.substring(4)) > 12
            		|| (contract != null && expiry.compareTo(contract.m_expiry) <= 0)) {
                errors.rejectValue("expiry", "error.invalid", null, "Invalid value");
            }
        }
    }   
}
