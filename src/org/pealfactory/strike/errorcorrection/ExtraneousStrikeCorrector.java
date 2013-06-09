package org.pealfactory.strike.errorcorrection;

import org.pealfactory.strike.*;
import org.pealfactory.strike.data.*;

/**
 * Does a similar job to the SensorEchoCorrector, i.e. attempts to weed out extra strikes. However it does
 * this in a more sophisticated way, checking for bells which appear to have rung twice in a row, and weeding
 * out the strike note which seems further away from the average row gap.
 * It is worth using the SensorEchoCorrector first to get rid of eachoes, then the ExtraneousStrikeCorrector to mop up
 * any gross mid-change extra strikes.
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
 * @author Mark
 */
public class ExtraneousStrikeCorrector  extends ErrorCorrectionHelper
{
	private int fNBells = 0;
	private RawRow fCurrentRow;
	private RawRow fNextRow;

	public ExtraneousStrikeCorrector()
	{
		fCurrentRow = new RawRow(true);
		fNextRow = new RawRow(false);
	}

	public void receiveBong(Bong bong)
	{
		int place1 = fCurrentRow.findBell(bong.bell);
		int place2 = fNextRow.findBell(bong.bell);
		// Fill first row
		if (fNextRow.getRowSize()==0 && place1<0)
		{
			fCurrentRow.addBong(bong);
			return;
		}
		// Fill subsequent rows
		if (place2<0)
		{
			fNextRow.addBong(bong);
			return;
		}
		fNBells = Math.max(fNBells, fCurrentRow.getRowSize());
		// No room in next row - consider whether this is because we have had two strikes for this bell
		int nBellsInBothRows = 0;
		int totalSep = 0;
		int mySep1 = 0;
		int mySep2 = 0;
		for (Bong b2: fNextRow)
		{
			int p1 = fCurrentRow.findBell(b2.bell);
			if (p1>0)
			{
				Bong b1 = fCurrentRow.getBong(p1);
				if (b2.bell==bong.bell)
				{
					mySep1 = b2.time - b1.time;
					mySep2 = bong.time - b2.time;
				}
				else
				{
					totalSep+= b2.time - b1.time;
					nBellsInBothRows++;
				}
			}
		}
		// In order to drop a strike, we have to reassure ourselves that, of the three strikes we are looking at,
		// at least one of the inter-strike gaps is less than half the normal interval, and in addition that the
		// total interval including all three strikes is also substantially more than a normal interval.
		int avSep = 0;
		if (nBellsInBothRows>0)
			avSep = totalSep/nBellsInBothRows;
		int myTotalSep = mySep1+mySep2;
		if (myTotalSep>0 && myTotalSep<avSep*1.2)
		{
			if (mySep1<mySep2)
			{
				// Drop middle strike
				fNextRow.removeBong(fNextRow.findBell(bong.bell));
				fNextRow.addBong(bong);
				return;
			}
			else if (mySep2<avSep/2)
			{
				// Drop this strike
				return;
			}
		}
		// Start new row after all
		finishRow();
		fNextRow.addBong(bong);
	}

	protected void finishRow()
	{
		for (Bong bong: fCurrentRow)
			fNextStage.receiveBong(bong);
		fCurrentRow = fNextRow;
		fNextRow = new RawRow(!fCurrentRow.isHandstroke());
	}


	public void notifyInputComplete()
	{
		finishRow();
		finishRow();
		fNextStage.notifyInputComplete();
	}
}
