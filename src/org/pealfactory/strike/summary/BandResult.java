package org.pealfactory.strike.summary;

/**
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
 */
public class BandResult implements Comparable<BandResult>
{
	private Character fBandNumber;
	/** Smaller is always better! */
	private double fResult;

	public BandResult(Character bandNumber, double result)
	{
		fBandNumber = bandNumber;
		fResult = result;
	}

	public Character getBandNumber()
	{
		return fBandNumber;
	}

	public void setBandNumber(Character bandNumber)
	{
		fBandNumber = bandNumber;
	}

	public double getResult()
	{
		return fResult;
	}

	public void setResult(double result)
	{
		fResult = result;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof BandResult)
		{
			BandResult br = (BandResult)o;
			return compareTo(br)==0;
		}
		return false;
	}

	@Override
	public int compareTo(BandResult o)
	{
		if (getResult()<o.getResult())
			return -1;
		if (getResult()>o.getResult())
			return +1;
		// If results are equal, compare on band number - never want two different bands to be the same
		// (messes up SortedSets for one thing!)
		return getBandNumber().compareTo(o.getBandNumber());
	}
}
