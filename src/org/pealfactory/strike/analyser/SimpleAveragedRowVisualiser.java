package org.pealfactory.strike.analyser;

import org.pealfactory.strike.data.Row;
import org.pealfactory.strike.data.AveragedRow;

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
public class SimpleAveragedRowVisualiser extends VisualiserHelper
{
	public final static String NAME = "Simple Averager";
	public final static String INFO = "The Simple Averager visualiser calculates the desired length of the current row by averaging the actual length of the row with the judged length of previous rows. Handstroke gap is a constant ";

	/** As a proportion of inter-bell gap, i.e. 1.0 is nominal. */
	private double fHandstrokeGap;

	private int fRowEndTime;
	private int fAvHandDuration;
	private int fAvBackDuration;


	public SimpleAveragedRowVisualiser(double handstrokeGap)
	{
		super(NAME, INFO+handstrokeGap+".");
		fHandstrokeGap = handstrokeGap;
	}

	public void clearData()
	{
		super.clearData();
		fRowEndTime = -1;
		fAvHandDuration = -1;
		fAvBackDuration = -1;
	}

	protected void newRow(Row row)
	{
		int averageGap = (row.getStrikeTime(row.getRowSize())-row.getStrikeTime(1))/(row.getNBells()-1);
		// Special case for first row - the end of the "last" row is really the start
		// of this row, but we have to guess when that was. Remember the first row
		// is a handstroke so the previous row end is two bell gaps before the treble.
		if (fRowEndTime<0)
			fRowEndTime = row.getStrikeTime(1) - averageGap*2;

		int rowEndTime = row.getStrikeTime(row.getRowSize());
		int duration = rowEndTime - fRowEndTime;
		int avDur = row.isHandstroke() ? fAvHandDuration : fAvBackDuration;
		if (avDur<=0)
			avDur = duration;
		duration = (avDur+duration)/2;
		avDur = duration;
		rowEndTime = fRowEndTime+duration;

		// Calculate stats for this row, then reset.
		addAveragedRow(row, rowEndTime, fHandstrokeGap, duration);

		fRowEndTime = rowEndTime;
		if (row.isHandstroke())
			fAvHandDuration = avDur;
		else
			fAvBackDuration = avDur;
	}
}
