package trader.utils;

public class MathUtils {
	
	private final static double EPSILON = 1E-6;

	/**
	 * Returns true if the difference between two doubles is less than EPSILON.
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean equals(double a, double b){
	    return a == b ? true : Math.abs(a - b) < EPSILON;
	}
	
	/**
	 * Rounds d to the nearest whole-number multiple of increment
	 * @param d
	 * @param increment
	 * @return
	 */
	public static double round(double d, double increment) {
		return Math.round(d / increment) * increment;
	}	
}
