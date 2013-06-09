package org.pealfactory.strike.summary;

import java.util.*;

/**
 * Immutable, holds the band order and results
 *
 * <p>
 * CAS Copyright 2003-2012 Mark B Davies
 * </p>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * </p>
 *
 */
public class BandOrder extends BasicBandOrder implements Comparable<BandOrder>
{
	private final String fAnalyserName;
	private final SortedSet<BandResult> fOrderedResults;

	protected BandOrder(String analyserName, SortedSet<BandResult> order)
	{
		super(order);
		fAnalyserName = analyserName;
		fOrderedResults = new TreeSet<BandResult>();
		fOrderedResults.addAll(order);
	}

	protected BandOrder(BandOrder other)
	{
		super(other.fOrderedResults);
		fAnalyserName = other.fAnalyserName;
		fOrderedResults = other.fOrderedResults;
	}

	public BandResult getBand(char bandNumber)
	{
		for (BandResult band: fOrderedResults)
			if (band.getBandNumber().equals(bandNumber))
				return band;
		return null;
	}

	public String getAnalyserName()
	{
		return fAnalyserName;
	}

	public double getNormalisedResult(char bandNumber)
	{
		BandResult band = getBand(bandNumber);
		if (band==null)
			throw new IndexOutOfBoundsException("Failed to find band "+bandNumber+" in "+this);
		double minResult = getBestResult();
		double maxResult = getWorstResult();
		if (minResult==maxResult)
			return 0;
		return (band.getResult()-minResult)/(maxResult-minResult);
	}

	public double getBestResult()
	{
		return fOrderedResults.first().getResult();
	}

	public double getWorstResult()
	{
		return fOrderedResults.last().getResult();
	}

	@Override
	public int compareTo(BandOrder o)
	{
		return fAnalyserName.compareTo(o.getAnalyserName()); 
	}

	/**
	 * Len must be bigger than 1, and should ideally be much bigger than the total number of bands.
	 *
	 * @param len
	 * @return
	 */
	public String getBandSpacingAsString(int len, String spaceChar)
	{
		int[] bandPositions = new int[fOrderedResults.size()];
		char[] bandChars = new char[fOrderedResults.size()];
		double minResult = getBestResult();
		double maxResult = getWorstResult();
		double charPosPerScore = 0;
		if (maxResult>minResult)
			charPosPerScore = (len-1)/(maxResult-minResult);
		int i = 0;
		for (BandResult band: fOrderedResults)
		{
			bandPositions[i] = (int)((band.getResult()-minResult)*charPosPerScore+0.5);
			bandChars[i] = band.getBandNumber();
			i++;
		}
		StringBuilder buf = new StringBuilder(len);
		i = 0;
		for (int j=0; j<len; j++)
		{
			if (j>=bandPositions[i])
				buf.append(bandChars[i++]);
			else
				buf.append(spaceChar);
		}
		return buf.toString();
	}
}
