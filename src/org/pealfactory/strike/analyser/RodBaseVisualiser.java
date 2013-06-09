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
abstract class RodBaseVisualiser extends VisualiserHelper
{
	private int fNWholePulls;
	private Row[] fRows;

	private int fFillPoint;
	private int fEmptyPoint;
	private double fCurrentInterBellGap;

	protected RodBaseVisualiser(String name, String info)
	{
		super(name, info);
		fNWholePulls = 3;
		fRows =  new Row[fNWholePulls*2];
	}

	public void clearData()
	{
		super.clearData();
		fFillPoint = 0;
		fEmptyPoint = 0;
		fCurrentInterBellGap = 0.0;
	}

	protected abstract double getCurrentHandstrokeGap();

	protected void newRow(Row row)
	{
		fRows[fFillPoint++] = row;
		if (fFillPoint==fRows.length)
		{
			// Have got enough rows to make a full set of whole pulls - calculate.
			double avWholePullLength = (calcWholePullMidpoint(fRows.length-2)-calcWholePullMidpoint(0))/(fRows.length/2-1);
      fCurrentInterBellGap = avWholePullLength/(getCurrentHandstrokeGap()+2*getNBells());
			while (fEmptyPoint<=(fRows.length/4)*2)
			{
				addWholePullRows(fCurrentInterBellGap);
				fEmptyPoint+= 2;
			}
			fEmptyPoint-= 2;
			System.arraycopy(fRows, 2, fRows, 0, fRows.length-2);
			fFillPoint-= 2;
		}
	}

	public void notifyLastRowRung()
	{
		while (fEmptyPoint<fFillPoint)
		{
			addWholePullRows(fCurrentInterBellGap);
			fEmptyPoint+= 2;
		}
		super.notifyLastRowRung();
	}

	private void addWholePullRows(double interbellGap)
	{
		int nbells = getNBells();
		// End of handstroke is midpoint of strike times of all bells in the whole pull minus half a gap.
		int rowEndTime = (int)(calcWholePullMidpoint(fEmptyPoint)-0.5*interbellGap);
		int duration = (int)(interbellGap*fRows[fEmptyPoint].getNBells());
		if (fRows[fEmptyPoint].isHandstroke())
			duration+= getCurrentHandstrokeGap()*interbellGap;
		addAveragedRow(fRows[fEmptyPoint], rowEndTime, getCurrentHandstrokeGap(), duration);
		// End of backstroke is end of handstroke plus nbells times interbell gap.
		rowEndTime+= nbells*interbellGap;
		duration = (int)(interbellGap*fRows[fEmptyPoint+1].getNBells());
		if (fRows[fEmptyPoint+1].isHandstroke())
			duration+= getCurrentHandstrokeGap()*interbellGap;
		addAveragedRow(fRows[fEmptyPoint+1], rowEndTime, getCurrentHandstrokeGap(), duration);
	}

	private double calcWholePullMidpoint(int row)
	{
		int c = 0;
		double ms = 0;
		for (int i=0; i<fRows[row].getRowSize(); i++)
		{
			ms+= fRows[row].getBong(i+1).time;
			c++;
		}
		for (int i=0; i<fRows[row+1].getRowSize(); i++)
		{
			ms+= fRows[row+1].getBong(i+1).time;
			c++;
		}
		return (ms/c);
	}

}
