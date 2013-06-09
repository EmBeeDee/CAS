package org.pealfactory.strike.data;

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
public class AveragedRow implements Row
{
	/* Good and bad standard deviations, milliseconds accuracy */
	public final static double GOOD_CUTOFF = 30;
	public final static double BAD_CUTOFF = 60;

	private Row fRow;
	/** Values calculated by the Visualiser in effect */
	private int fRowEndTime;
	/** As a proportion of average inter-bell gap, i.e. 1.0 is nominal (and may be set for all rows by some Visualisers) */
	private double fHandstrokeGap;
	private int fRowDuration;

  private double fAverageGap;
	private double fRowVariance;
	private double fDiscreteRowVariance;

	private int fWholePullDuration;

	/** Set for rows which are believed to be "in changes", i.e. not part of initial and finishing rounds */
	private boolean fInChanges;

	/** If we're in changes, a count of the number of preceding consecutive rows of changes; or if we're in rounds,
	 *  a count of the number of preceding consecutive rows of rounds. Used to ignore short bursts of changes whilst
	 *  we're ringing rounds (e.g. caused by failure to settle into rounds properly).  */
	private int fInChangesCount;

  public AveragedRow(Row row, int endTime, double handstrokeGap, int duration)
	{
		fRow = row;
		fRowEndTime = endTime;
		fHandstrokeGap = handstrokeGap;
		fRowDuration = duration;
		calcStats();
	}

	private void calcStats()
	{
		// Calculate average gap
		double n = getNBells();
		if (isHandstroke())
			n+= fHandstrokeGap;
		fAverageGap = (double)fRowDuration/n;

		// Calculate row variance.
		n = getRowSize();
		double d = 0.0;
		double dd = 0.0;
		for (int i=0; i<n; i++)
		{
      double t = getStrikeTime(i+1)-getCorrectStrikeTime(i+1);
			d+= t*t;
			int x = (int)(Math.abs(t)/GOOD_CUTOFF);
			t = x*GOOD_CUTOFF;
			/*
			if (t<GOOD_CUTOFF)
				t = 0;
				*/
			dd+= t*t;
		}
		if (n>0)
		{
			d = d/n;
			dd = dd/n;
		}
		fRowVariance = d;
		fDiscreteRowVariance = dd;
	}

	public String toString()
	{
		return fRow.toString();
	}

	/**
	 * @param place 1..n
	 * @return
	 */
	public Bong getBong(int place)
	{
		return fRow.getBong(place);
	}

	/**
	 * @param place 1..n
	 * @return
	 */
	public int getBellAt(int place)
	{
		return fRow.getBellAt(place);
	}

	/**
	 * @param place 1..n
	 * @return
	 */
	public int getStrikeTime(int place)
	{
		return fRow.getStrikeTime(place);
	}

	public int findBell(int bell)
	{
		return fRow.findBell(bell);
	}

	/**
	 *
	 * @param place 1..n
	 * @return
	 */
	public int getCorrectStrikeTime(int place)
	{
		double timePerBell = fAverageGap;
		int correctTime = fRowEndTime-(int)(timePerBell*(getNBells()-place));
		return correctTime;
	}

	public int getLatenessMilliseconds(int place)
	{
		return getStrikeTime(place) - getCorrectStrikeTime(place);
	}

	public double getPercentageDeviation()
	{
		return getStandardDeviation()/fAverageGap;
	}

	public double getStandardDeviation()
	{
		return Math.sqrt(fRowVariance);
	}

	public double getVariance()
	{
    return fRowVariance;
	}

	public double getDiscreteVariance()
	{
    return fDiscreteRowVariance;
	}

	/**
	 * Returns the ideal average gap between bells, as calculated by the analyser
	 *
	 * @return
	 */
	public double getAveragedGap()
	{
		return fAverageGap;
	}

	/**
	 * Returns the actual inter-bell gap (not including lead, so really just last bell - first bell
	 * divided by n-1).
	 *
	 * @return
	 */
	public double getMeanInterbellGap()
	{
		double d = 0.0;
		if (getRowSize()>1)
			d =  (getBong(getRowSize()).time-getBong(1).time)/(getRowSize()-1);
		return d;
	}

	public boolean isHandstroke()
	{
		return fRow.isHandstroke();
	}

	public int getRowEndTime()
	{
		return fRowEndTime;
	}

	public int getRowDuration()
	{
		return fRowDuration;
	}

	public int getWholePullDuration()
	{
		return fWholePullDuration;
	}

	public void setWholePullDuration(int duration)
	{
		fWholePullDuration = duration;
	}

	public double getHandstrokeGap()
	{
		return fHandstrokeGap;
	}

	/**
	 * Should take into account backstroke speed too?
	 *
	 * @return
	 */
	public double getHandstrokeGapMs()
	{
		return fHandstrokeGap*fAverageGap;
	}

	public int getNBells()
	{
		return fRow.getNBells();
	}

	public int getRowSize()
	{
		return fRow.getRowSize();
	}

	public boolean isGood()
	{
		return getStandardDeviation()<=GOOD_CUTOFF;
	}

	public boolean isBad()
	{
		return getStandardDeviation()>=BAD_CUTOFF;
	}

	public boolean isInChanges()
	{
		return fInChanges;
	}

	public void setIsInChanges(boolean inChanges)
	{
		fInChanges = inChanges;
	}

	public boolean isCloseToRounds()
	{
    return fRow.isCloseToRounds();
	}

	public int getInChangesCount()
	{
		return fInChangesCount;
	}

	public void setInChangesCount(int inChangesCount)
	{
		fInChangesCount = inChangesCount;
	}

}
