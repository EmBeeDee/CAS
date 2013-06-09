package org.pealfactory.strike.analyser;

import org.pealfactory.strike.data.*;

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
public class SimpleLADVisualiser extends VisualiserHelper
{
	public final static String NAME = "Simple LAD Visualiser";
	public final static String INFO = "The Simple LAD Visualiser uses a Least Absolute Deviation regression line to estimate the correct strike times for each row. There can be discontinuities between rows.";

	private Row fPreviousRow;

	public SimpleLADVisualiser()
	{
		super(NAME, INFO);
	}

	protected void newRow(Row row)
	{
		int bestLad = Integer.MAX_VALUE;
		int bestGap = 0;
		int bestRowStart = row.getBong(1).time;

		int n = row.getRowSize();
		for (int i=0; i<n; i++)
			for (int j=i+1; j<n; j++)
			{
				Bong bell1 = row.getBong(i+1);
				Bong bell2  = row.getBong(j+1);
        int gap = (bell2.time-bell1.time)/(j-i);
				int rowstart = bell1.time - i*gap;
				int lad = 0;
				for (int k=0; k<n; k++)
					lad+= Math.abs(row.getBong(k+1).time - (rowstart+k*gap));
				if (lad<bestLad)
				{
					bestLad = lad;
					bestGap = gap;
					bestRowStart = rowstart;
				}
			}
		double handstrokeGap = 1.0;
		int endTime = bestRowStart+(n-1)*bestGap;
		int startTime = bestRowStart-bestGap;
		if (row.isHandstroke())
			startTime-= bestGap*handstrokeGap;
		addAveragedRow(row, endTime, handstrokeGap, endTime-startTime);
		fPreviousRow = row;
	}

}
