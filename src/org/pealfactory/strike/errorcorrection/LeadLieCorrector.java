package org.pealfactory.strike.errorcorrection;

import org.pealfactory.strike.*;
import org.pealfactory.strike.data.*;

import java.util.*;

import static org.pealfactory.strike.Constants.*;

/**
 * This error corrector is intended to sit as the final error-correcting phase for bong input streams which need
 * handstroke/backstrokes to be inferred. It is important that the StrokeCorrector has already run, in order to
 * assign hand/back flags based on the initial flow of rows. What we do is correct any anomalies in earlier
 * error correction phases, caused for instance by missing sensor data, which have resulted in a bell which should
 * have been at lead being put at the back of the previous change; or vice versa.
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
public class LeadLieCorrector extends ErrorCorrectionHelper
{
	private static final boolean LOG_OUTPUT = false;

	private int fNBells = 0;
	private int[] fLastKnownGoodPositions;
	private RawRow fCurrentRow;
	private RawRow fNextRow;
	private int fRowsProcessed = 0;

	public LeadLieCorrector()
	{
		fLastKnownGoodPositions = new int[MAXNBELLS];
	}

	public void receiveBong(Bong bong)
	{
		// For the very first row we accept, there should be space in fCurrentRow (but no bells in fNextRow)
		if (fRowsProcessed==0)
		{
			if (fCurrentRow==null)
			{
				fCurrentRow = new RawRow(bong.stroke==Bong.HANDSTROKE);
				fNextRow = new RawRow(bong.stroke!=Bong.HANDSTROKE);
			}
			int place1 = fCurrentRow.findBell(bong.bell);
			if (!fCurrentRow.isMatchingStroke(bong))
			{
				fNextRow.addBong(bong);
				fRowsProcessed++;
			}
			else if (place1<0)
			{
				fCurrentRow.addBong(bong);
			}
			else
			{
				System.out.println("WARNING: bell "+bong.bell+" sounded twice in row "+fRowsProcessed+1+"; ignoring second strike.");
			}
			return;
		}

		fNBells = Math.max(fNBells, fCurrentRow.getNBells());
		// If we are the same stroke as before, continue to fill up fNextRow
		if (fNextRow.isMatchingStroke(bong))
		{
			int place2 = fNextRow.findBell(bong.bell);
			if (place2<0)
			{
				fNextRow.addBong(bong);
			}
			else
			{
				// Hmm, we must have come across a bell which has been shunted up by some earlier lie/lead swaps.
				// No choice but to swap its stroke and start a new row for it.
				finishRow();
				fNextRow.addBong(bong.swapStroke());
			}
			return;
		}

		// Otherwise, both rows are full; now is the time to decide if any shuffling needs to go on between the end of 
		// fCurrentRow and the start of fNextRow.
		if (fRowsProcessed<2)
		{
			// For the first whole pull, we have no information about previous positions of bells, but we can try and look
			// at the first handstroke gap, and we can see if the bells look like they ought to be in rounds.
			// But first, we make a simple check to see if any bells can be moved off the end of the first row onto the 
			// start of the second row, without losing anything off the end of the second; in this case, it's likely that
			// we have started "listening" halfway through a row. Note this generally won't be able to sort the whole
			// problem, since at least one of the misplaced bells is likely to be hanging on the end of the second row,
			// blocking our ability to move it off the end of the first.
			while (fCurrentRow.getRowSize()>1)
			{
				Bong firstRowLie = fCurrentRow.getLastBong();
				if (fNextRow.findBell(firstRowLie.bell)>=0)
					// Nope, can't fit bell onto the second row - exit loop
					break;
				// Yes! Bell fits on start of next row. Swap stroke and put it there.
				fCurrentRow.removeLastBong();
				fNextRow.addBongAtLead(firstRowLie.swapStroke());
			}
			// See if the first row is the end plus the start of rounds - would be a clue that we have started halfway
			// through a rounds row.
			int split = getCyclicSplit(fCurrentRow);
			if (split>0)
			{
				// If the second row is the same, it's a cert
				if (split==getCyclicSplit(fNextRow))
				{
					// However, final check to make sure none of the bells we are about to move off the end of the
					// second row match the new incoming bong - can't add them both to the third row!
					for (int i=split; i<fNextRow.getRowSize(); i++)
					{
						if (fNextRow.getBellAt(i+1)==bong.bell)
						{
							// Bad - abandon efforts
							finishRow();
							fNextRow.addBong(bong);
							return;
						}
					}
					// Move the bells up.
					RawRow extra = new RawRow(!fNextRow.isHandstroke());
					for (int i=split; i<fNextRow.getRowSize(); i++)
					{
						extra.addBongAtLead(fNextRow.removeLastBong().swapStroke());
						fNextRow.addBongAtLead(fCurrentRow.removeLastBong().swapStroke());
					}
					finishRow();
					fNextRow = extra;
					// Still have to deal with the latest incoming bong!
					fNextRow.addBong(bong);
					return;
				}
			}
		}
		else
		{
			// Once we're up and running, we try and use previous information about the bells at lead and lie to
			// determine what to do with them. For instance, if a bell appears at the end of a row, whereas previously
			// it was at the start, this is a clue that it needs shunting to the start of the next change.
			if (fCurrentRow.getRowSize()==fCurrentRow.getNBells())
			{
				Bong firstRowLie = fCurrentRow.getLastBong();
				if (bong.bell!=firstRowLie.bell && fLastKnownGoodPositions[firstRowLie.bell-1]<fNBells/3)
				{
					int placeInNext = fNextRow.findBell(firstRowLie.bell);
					if (placeInNext==fNextRow.getRowSize())
					{
						// Bong can be moved, but we have to shunt the same bell off the end of the next row, too.
						fCurrentRow.removeLastBong();
						Bong nextRowLie = fNextRow.removeLastBong();
						fNextRow.addBongAtLead(firstRowLie.swapStroke());
						finishRow();
						fNextRow.addBong(nextRowLie.swapStroke());
						fNextRow.addBong(bong);
						return;
					}
					if (placeInNext<0)
					{
						// Bong can be moved, and it's easy since it doesn't exist in the next row.
						fCurrentRow.removeLastBong();
						fNextRow.addBongAtLead(firstRowLie.swapStroke());
						finishRow();
						fNextRow.addBong(bong);
						return;
					}
				}
				// Nope - carry on.
			}
		}

		finishRow();
		fNextRow.addBong(bong);
	}
	
	private int getCyclicSplit(RawRow row)
	{
		RawRow rotatedRow = new RawRow(row.isHandstroke());
		Iterator<Bong> it = row.iterator();
		Bong b1 = it.next();
		int split = 1;
		while (it.hasNext())
		{
			Bong b2 = it.next();
			if (b1.bell-b2.bell>=fNBells-2)
				break;
			b1 = b2;
			split++;
		}
		if (split<row.getRowSize())
		{
			for (int i=split; i<row.getRowSize(); i++)
				rotatedRow.addBong(row.getBong(i+1));
			for (int i=0; i<split; i++)
				rotatedRow.addBong(row.getBong(i+1));
			if (rotatedRow.isCloseToRounds())
				return split;
		}
		return -1;
	}

	protected void finishRow()
	{
		if (LOG_OUTPUT)
			System.out.println("LeadLieCorrector: "+fCurrentRow.rowAsString());
		for (Bong bong: fCurrentRow)
		{
			fNextStage.receiveBong(bong);
			fLastKnownGoodPositions[bong.bell-1] = fCurrentRow.findBell(bong.bell);
		}
		fCurrentRow = fNextRow;
		fNextRow = new RawRow(!fCurrentRow.isHandstroke());
		fRowsProcessed++;
	}

	public void notifyInputComplete()
	{
		finishRow();
		finishRow();
		fNextStage.notifyInputComplete();
	}

}
