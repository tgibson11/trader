package trader.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import trader.dao.PerformanceDao;
import trader.domain.PerformanceData;
import trader.domain.PerformanceSummary;

@Service
public class PerformanceService {
	
    private static final double INITIAL_VAMI = 1000;

    protected final Log logger = LogFactory.getLog(getClass());

    @Resource
    private PerformanceDao performanceDao;
	
	public List<PerformanceSummary> getPerformanceSummary(List<PerformanceData> performanceData) {
		List<PerformanceSummary> performanceSummary = new ArrayList<PerformanceSummary>();
		PerformanceSummary summary = null;
		int year = 0;
		for (PerformanceData data : performanceData) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(data.getDate());
			if (cal.get(Calendar.YEAR) != year) {
				year = cal.get(Calendar.YEAR);
				summary = new PerformanceSummary();
				summary.setYear(year);
				performanceSummary.add(summary);
			}
			switch (cal.get(Calendar.MONTH)) {
			case Calendar.JANUARY:
				summary.setJanRor(data.getRor());
				break;
			case Calendar.FEBRUARY:
				summary.setFebRor(data.getRor());
				break;
			case Calendar.MARCH:
				summary.setMarRor(data.getRor());
				break;
			case Calendar.APRIL:
				summary.setAprRor(data.getRor());
				break;
			case Calendar.MAY:
				summary.setMayRor(data.getRor());
				break;
			case Calendar.JUNE:
				summary.setJunRor(data.getRor());
				break;
			case Calendar.JULY:
				summary.setJulRor(data.getRor());
				break;
			case Calendar.AUGUST:
				summary.setAugRor(data.getRor());
				break;
			case Calendar.SEPTEMBER:
				summary.setSepRor(data.getRor());
				break;
			case Calendar.OCTOBER:
				summary.setOctRor(data.getRor());
				break;
			case Calendar.NOVEMBER:
				summary.setNovRor(data.getRor());
				break;
			case Calendar.DECEMBER:
				summary.setDecRor(data.getRor());
				break;
			}			
		}
		for (PerformanceSummary summ : performanceSummary) {
			summ.setYearRor(
					(1 + summ.getJanRor())*
					(1 + summ.getFebRor())*
					(1 + summ.getMarRor())*
					(1 + summ.getAprRor())*
					(1 + summ.getMayRor())*
					(1 + summ.getJunRor())*
					(1 + summ.getJulRor())*
					(1 + summ.getAugRor())*
					(1 + summ.getSepRor())*
					(1 + summ.getOctRor())*
					(1 + summ.getNovRor())*
					(1 + summ.getDecRor()) - 1);
		}
		return performanceSummary;
	}

	public List<PerformanceData> getPerformanceData(String accountId) {
		List<PerformanceData> performanceData = performanceDao.getPerformanceData(accountId);	
        doPerformanceCalcs(performanceData);
        return performanceData;
	}
	
	public String getVamiChartData(List<PerformanceData> performanceData) {
		String vamiChartData = "[";
		for (PerformanceData data : performanceData) {
			vamiChartData += data.getVami() + ",";
		}
		vamiChartData += "]";
		return vamiChartData;
	}
	
	private void doPerformanceCalcs(List<PerformanceData> performanceData) {
		double vami = INITIAL_VAMI;
		double peakVami = vami;
		double i = 0;
		for (PerformanceData data : performanceData) {
			i++;
			
			data.setRor(data.getPerformance() / data.getBnav());
			
			vami = (1 + data.getRor()) * vami;
			data.setVami(vami);
			
			peakVami = vami > peakVami ? vami : peakVami;
			data.setPeakVami(peakVami);
	
			data.setDrawdown((peakVami - vami) / peakVami);		
			data.setCagr(Math.pow(1 + (vami - INITIAL_VAMI) / INITIAL_VAMI, 12 / i) - 1);
			data.setExpectedAnnualPerformance(data.getEnav() * data.getCagr());
		}
	}	
}
