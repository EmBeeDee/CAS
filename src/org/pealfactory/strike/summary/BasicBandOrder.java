package org.pealfactory.strike.summary;

import java.util.*;

/**
 * Immutable, just holds the band order string.
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
public class BasicBandOrder
{
	private final String fOrderString;

	public BasicBandOrder(String orderString)
	{
		fOrderString = orderString;
	}

	public BasicBandOrder(SortedSet<BandResult> orderedResults)
	{
		fOrderString = buildOrderString(orderedResults);
	}

	private String buildOrderString(SortedSet<BandResult> orderedResults)
	{
		StringBuffer buf = new StringBuffer();
		for (BandResult r: orderedResults)
		{
			buf.append(r.getBandNumber());
			buf.append(" ");
		}
		return buf.toString();
	}

	public String getOrder()
	{
		return fOrderString;
	}

	@Override
	public String toString()
	{
		return getOrder();
	}

	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof BasicBandOrder)
		{
			BasicBandOrder bo = (BasicBandOrder)o;
			return getOrder().equals(bo.getOrder());
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return getOrder().hashCode();
	}

	/**
	 * Returns the swapping distance between the two band orders, i.e. the minimum number of swaps of
	 * adjacent pairs which will turn one order into the other.
	 *
	 * @param o
	 * @return
	 */
	public int distance(BasicBandOrder o)
	{
		if (o.equals(this))
			return 0;
		StringBuilder buf = new StringBuilder(o.getOrder());
		int c = 0;
		for (int i=0; i<fOrderString.length(); i++)
			c+= swapCharUpToPos(buf, fOrderString.charAt(i), i);
		return c;
	}

	private int swapCharUpToPos(StringBuilder buf, char c, int pos)
	{
		if (c==buf.charAt(pos))
			return 0;
		for (int j=pos+1; j<buf.length(); j++)
		{
			if (c==buf.charAt(j))
			{
				buf.setCharAt(j, buf.charAt(pos));
				buf.setCharAt(pos, c);
				return j-pos;
			}
		}
		// Second order doesn't contain a band character - oops!
		// Insert it to sort out lengths and positions properly, and return a very big distance.
		buf.insert(pos, c);
		return 1000;
	}

}
