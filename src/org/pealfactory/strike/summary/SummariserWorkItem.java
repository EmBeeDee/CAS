package org.pealfactory.strike.summary;

import org.pealfactory.strike.analyser.*;
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
 */
public class SummariserWorkItem implements AnalysisStageListener
{
	private char fBandNumber;
	private Visualiser fVisualiser;
	private double fResult;

	public SummariserWorkItem(char bandNumber, Visualiser visualiser)
	{
		fBandNumber = bandNumber;
		fVisualiser = visualiser;
	}

	/**
	 * Should be run on a worker thread.
	 *
	 * @param summariser
	 */
	public void summarize(Summariser summariser)
	{
		reloadVisualiserData(summariser);
		fResult = fVisualiser.getAveragedTouchData().getStrikingRMSE(summariser.isInChangesOnly()).whole;
	}

	protected void reloadVisualiserData(Summariser summariser)
	{
		RowSource rawTouchData = summariser.getTouchData(fBandNumber);
		fVisualiser.clearData();
		fVisualiser.setAnalysisListener(this);
		fVisualiser.newRowsAvailable(rawTouchData);
		fVisualiser.notifyLastRowRung();
	}

	public double getResult()
	{
		return fResult;
	}

	protected void setResult(double result)
	{
		fResult = result;
	}

	public char getBandNumber()
	{
		return fBandNumber;
	}

	public String getAnalysisName()
	{
		return fVisualiser.getName();
	}

	protected Visualiser getVisualiser()
	{
		return fVisualiser;
	}

	@Override
	public void analysisComplete()
	{
		// No-op
	}

	@Override
	public void newAveragedRowAvailable()
	{
		// No-op
	}
}

