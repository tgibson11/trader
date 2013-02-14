package trader.domain;


import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;

public class ExtContract extends Contract implements Comparable<ExtContract> {
    
	private String description;
	private Integer multiplier;
	private Double tickSize;
	private Boolean pricedInCents;
	private String openInterestUrl;
	private Integer volume;
	private Double atr;
	private Double entryHigh;
	private Double entryLow;
	private Double exitHigh;
	private Double exitLow;
	private boolean openPosition;
	private List<HistoricalData> historicalData;
	
	public ExtContract() {
		super();
		this.historicalData = new ArrayList<HistoricalData>();
	}
	
	public void setSymbol(String symbol) {
		this.m_symbol = symbol;
	}
	
	public String getSymbol() {
		return this.m_symbol;
	}
	
	public void setExpiry(String expiry) {
		this.m_expiry = expiry;
	}
	
	public String getExpiry() {
		return this.m_expiry;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setMultiplier(Integer multiplier) {
		this.multiplier = multiplier;
	}

	public Integer getMultiplier() {
		return multiplier;
	}

	public void setTickSize(Double tickSize) {
		this.tickSize = tickSize;
	}

	public Double getTickSize() {
		return tickSize;
	}

	public void setPricedInCents(Boolean pricedInCents) {
		this.pricedInCents = pricedInCents;
	}

	public Boolean getPricedInCents() {
		return pricedInCents;
	}

	public String getOpenInterestUrl() {
		return openInterestUrl;
	}

	public void setOpenInterestUrl(String openInterestUrl) {
		this.openInterestUrl = openInterestUrl;
	}

	public Integer getVolume() {
		return volume;
	}

	public void setVolume(Integer volume) {
		this.volume = volume;
	}

	public void setAtr(Double atr) {
		this.atr = atr;
	}

	public Double getAtr() {
		return atr;
	}

	public void setEntryHigh(Double entryHigh) {
		this.entryHigh = entryHigh;
	}

	public Double getEntryHigh() {
		return entryHigh;
	}

	public void setEntryLow(Double entryLow) {
		this.entryLow = entryLow;
	}

	public Double getEntryLow() {
		return entryLow;
	}

	public void setExitHigh(Double exitHigh) {
		this.exitHigh = exitHigh;
	}

	public Double getExitHigh() {
		return exitHigh;
	}

	public void setExitLow(Double exitLow) {
		this.exitLow = exitLow;
	}

	public Double getExitLow() {
		return exitLow;
	}

	public boolean isOpenPosition() {
		return openPosition;
	}

	public void setOpenPosition(boolean openPosition) {
		this.openPosition = openPosition;
	}

	public void setHistoricalData(List<HistoricalData> historicalData) {
		this.historicalData = historicalData;
	}

	public List<HistoricalData> getHistoricalData() {
		return historicalData;
	}
	
	public String toString() {
    	return m_symbol;
    }

	@Override
	public int compareTo(ExtContract o) {
		if (this.m_symbol.compareTo(o.m_symbol) == 0) {
			return this.m_expiry.compareTo(o.m_expiry);
		} else {
			return this.m_symbol.compareTo(o.m_symbol);
		}
	}

}
