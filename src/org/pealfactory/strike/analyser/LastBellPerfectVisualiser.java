package org.pealfactory.strike.analyser;

import org.pealfactory.strike.data.Row;

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
 *
 * @author MBD
 */
public class LastBellPerfectVisualiser extends VisualiserHelper
{
	public final static String NAME = "Last Bell Perfect";
	public final static String INFO = "The Last Bell Perfect visualiser assumes that the last bell to strike in a row is in the correct place, and marks the end of the row. Other bells are marked with respect to it. Handstroke gap is a constant ";

	/** As a proportion of inter-bell gap, i.e. 1.0 is nominal. */
	private double fHandstrokeGap;


	public LastBellPerfectVisualiser(double handstrokeGap)
	{
		super(NAME, INFO+handstrokeGap+".");
		fHandstrokeGap = handstrokeGap;
	}

	protected void newRow(Row row)
	{
		// Row end time is always where last bell rang.
		int rowEndTime = row.getStrikeTime(row.getRowSize());
		addAveragedRow(row, rowEndTime, fHandstrokeGap);
	}

}
