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
	
}
