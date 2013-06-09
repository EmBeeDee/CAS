package org.pealfactory.strike.pipeline;

import org.pealfactory.strike.Constants;
import org.pealfactory.strike.input.BongListener;
import org.pealfactory.strike.data.*;
import org.pealfactory.strike.pipeline.Pipeline;

import java.util.ArrayList;

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
public class RowAccumulator implements BongListener
{
	private static final boolean LOG_OUTPUT = false;

	private Pipeline fPipeline;
	private ArrayList fData;
	private RawRow fCurrentRow;
	private RawRow fNextRow;
	private int fNBells = 0;

	public RowAccumulator(Pipeline pipeline)
	{
		fPipeline = pipeline;
		fData = new ArrayList();
		fCurrentRow = new RawRow(true);
		fNextRow = new RawRow(false);
	}

	/**
	 * Assume error correction has sorted out most problems with input - Bongs in time-sorted order,
	 * no sensor echoes, handstroke/backstroke flags correct, or marked as unknown.
	 * Still have to deal with missing blows, and bells sounding their first stroke of the next row
	 * before the current row is complete.
	 *
	 * @param bong
	 */
  public void receiveBong(Bong bong)
	{
		// fNextRow will be empty when the very first row is received; fill up fCurrentRow first
		if (fNextRow.getRowSize()==0)
		{
			if (fCurrentRow.isMatchingStroke(bong))
			{
				if (fCurrentRow.findBell(bong.bell)>0)
					System.out.println("WARNING: bell "+bong.bell+" sounded twice in row 1; ignoring second strike.");
				else
					fCurrentRow.addBong(bong);
				return;
			}
		}
		// See if we fit in fNextRow
		if (fNextRow.isMatchingStroke(bong))
		{
			if (fNextRow.findBell(bong.bell)>0)
				System.out.println("WARNING: bell "+bong.bell+" sounded twice in row "+fData.size()+2+"; ignoring second strike.");
			else
				fNextRow.addBong(bong);
			return;
		}
		// Nope - finish the row and add to the next
		finishRow();
		fNextRow.addBong(bong);
	}

	protected void finishRow()
	{
		fNBells = Math.max(fNBells, fCurrentRow.getNBells());
		if (LOG_OUTPUT)
			System.out.println("RowAccumulator: "+fCurrentRow.rowAsString());
		fData.add(fCurrentRow);
		fCurrentRow = fNextRow;
		fNextRow = new RawRow(!fCurrentRow.isHandstroke());
		fPipeline.rowsAvailable(fData.size());
	}

	protected boolean isSameStroke(Bong b1, Bong b2)
	{
		if (b1.stroke==Bong.UNKNOWNSTROKE || b2.stroke==Bong.UNKNOWNSTROKE)
			return true;
		return b1.stroke==b2.stroke;
	}

	protected boolean isSameStroke(Bong b, Row r)
	{
		if (b.stroke==Bong.UNKNOWNSTROKE)
			return true;
		if (b.stroke==Bong.HANDSTROKE)
			return r.isHandstroke();
		return !r.isHandstroke();
	}

	public void notifyInputComplete()
	{
		fData.add(fCurrentRow);
		// Don't add final row if it's a handstroke - stats and rendering can only cope with whole pulls!
		if (!fNextRow.isHandstroke())
			fData.add(fNextRow);
		fPipeline.rowsAvailable(fData.size());
		fPipeline.notifyLastRowRung();
	}

	public int getNBells()
	{
		return fNBells;
	}

	public int size()
	{
		return fData.size();
	}

	public Row getRow(int i)
	{
		return (Row)fData.get(i);
	}
}
