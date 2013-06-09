package org.pealfactory.strike.analyser;

import org.pealfactory.strike.data.Row;

/**
 *
 * @author MBD
 */
public class RodVisualiser2 extends RodBaseVisualiser
{
	public final static String NAME = "RodModel2";
	public final static String INFO = "The RodModel2 visualiser calculates the desired length of a whole pull, minus handstroke gap, by averaging the difference between the midpoint of the bells striking in the next whole pull and that of the previous whole pull. Handstroke gap is the average gap for the ringing so far.";

	private Row fLastRow;
	private double fTotalHandstrokeGap;
	private double fTotalInterbellGap;
	private double fCurrentHandstrokeGap;

	public RodVisualiser2()
	{
		super(NAME, INFO);
	}

	public void clearData()
	{
		super.clearData();
		fLastRow = null;
		fTotalHandstrokeGap = 0.0;
		fTotalInterbellGap = 0.0;
		fCurrentHandstrokeGap = 0.0;
	}


	protected double getCurrentHandstrokeGap()
	{
		return fCurrentHandstrokeGap;
	}

	protected void newRow(Row row)
	{
		if (row.getRowSize()>1)
			fTotalInterbellGap+= (row.getBong(row.getRowSize()).time-row.getBong(1).time)/(row.getRowSize()-1);
		if (row.isHandstroke() && fLastRow!=null)
		{
      fTotalHandstrokeGap+= row.getStrikeTime(1)-fLastRow.getStrikeTime(fLastRow.getRowSize());
			fCurrentHandstrokeGap = fTotalHandstrokeGap/fTotalInterbellGap;
		}
		fLastRow = row;
		super.newRow(row);
	}
}
