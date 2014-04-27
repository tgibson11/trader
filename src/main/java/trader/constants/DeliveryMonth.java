package trader.constants;

public enum DeliveryMonth {

	F("01"),
	G("02"),
	H("03"),
	J("04"),
	K("05"),
	M("06"),
	N("07"),
	Q("08"),
	U("09"),
	V("10"),
	X("11"),
	Z("12");
	
	private final String twoDigitMonth;
	
	DeliveryMonth(String twoDigitMonth) {
		this.twoDigitMonth = twoDigitMonth;
	}
	
	public String getTwoDigitMonth() {
		return twoDigitMonth;
	}

}
