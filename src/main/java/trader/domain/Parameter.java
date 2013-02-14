package trader.domain;

import java.io.Serializable;

public class Parameter implements Serializable {

	private static final long serialVersionUID = 1433574299124906692L;
	
	private String parameterCd;
	private String parameterValue;
	
	public void setParameterCd(String parameterCd) {
		this.parameterCd = parameterCd;
	}

	public String getParameterCd() {
		return parameterCd;
	}

	public void setParameterValue(String parameterValue) {
		this.parameterValue = parameterValue;
	}

	public String getParameterValue() {
		return parameterValue;
	}
	
}
