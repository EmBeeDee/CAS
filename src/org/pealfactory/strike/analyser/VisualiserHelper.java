package org.pealfactory.strike.analyser;

import org.pealfactory.strike.data.*;
import org.pealfactory.strike.pipeline.*;

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
public abstract class VisualiserHelper implements Visualiser
{
	private AnalysisStageListener fListener;
	private String fName;
	private String fInfo;
	private AveragedRowData fRows;
	private int fNRowsProcessed;
	private int fNBells;

	public VisualiserHelper(String name, String info)
	{
		fName = name;
		fInfo = info;
		clearData();
	}

	public void clearData()
	{
		fRows = new AveragedRowData();
		fNRowsProcessed = 0;
	}

	public void setAnalysisListener(AnalysisStageListener listener)
	{
		fListener = listener;
	}

	protected int getNBells()
	{
		return fNBells;
	}

	/**
	 * Responsive to thread interrupts.
	 *
	 * @param rowSource
	 */
	public void newRowsAvailable(RowSource rowSource)
	{
		fNBells = rowSource.getNBells();
		while (fNRowsProcessed<rowSource.getNRows())
		{
			if (Thread.currentThread().isInterrupted())
				return;
			newRow(rowSource.getRow(fNRowsProcessed++));
		}
	}

	public void notifyLastRowRung()
	{
		fListener.analysisComplete();
	}

	protected abstract void newRow(Row row);

	/**
	 * Returns a snapshot of the data so far, with statistical operators.
	 *
	 * @return
	 */
	public TouchStats getAveragedTouchData()
	{
		return new TouchStats(fRows, fRows.getNBells());
	}

	protected void addAveragedRow(Row row, int endTime, double handstrokeGap)
	{
		fRows.addRow(row, endTime, handstrokeGap);
		fListener.newAveragedRowAvailable();
	}

	protected void addAveragedRow(Row row, int endTime, double handstrokeGap, int duration)
	{
		fRows.addRow(row, endTime, handstrokeGap, duration);
		fListener.newAveragedRowAvailable();
	}

	public String getName()
	{
		return fName;
	}

	public String getInfo()
	{
		if (fInfo==null || fInfo.trim().length()==0)
			return fName;
		return fInfo;
	}

	public int getNRows()
	{
		return fRows.getNRows();
	}

	public AveragedRow getRow(int i)
	{
		return fRows.getRow(i);
	}

	@Override
	public int hashCode()
	{
		return fName.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Visualiser)
			return ((Visualiser) obj).getName().equals(fName);
		return false;
	}
}
