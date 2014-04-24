package trader.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import trader.dao.ParameterDao;
import trader.domain.Parameter;

@Service
public class ParameterService implements ApplicationListener<ApplicationEvent>{
	
	@Resource
	private ParameterDao parameterDao;
	
	private Map<String, String> parameters;
	
	/**
	 * Called when the Spring application context is refreshed.
	 * Loads parameters from database.
	 * @param event
	 */
	public void onApplicationEvent(ApplicationEvent event) {
		 if (event.getClass().equals(ContextRefreshedEvent.class)) {
			 loadParameters();
		 }
	}
	
	public void loadParameters() {
		parameters = new HashMap<String, String>();
		for (Parameter parameter : parameterDao.getParameters()) {
			parameters.put(parameter.getParameterCd(), parameter.getParameterValue());
		}
	}
	
	public Double getDoubleParameter(String parameterCd) {
		return Double.parseDouble(parameters.get(parameterCd));
	}
	
	public Integer getIntParameter(String parameterCd) {
		return Integer.parseInt(parameters.get(parameterCd));
	}
	
	public String getStringParameter(String parameterCd) {
		return parameters.get(parameterCd);
	}

}
