package org.pealfactory.strike.data;

import java.util.*;
import java.io.PrintWriter;

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
public class TouchStats implements AveragedRowSource
{
	public static final int WHOLEPULL = 0;
	public static final int HANDSTROKE = 1;
	public static final int BACKSTROKE = 2;

	public static final int MAXFAULTSPERROW = 4;
	public static final double FAULTFACTOR = 0.75;

	public static final String TEXT_STRIKING_RMSE = "Striking RMSE";
	public static final String TEXT_DISCRETE_RMSE = "Discrete RMSE";
	public static final String TEXT_STRIKING_SD = "Striking SD";
	public static final String TEXT_DISCRETE_SD = "Discrete SD";
	public static final String TEXT_INTERVAL_MEAN = "Interval mean";
	public static final String TEXT_QUICKEST_ROW = "Quickest row";
	public static final String TEXT_SLOWEST_ROW = "Slowest row";
	public static final String TEXT_ROW_LENGTH_SD = "Row length SD";
	public static final String TEXT_FAULTS = "Faults";
	/** These ones for individual bell stats */
	public static final String TEXT_SD = "Std deviation";
	public static final String TEXT_RMSE = "RMS Error";
	public static final String TEXT_AV_MS_LATE = "Av ms late";


	private AveragedRowSource fData;
	private int fNRows;
	private int fNBells;
	private Map<String,Double> fStatsCache = new HashMap();

  public TouchStats(AveragedRowSource data, int nbells)
	{
		fData = data;
		// We store the number of rows to capture the value at the moment of construction. All the stats operations
		// are based on rows up to this value, so are guaranteed to be constant even if another thread adds more rows.
		// Note no need to synchronize as this is an atomic operation.
		fNRows = fData.getNRows();
		fNBells = nbells;
		// Cache stats which redraw code on the event thread might need - min & max durations.
		getMinDuration(false);
		getMaxDuration(false);
	}

	public static class HandBackWhole
	{
		public double hand = 0.0;
		public double back = 0.0;
		public double whole = 0.0;
	}

	public static class HandBackWholeInt
	{
		public int hand = 0;
		public int back = 0;
		public int whole = 0;
	}

	public AveragedRow getRow(int i)
	{
		return fData.getRow(i);
	}

	/**
	 * Don't ask the data for the number of rows - might have increased since we were constructed.
	 *
	 * @return
	 */
	public int getNRows()
	{
		return fNRows;
	}

	public int getNBells()
	{
		return fNBells;
	}

	public void outputStats(PrintWriter out, boolean inChangesOnly)
	{
    out.println("Touch stats calculated from "+getNRows()+" rows");
		out.println("Faults: "+getFaults(inChangesOnly)+", "+getFaultPercentage(inChangesOnly)+"%");
		out.println("Metric, whole, hand, back");
		_outThree(out, TEXT_STRIKING_RMSE, getStrikingRMSE(inChangesOnly));
		_outThree(out, TEXT_DISCRETE_RMSE, getDiscreteStrikingRMSE(inChangesOnly));
    _outThree(out, TEXT_INTERVAL_MEAN, getMeanInterbellGap(inChangesOnly));
    _outThree(out, TEXT_QUICKEST_ROW, getMinDuration(inChangesOnly));
		_outThree(out, TEXT_SLOWEST_ROW, getMaxDuration(inChangesOnly));
		_outThree(out, TEXT_ROW_LENGTH_SD, getRowLengthSD(inChangesOnly));
		for (int i=1; i<=getNBells(); i++)
		{
			out.println("Bell "+i);
			_outThree(out, TEXT_SD, getBellSD(i, inChangesOnly));
			_outThree(out, TEXT_RMSE, getBellRMSE(i, inChangesOnly));
      _outThree(out, TEXT_AV_MS_LATE, getLateness(i, inChangesOnly));
		}
		out.println();
	}

	private void _outThree(PrintWriter out, String text, HandBackWhole stats)
	{
		out.println(text+", "+stats.whole+", "+stats.hand+", "+stats.back);
	}

	private void _outThree(PrintWriter out, String text, HandBackWholeInt stats)
	{
		out.println(text+", "+stats.whole+", "+stats.hand+", "+stats.back);
	}

	protected void visitRows(AveragedRowVisitor visitor, int stroke, boolean inChanges)
	{
		if (fNRows==0)
			return;
		int i = 0;
		int step = 2;
		if (stroke==WHOLEPULL)
			step = 1;
		else if (stroke==HANDSTROKE && !getRow(0).isHandstroke())
			i++;
		else if (stroke==BACKSTROKE && getRow(0).isHandstroke())
			i++;
		for (; i<fNRows; i+=step)
		{
			AveragedRow row = getRow(i);
			if (row==null)
				System.out.println("Null Row in visitRows = "+i+" out of "+fNRows);
			else
			{
				boolean rowWorthVisiting = true;
				// Don't visit row if we're only marking changes, and we're not in changes
				if (inChanges && !row.isInChanges())
					rowWorthVisiting = false;
				// Don't visit row if not all bells struck in it
				//if (row.getRowSize()<fNBells)
				//	rowWorthVisiting = false;
				if (rowWorthVisiting)
					visitor.visit(row);
			}
		}
	}

	protected synchronized double cachedVisitRows(AveragedRowVisitor visitor, int stroke, boolean inChanges, String cacheKey)
	{
		cacheKey = cacheKey+"/"+stroke+"/"+inChanges;
    Double cacheValue = fStatsCache.get(cacheKey);
		if (cacheValue==null)
		{
			visitRows(visitor, stroke, inChanges);
			cacheValue = new Double(visitor.getResult());
			fStatsCache.put(cacheKey, cacheValue);
		}
		return cacheValue.doubleValue();
	}

	/** Stats getters */

	public int getFaults(boolean inChanges)
	{
		return getFaults(inChanges, FAULTFACTOR);
	}

	public int getFaults(boolean inChanges, double faultFactor)
	{
		return (int)cachedVisitRows(new RowTotalVisitor(new RowFaultsRetriever(faultFactor)), WHOLEPULL, inChanges, "Faults");
	}

	public double getFaultPercentage(boolean inChanges)
	{
		int nrows = (int)cachedVisitRows(new RowTotalVisitor(new RowExistenceRetriever()), WHOLEPULL, inChanges, "NRows");
		double max = nrows*MAXFAULTSPERROW;
		return (max-getFaults(inChanges))/max;
	}

	public HandBackWholeInt getMinDuration(boolean inChanges)
	{
		HandBackWholeInt ret = new HandBackWholeInt();
		ret.hand = _getMinDuration(HANDSTROKE, inChanges);
		ret.back = _getMinDuration(BACKSTROKE, inChanges);
		ret.whole = _getMinDuration(WHOLEPULL, inChanges);
		return ret;
	}
	private int _getMinDuration(int stroke, boolean inChanges)
	{
		AveragedRowVisitor v;
		String cacheKey = "MinDuration";
		if (stroke==WHOLEPULL)
		{
			v = new RowMinVisitor(new WholePullDurationRetriever());
			stroke = BACKSTROKE;
			cacheKey+= "Whole";
		}
		else
		{
			v = new RowMinVisitor(new RowDurationRetriever());
		}
		return (int)cachedVisitRows(v, stroke, inChanges, cacheKey);
	}

	public HandBackWholeInt getMaxDuration(boolean inChanges)
	{
		HandBackWholeInt ret = new HandBackWholeInt();
		ret.hand = _getMaxDuration(HANDSTROKE, inChanges);
		ret.back = _getMaxDuration(BACKSTROKE, inChanges);
		ret.whole = _getMaxDuration(WHOLEPULL, inChanges);
		return ret;
	}
	public int _getMaxDuration(int stroke, boolean inChanges)
	{
		AveragedRowVisitor v;
		String cacheKey = "MaxDuration";
		if (stroke==WHOLEPULL)
		{
			v = new RowMaxVisitor(new WholePullDurationRetriever());
			stroke = BACKSTROKE;
			cacheKey+= "Whole";
		}
		else
		{
			v = new RowMaxVisitor(new RowDurationRetriever());
		}
		return (int)cachedVisitRows(v, stroke, inChanges, cacheKey);
	}

	public HandBackWhole getMeanInterbellGap(boolean inChanges)
	{
		HandBackWhole ret = new HandBackWhole();
		ret.hand = _getMeanInterbellGap(HANDSTROKE, inChanges);
		ret.back = _getMeanInterbellGap(BACKSTROKE, inChanges);
		ret.whole = _getMeanInterbellGap(WHOLEPULL, inChanges);
		return ret;
	}
	private double _getMeanInterbellGap(int stroke, boolean inChanges)
	{
		return cachedVisitRows(new RowMeanVisitor(new InterbellGapRetriever()), stroke, inChanges, "MeanInterbellGap");
	}

	public HandBackWhole getRowLengthSD(boolean inChanges)
	{
		HandBackWhole ret = new HandBackWhole();
		ret.hand = _getRowLengthSD(HANDSTROKE, inChanges);
		ret.back = _getRowLengthSD(BACKSTROKE, inChanges);
		ret.whole = _getRowLengthSD(WHOLEPULL, inChanges);
		return ret;
	}
	private double _getRowLengthSD(int stroke, boolean inChanges)
	{
		double rowLengthMean = _getMeanRowLength(stroke, inChanges);
		AveragedRowVisitor v = new RowMeanVisitor(new RowValueVarianceRetriever(new RowDurationRetriever(), rowLengthMean));
    return Math.sqrt(cachedVisitRows(v, stroke, inChanges, "RowLengthSD"));
	}

	public HandBackWhole getMeanRowLength(boolean inChanges)
	{
		HandBackWhole ret = new HandBackWhole();
		ret.hand = _getMeanRowLength(HANDSTROKE, inChanges);
		ret.back = _getMeanRowLength(BACKSTROKE, inChanges);
		ret.whole = _getMeanRowLength(WHOLEPULL, inChanges);
		return ret;
	}
	private double _getMeanRowLength(int stroke, boolean inChanges)
	{
		return cachedVisitRows(new RowMeanVisitor(new RowDurationRetriever()), stroke, inChanges, "MeanRowLength");
	}

	public HandBackWhole getDiscreteStrikingRMSE(boolean inChanges)
	{
		HandBackWhole ret = new HandBackWhole();
		ret.hand = _getDiscreteStrikingRMSE(HANDSTROKE, inChanges);
		ret.back = _getDiscreteStrikingRMSE(BACKSTROKE, inChanges);
		ret.whole = _getDiscreteStrikingRMSE(WHOLEPULL, inChanges);
		return ret;
	}
	private double _getDiscreteStrikingRMSE(int stroke, boolean inChanges)
	{
		return Math.sqrt(cachedVisitRows(new RowMeanVisitor(new RowDiscreteVarianceRetriever()), stroke, inChanges, "DiscreteRMSE"));
	}

	public HandBackWhole getStrikingRMSE(boolean inChanges)
	{
		HandBackWhole ret = new HandBackWhole();
		ret.hand = _getStrikingRMSE(HANDSTROKE, inChanges);
		ret.back = _getStrikingRMSE(BACKSTROKE, inChanges);
		ret.whole = _getStrikingRMSE(WHOLEPULL, inChanges);
		return ret;
	}
	private double _getStrikingRMSE(int stroke, boolean inChanges)
	{
		return Math.sqrt(cachedVisitRows(new RowMeanVisitor(new RowStrikingVarianceRetriever()), stroke, inChanges, "StrikingRMSE"));
	}

	public HandBackWhole getBellSD(int bell, boolean inChanges)
	{
		HandBackWhole ret = new HandBackWhole();
		ret.hand = _getBellSD(bell, HANDSTROKE, inChanges);
		ret.back = _getBellSD(bell, BACKSTROKE, inChanges);
		ret.whole = _getBellSD(bell, WHOLEPULL, inChanges);
		return ret;
	}
	private double _getBellSD(int bell, int stroke, boolean inChanges)
	{
		double meanLateness = _getLateness(bell, stroke, inChanges);
		AveragedRowVisitor v = new BellMeanVisitor(new BellValueVarianceRetriever(new BellLatenessRetriever(), meanLateness), bell);
		return Math.sqrt(cachedVisitRows(v, stroke, inChanges, "BellSD"+bell));
	}

	public HandBackWhole getBellRMSE(int bell, boolean inChanges)
	{
		HandBackWhole ret = new HandBackWhole();
		ret.hand = _getBellRMSE(bell, HANDSTROKE, inChanges);
		ret.back = _getBellRMSE(bell, BACKSTROKE, inChanges);
		ret.whole = _getBellRMSE(bell, WHOLEPULL, inChanges);
		return ret;
	}
	private double _getBellRMSE(int bell, int stroke, boolean inChanges)
	{
		AveragedRowVisitor v = new BellMeanVisitor(new BellValueVarianceRetriever(new BellLatenessRetriever(), 0), bell);
		return Math.sqrt(cachedVisitRows(v, stroke, inChanges, "BellRMSE"+bell));
	}

	public HandBackWhole getLateness(int bell, boolean inChanges)
	{
		HandBackWhole ret = new HandBackWhole();
		ret.hand = _getLateness(bell, HANDSTROKE, inChanges);
		ret.back = _getLateness(bell, BACKSTROKE, inChanges);
		ret.whole = _getLateness(bell, WHOLEPULL, inChanges);
		return ret;
	}
	private double _getLateness(int bell, int stroke, boolean inChanges)
	{
		return cachedVisitRows(new BellMeanVisitor(new BellLatenessRetriever(), bell), stroke, inChanges, "BellLateness"+bell);
	}

	public double getMeanHandstrokeGap(boolean inChanges)
	{
		return cachedVisitRows(new RowMeanVisitor(new HandstrokeGapRetriever()), HANDSTROKE, inChanges, "MeanHandstrokeGap");
	}

	public double getHandstrokeGapSD(boolean inChanges)
	{
		double hgMean = getMeanHandstrokeGap(inChanges);
		AveragedRowVisitor v = new RowMeanVisitor(new RowValueVarianceRetriever(new HandstrokeGapRetriever(), hgMean));
    return Math.sqrt(cachedVisitRows(v, HANDSTROKE, inChanges, "HandstrokeGapSD"));
	}

	public double getMeanBellHandstrokeGap(int bell, boolean inChanges)
	{
		return cachedVisitRows(new PlacedBellMeanVisitor(new HandstrokeGapRetriever(), bell, 1), HANDSTROKE, inChanges, "MeanBellHandstrokeGap"+bell);
	}

	public double getBellHandstrokeGapSD(int bell, boolean inChanges)
	{
		double hgMean = getMeanBellHandstrokeGap(bell, inChanges);
		AveragedRowVisitor v = new PlacedBellMeanVisitor(new RowValueVarianceRetriever(new HandstrokeGapRetriever(), hgMean), bell, 1);
    return Math.sqrt(cachedVisitRows(v, HANDSTROKE, inChanges, "BellHandstrokeGapSD"+bell));
	}


	/**
	 *
	 */
	abstract class BaseRowVisitor implements AveragedRowVisitor
	{
		protected double d = 0;

		public double getResult()
		{
			return d;
		}
	}

	/**
	 *
	 */
	class RowTotalVisitor extends BaseRowVisitor
	                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      {
		RowValueRetriever retriever;

		public RowTotalVisitor(RowValueRetriever r)
		{
			retriever = r;
		}

		public void visit(AveragedRow row)
		{
			d+= retriever.getValue(row);
		}

		public double getResult()
		{
			return d;
		}
	}

  /**
   *
   */
  class RowMeanVisitor extends BaseRowVisitor
	{
	  RowValueRetriever retriever;
	  protected int c = 0;

	  public RowMeanVisitor(RowValueRetriever r)
  	{
		  retriever = r;
	  }

	  public void visit(AveragedRow row)
	  {
		  d+= retriever.getValue(row);
		  c++;
	  }

	  public double getResult()
	  {
		  if (c>0)
			  d = d/c;
		  return d;
	  }
  }

	/**
	 *
	 */
	class BellMeanVisitor extends BaseRowVisitor
	{
		BellValueRetriever retriever;
		protected int c = 0;
		protected int bell;

		BellMeanVisitor(BellValueRetriever r, int b)
		{
			retriever = r;
			bell = b;
		}

		public void visit(AveragedRow row)
		{
			int place = row.findBell(bell);
			if (place>0)
			{
				d+= retriever.getValue(row, place);
				c++;
			}
		}

		public double getResult()
		{
			if (c>0)
				d = d/c;
			return d;
		}
	}

	/**
	 *
	 */
	class PlacedBellMeanVisitor extends BellMeanVisitor
	{
		RowValueRetriever rowRetriever;
		protected int place;

		PlacedBellMeanVisitor(BellValueRetriever r, int b, int p)
		{
			super(r, b);
			place = p;
		}

		PlacedBellMeanVisitor(RowValueRetriever r, int b, int p)
		{
			super(null, b);
			rowRetriever = r;
			place = p;
		}

		public void visit(AveragedRow row)
		{
			int p = row.findBell(bell);
			if (p>0 && p==place)
			{
				if (retriever==null)
					d+= rowRetriever.getValue(row);
				else
					d+= retriever.getValue(row, place);
				c++;
			}
		}
	}

	/**
	 *
	 */
	class RowMaxVisitor extends BaseRowVisitor
	{
		RowValueRetriever retriever;

		public RowMaxVisitor(RowValueRetriever r)
		{
			retriever = r;
		}

		public void visit(AveragedRow row)
		{
			d = Math.max(d, retriever.getValue(row));
		}
	}

	/**
	 *
	 */
	class RowMinVisitor extends BaseRowVisitor
	{
		RowValueRetriever retriever;

		public RowMinVisitor(RowValueRetriever r)
		{
			retriever = r;
			d = Integer.MAX_VALUE;
		}

		public void visit(AveragedRow row)
		{
			d = Math.min(d, retriever.getValue(row));
		}
	}

	/**
	 *
	 */
	class RowExistenceRetriever implements RowValueRetriever
	{
		public double getValue(AveragedRow row)
		{
			return 1;
		}
	}

	/**
	 *
	 */
	class RowFaultsRetriever implements RowValueRetriever
	{
		/** Proportion of inter-bell gap deemed to be a fault */
		double faultFactor;
		int t;

		public RowFaultsRetriever(double ff)
		{
			faultFactor = ff;
			t = 0;
		}

		public double getValue(AveragedRow row)
		{
			int nfaults = 0;
			double maxGoodGap = faultFactor*row.getMeanInterbellGap();
			int i=1;
			if (row.isHandstroke())
			{
				t = row.getBong(i++).time;
			}
			while (i<=row.getRowSize())
			{
				int d = row.getBong(i++).time;
				if (Math.abs(d-t)<maxGoodGap)
					nfaults++;
				t = d;
			}
			if (nfaults>MAXFAULTSPERROW)
				nfaults = MAXFAULTSPERROW;
			return nfaults;
		}
	}

	/**
	 *
	 */
	class RowValueVarianceRetriever implements RowValueRetriever
	{
		RowValueRetriever delegate;
		double mean;

		public RowValueVarianceRetriever(RowValueRetriever r, double m)
		{
			delegate = r;
			mean = m;
		}

		public double getValue(AveragedRow row)
		{
			double x = delegate.getValue(row) - mean;
			return x*x;
		}
	}

	/**
	 *
	 */
	class BellValueVarianceRetriever implements BellValueRetriever
	{
		BellValueRetriever delegate;
		double mean;

		public BellValueVarianceRetriever(BellValueRetriever r, double m)
		{
			delegate = r;
			mean = m;
		}

		public double getValue(AveragedRow row, int place)
		{
			double x = delegate.getValue(row, place) - mean;
			return x*x;
		}
	}

  /**
   *
   */
	class BellLatenessRetriever implements BellValueRetriever
	{
		public double getValue(AveragedRow row, int place)
		{
			return row.getLatenessMilliseconds(place);
		}
	}

  /**
   *
   */
	class RowStrikingVarianceRetriever implements RowValueRetriever
	{
		public double getValue(AveragedRow row)
		{
      return row.getVariance();
		}
	}

	/**
	 *
	 */
	class RowDiscreteVarianceRetriever implements RowValueRetriever
	{
		public double getValue(AveragedRow row)
		{
			return row.getDiscreteVariance();
		}
	}

	/**
	 *
	 */
	class RowDurationRetriever implements RowValueRetriever
	{
		public double getValue(AveragedRow row)
		{
			return row.getRowDuration();
		}
	}

	/**
	 * Should only be used on backstrokes
	 */
	class WholePullDurationRetriever implements RowValueRetriever
	{
		public double getValue(AveragedRow row)
		{
			return row.getWholePullDuration();
		}
	}

	/**
	 *
	 */
	class InterbellGapRetriever implements RowValueRetriever
	{
		public double getValue(AveragedRow row)
		{
			return row.getMeanInterbellGap();
		}
	}

	/**
	 * Should only be used on handstrokes
	 */
	class HandstrokeGapRetriever implements RowValueRetriever
	{
		public double getValue(AveragedRow row)
		{
			return row.getHandstrokeGapMs();
		}
	}

}
