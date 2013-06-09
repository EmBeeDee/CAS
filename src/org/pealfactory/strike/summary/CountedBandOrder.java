package org.pealfactory.strike.summary;

import java.util.*;

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
public class CountedBandOrder extends BasicBandOrder implements Comparable<CountedBandOrder>
{
	private SortedSet<BandOrder> fMatchingResults = new TreeSet<BandOrder>();

	public CountedBandOrder(BandOrder order)
	{
		super(order.getOrder());
	}

	public void addOrder(BandOrder order)
	{
		if (!equals(order))
			throw new IllegalArgumentException("Attempted to add BandOrder \"+order.toString()+\", but does not match CountedBandOrder "+toString());
		fMatchingResults.add(order);
	}

	public int getPopularity()
	{
		return fMatchingResults.size();
	}

	public SortedSet<BandOrder> getAllMatchingResults()
	{
		return fMatchingResults;
	}

	@Override
	public int compareTo(CountedBandOrder cbo)
	{
		int ourPopularity = getPopularity();
		int otherPopularity = cbo.getPopularity();
		// If two popularities are the same, compare on band order.
		// Can't have two CountedBandOrders being equal, otherwise containing SortedSets will drop them!
		if (ourPopularity==otherPopularity)
			return getOrder().compareTo(cbo.getOrder());
		return new Integer(otherPopularity).compareTo(ourPopularity);
	}
}
