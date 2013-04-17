package trader.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Constants {
	
    public static final Map<String, String> FUTURES_MONTHS;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put("F", "01");
		map.put("G", "02");
		map.put("H", "03");
		map.put("J", "04");
		map.put("K", "05");
		map.put("M", "06");
		map.put("N", "07");
		map.put("Q", "08");
		map.put("U", "09");
		map.put("V", "10");
		map.put("X", "11");
		map.put("Z", "12");
		FUTURES_MONTHS = Collections.unmodifiableMap(map);
	}
	
	// Contract properties
	public static final String		CONTRACT_SEC_TYPE_FUTURE	= "FUT";
	public static final String		CONTRACT_CURRENCY_USD		= "USD";
	
	// Order properties
	public static final String		ORDER_ACTION_BUY			= "BUY";
	public static final String		ORDER_ACTION_SELL			= "SELL";
	public static final String		ORDER_TYPE_STOP				= "STP";
	public static final String		ORDER_TYPE_MKT				= "MKT";
	public static final String		ORDER_TIF_GTC				= "GTC";
	
	// Order statuses
	public static final String 		ORDER_STATUS_CANCELLED		= "Cancelled";
	public static final String		ORDER_STATUS_FILLED			= "Filled";
	
	// Parameter codes
	public static final String		PARAMETER_CD_TWS_PORT		= "TWS_PORT";
	
    public static final int LONG = 1;
	public static final int SHORT = -1;
	
	public static final String HIGH_CORRELATION = "HIGH";
	public static final String LOW_CORRELATION = "LOW";
	
	public static final String CSS_CLASS_SELECTED = "selected";
	
}
