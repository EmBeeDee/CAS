package org.pealfactory.strike.summary;

import javax.swing.*;
import javax.swing.table.*;
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
public class SummaryTableModel extends AbstractTableModel implements Runnable
{
	private int fRows = 0;
	private Summariser fSummariser;
	private List<BandOrder> fBandOrders;

	public SummaryTableModel()
	{
	}

	@Override
	public String getColumnName(int column)
	{
		if (column==0)
			return "Analyser type";
		if (column==1)
			return "Band order";
		return "";
	}

	@Override
	public int getRowCount()
	{
		return fRows;
	}

	@Override
	public int getColumnCount()
	{
		return 2;
	}

	/**
	 * To be called on a worker thread.
	 * @param s
	 */
	public void newResults(Summariser s)
	{
		fSummariser = s;
		try
		{
			SwingUtilities.invokeAndWait(this);
		}
		catch (Exception e)
		{
			
		}
	}

	/**
	 * To be called on the event thread.
	 */
	@Override
	public void run()
	{
		fRows = 0;
		fBandOrders = new ArrayList<BandOrder>();
		for (BandOrder order: fSummariser.getBandOrders())
		{
			fBandOrders.add(order);
			fRows++;
		}
		BandOrder order = fSummariser.getAverageOrder();
		if (order!=null)
		{
			fBandOrders.add(order);
			fRows++;		
		}
		fireTableDataChanged();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		BandOrder bandOrder = fBandOrders.get(rowIndex);
		if (columnIndex==0)
			return bandOrder.getAnalyserName();
		else if (columnIndex==1)
			return bandOrder.getOrder();
		return ""; 
	}

}
