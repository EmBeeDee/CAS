package org.pealfactory.strike.data;

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
 *
 * @author MBD
 */
public class AveragedRowData implements AveragedRowSource
{
	public static final boolean LOG_DEVIATIONS = false;

	private int fNBells;
	private List<AveragedRow> fRows;

  public AveragedRowData()
	{
		fRows = new ArrayList();
		fNBells = 0;
	}

	public synchronized AveragedRow getRow(int i)
	{
		return fRows.get(i);
	}

	public synchronized int getNRows()
	{
		return fRows.size();
	}

	public synchronized int getNBells()
	{
		return fNBells;
	}

	/**
	 * Add an averaged row with the given row end time, the default handstroke gap (1.0), and row duration
	 * calculated from the end of the previous row.
	 *
	 * @param row
	 * @param endTime
	 */
	public void addRow(Row row, int endTime)
	{
		addRow(row, endTime, 1.0);
	}

	/**
	 * Add an averaged row with the given row end time and handstroke gap, but row duration
	 * calculated from the end of the previous row.
	 *
	 * @param row
	 * @param endTime
	 * @param handstrokeGap
	 */
	public void addRow(Row row, int endTime, double handstrokeGap)
	{
		// work out row duration - just the gap between one row end and the next.
		int lastRow = fRows.size() - 1;
		int duration;
		if (lastRow>=0)
		{
			duration = endTime-getRow(lastRow).getRowEndTime();
		}
		else if (row.getRowSize()>1)
		{
			// Special case for first row - the end of the "last" row is really the start
			// of this row, but we have to guess when that was. Remember the first row
			// is a handstroke so the previous row end is two bell gaps before the treble.
			duration = row.getStrikeTime(row.getRowSize())-row.getStrikeTime(1);
			duration = duration + (int)((1.0+handstrokeGap)*duration/(row.getRowSize()-1));
		}
		else
		{
			// Problem case if only one bell struck in first row!
			// Adopt one second.
			duration = 1000;
		}
		addRow(row, endTime, handstrokeGap, duration);
	}

	/**
	 * Add an averaged row with the given row end time, handstroke gap and row duration.
	 * Note that if the given row duration does not match the distance between the row end time
	 * and the previous row's end time, then a discontinuity in the display may occur.
	 * For the very first row, a duration value of at least the length of the row should be passed.
	 *
	 * @param row
	 * @param endTime
	 * @param handstrokeGap
	 * @param duration
	 */
	public void addRow(Row row, int endTime, double handstrokeGap, int duration)
	{
		addRow(new AveragedRow(row, endTime, handstrokeGap, duration));
	}

	/** How many changes of e.g. not-round we need before deciding it really isn't rounds; and vice versa */
	private static final int IN_CHANGES_SENSITIVITY = 1;

	/**
	 * Synchronization very important - other users of AveragedRowData may want to synch on the
	 * instance whilst creating TouchStats. Without the synch lock, the stats could be invalid,
	 * since an addRow operation could occur in the middle.
	 *
	 * @param row
	 */
	private synchronized void addRow(AveragedRow row)
	{
		fRows.add(row);
		fNBells = Math.max(fNBells, row.getNBells());

		int n = fRows.size();
		// At every backstroke, calculate whole pull durations, store in both hand & back rows.
		if (!row.isHandstroke())
		{
			int duration = row.getRowDuration();
			if (n>1)
			{
				AveragedRow handstrokeRow = getRow(n-2);
				duration+= handstrokeRow.getRowDuration();
				handstrokeRow.setWholePullDuration(duration);
			}
			row.setWholePullDuration(duration);
		}

		// Set flag to indicate whether this row is "in changes" or not.
		// Initially, just set "in changes" if the row doesn't appear to be rounds.
		// A row is in rounds if all bells strike in pitch-descending order; two-bell swaps are allowed if timing is close.
		boolean currentInChanges = !row.isCloseToRounds();
		row.setIsInChanges(currentInChanges);
    // Now refine this naive decision based on what previous changes have been
		if (n>1)
		{
			AveragedRow prevRow = getRow(n-2);
			if (currentInChanges==prevRow.isInChanges())
			{
				// We're the same as the previous row - increment the "run" counter
				int count = prevRow.getInChangesCount()+1;
				row.setInChangesCount(count);
				// If the run counter gets high enough, we are now certain what state we are in.
				// Now we need to look at the previous segment - if it was only a short run, we ignore it, resetting 
				// the "inchanges" flags back to the current, long run.
				if (count>=IN_CHANGES_SENSITIVITY)
				{
					int end = n-count-2;
					resetInChanges(currentInChanges, end);
				}
			}
			else
			{
				// We're different to the previous row
				row.setInChangesCount(0);
			}
		}
		if (LOG_DEVIATIONS)
		{
			StringBuffer s = new StringBuffer("Row ");
			s.append(fRows.size());
			s.append(":");
			for (int i=1; i<=row.getRowSize(); i++)
			{
				s.append(" ");
				s.append(row.getStrikeTime(i)-row.getCorrectStrikeTime(i));
			}

			System.out.println(s);
		}
	}

	private void resetInChanges(boolean currentInChanges, int end)
	{
		if (end>0)
		{
			AveragedRow endOfPreviousSegment = getRow(end);
			int prevCount = endOfPreviousSegment.getInChangesCount();
			if (prevCount<IN_CHANGES_SENSITIVITY)
			{
				endOfPreviousSegment.setIsInChanges(currentInChanges);
				for (int i=1; i<=prevCount; i++)
					getRow(end-i).setIsInChanges(currentInChanges);
				// Recursively sort out any even earlier small segments
				resetInChanges(currentInChanges, end-prevCount-1);
			}
			else if (currentInChanges)
			{
				// Always mark the last row of rounds as "inchanges", to make it part of the touch.
				endOfPreviousSegment.setIsInChanges(true);
			}
		}
	}

}
