package org.pealfactory.strike.analyser;

import org.pealfactory.strike.data.*;

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
 * @author Mark
 */
public class McBurnieVisualiser extends VisualiserHelper
{
	public final static String NAME = "McBurnie Visualiser";
	public final static String INFO = "The McBurnie Visualiser considers each whole pull in two phases: first an estimate for the length of the row is made from the average strike time of all the bells in the row; then outliers are discarded and the least-squares method used to find a best-fir line for the remaining 'good' bells. Handstroke gap is derived from the average of all handstroke gaps rung so far.";

	private Row fPreviousBackstroke;
	private Row fHandstroke;
	private int fExpectedEndOfLastBackstroke;
	private int fPreviousExpectedGap;
	private int fTotalActualHandstrokeGap = 0;
	private int fTotalAvGap = 0;

	public McBurnieVisualiser(String name, String info)
	{
		super(NAME, INFO);
	}


	@Override
	protected void newRow(Row row)
	{
		if (row.isHandstroke())
		{
			fHandstroke = row;
		}
		else
		{
			// Work out expected handstroke strike time
			int oneBeforeHandstroke;
			int actualHandstrokeStrike = fHandstroke.getStrikeTime(1);
			int expectedHandstrokeStrike;
			double currentHandstrokeGapRatio;
			if (fPreviousBackstroke==null)
			{
				// First whole pull
				expectedHandstrokeStrike = actualHandstrokeStrike;
				oneBeforeHandstroke = actualHandstrokeStrike;
				currentHandstrokeGapRatio = 2.0;
			}
			else
			{
				int actualEndOfLastBackstroke = fPreviousBackstroke.getStrikeTime(row.getRowSize());
				fTotalActualHandstrokeGap+= actualHandstrokeStrike - actualEndOfLastBackstroke;
				currentHandstrokeGapRatio = ((double)fTotalActualHandstrokeGap)/fTotalAvGap;
				oneBeforeHandstroke = actualHandstrokeStrike - (int)((actualHandstrokeStrike - actualEndOfLastBackstroke)/currentHandstrokeGapRatio);
				expectedHandstrokeStrike = fExpectedEndOfLastBackstroke + (int)(currentHandstrokeGapRatio*fPreviousExpectedGap);
				expectedHandstrokeStrike = (expectedHandstrokeStrike+actualHandstrokeStrike)/2;
			}

			// Work out whole pull midpoint strike time, and from it the actual average gap for the whole pull
			double midStrikeTime = 0;
			for (int place=1; place<=fHandstroke.getRowSize(); place++)
				midStrikeTime+= fHandstroke.getStrikeTime(place)-expectedHandstrokeStrike;
			for (int place=1; place<=row.getRowSize(); place++)
				midStrikeTime+= row.getStrikeTime(place)-expectedHandstrokeStrike;
			int nbellsStrikingThisWholePull = fHandstroke.getRowSize()+row.getRowSize();
			midStrikeTime = midStrikeTime/nbellsStrikingThisWholePull;
			double avGap = midStrikeTime/(getNBells()-0.5);

			// Create pairs (place, strike time) for each bell in each row, including missing bells
			// (these will be indicated with strike time = -1)
			int actualInterRowGap = row.getStrikeTime(1)-fHandstroke.getStrikeTime(fHandstroke.getRowSize());
			List<Integer> handstrokeStrikeTimes = getStrikeTimesInRowIncludingMissingBells(fHandstroke, actualHandstrokeStrike-oneBeforeHandstroke, actualInterRowGap);
			// TODO might need to know the gap to the start of the next handstroke!
			List<Integer> backstrokeStrikeTimes = getStrikeTimesInRowIncludingMissingBells(row, actualInterRowGap, (int)avGap);

			// Weed out the outliers
			// TODO mustn't weed too many out!!
			List<Integer> wholePullStrikeTimes = new ArrayList<Integer>(handstrokeStrikeTimes);
			wholePullStrikeTimes.addAll(backstrokeStrikeTimes);
			weedOutBadBells(wholePullStrikeTimes, expectedHandstrokeStrike, avGap);

			// Now perform least-squares fit on the remaining "good" bells.
			double sumX = 0;
			double sumXsquared = 0;
			double sumXY = 0;
			double sumY = 0;
			int n = 0;
			for (int i=0; i<wholePullStrikeTimes.size(); i++)
			{
				if (wholePullStrikeTimes.get(i)>0)
				{
					n++;
					sumX+= i;
					sumXsquared+= i*i;
					sumXY+= i*wholePullStrikeTimes.get(i);
					sumY+= wholePullStrikeTimes.get(i);
				}
			}
			double divisor = n*sumXsquared - sumX*sumX;
			double a = (sumY*sumXsquared - sumX*sumXY)/divisor;
			double b = (n*sumXY - sumX*sumY)/divisor;

			// Now expected strike times are a + b*place
			fExpectedEndOfLastBackstroke = (int)(a+b*getNBells()*2);
			fPreviousExpectedGap = (int)b;
			fTotalAvGap+= avGap;
			addAveragedRow(fHandstroke, (int)(a+b*getNBells()), currentHandstrokeGapRatio-1.0, (int)(b*getNBells()));
			addAveragedRow(row, fExpectedEndOfLastBackstroke, currentHandstrokeGapRatio-1.0, (int)(b*getNBells()));
			fPreviousBackstroke = row;
		}
	}
	
	private void weedOutBadBells(List<Integer> strikeTimes, int expectedStart, double avGap)
	{
		double goodStrikeTolerance = avGap/4;
		double expectedStrike = expectedStart;
		for (int i=0; i<strikeTimes.size(); i++)
		{
			if (strikeTimes.get(i)>0)
				if (Math.abs(strikeTimes.get(i)-expectedStart)>goodStrikeTolerance)
					strikeTimes.set(i, -1);
			expectedStrike+= avGap;
		}
	}

	/**
	 * Returns a list with exactly nBells strike time integers, extracted from the supply row.
	 * If the row contains fewer bells (i.e. some strikes are missing), the returned list will contain markers
	 * for these missing bells, in the shape of entries with strike time equal to -1. The position of the marker
	 * entries is inferred from the size of gaps between the bells which actually struck in the row, including
	 * the gap before the first and last bells (which must be supplied). Basically, if one bell is missing, a -1 will
	 * be inserted in the largest gap; and so on for more missing bells.
	 *
	 * @param row
	 * @param firstGap
	 * @return
	 */
	private List<Integer> getStrikeTimesInRowIncludingMissingBells(Row row, int firstGap, int lastGap)
	{
		List<Integer> strikeTimes = new ArrayList<Integer>(getNBells());
		int nStrikes = row.getRowSize();
		for (int place=1; place<=nStrikes; place++)
			strikeTimes.add(row.getStrikeTime(place));
		int oneBeforeFirstStrike = strikeTimes.get(0)-firstGap;
		while (nStrikes<getNBells())
		{
			int biggestGap = -1;
			int indexOfBiggestGap = 0;
			int prevStrike = oneBeforeFirstStrike;
			int missingBellCount = 0;
			for (int i=0; i<=nStrikes; i++)
			{
				int strikeTime = i<nStrikes? strikeTimes.get(i) : prevStrike+lastGap;
				if (strikeTime<0)
				{
					missingBellCount++;
				}
				else
				{
					int gap = strikeTime-prevStrike;
					int gapIncludingMissingBells = gap/(missingBellCount+1);
					if (gapIncludingMissingBells>biggestGap)
					{
						biggestGap = gapIncludingMissingBells;
						indexOfBiggestGap = i;
					}
					missingBellCount = 0;
					prevStrike = strikeTime;
				}
			}
			strikeTimes.add(indexOfBiggestGap, -1);
			nStrikes++;
		}
		return strikeTimes;
	}
}
