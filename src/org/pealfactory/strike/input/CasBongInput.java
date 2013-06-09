package org.pealfactory.strike.input;

import org.pealfactory.strike.data.*;
import org.pealfactory.strike.errorcorrection.*;

import java.io.*;
import java.util.*;

/**
 * Format of a CAS file:
 * <ul>
 *  <li>The first line starts with the letters "CAS" (can optionally be followed by anything, e.g. file title)</li>
 *  <li>Subsequent lines contain entire rows, prefixed with alternating H/B characters</li>
 *  <li>Along each row the bells are listed in the format b ttt, where b is the bell number 1-0, E, T, and ttt
 *  is a non-negative decimal integer giving the interval in milliseconds between this and the previous bell.</li>
 *  <li>Comment lines start with # or *</li>
 * </ul>
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
public class CasBongInput extends BongInputHelper
{
	private static final String FORMAT_STRING = "CAS";
	
	private int fTimestamp = 0;
	
	public CasBongInput(String filename, Reader input)
	{
		super(filename, input);
	}

	@Override
	public String getInputFormat()
	{
		return FORMAT_STRING;
	}

	public static boolean isMyType(String line)
	{
		if (line.startsWith(FORMAT_STRING))
			return true;
		return false;
	}

	@Override
	public List<ErrorCorrecter> getErrorCorrecters()
	{
		//return Collections.emptyList();
		return getTestErrorCorrectors();
	}
	
	private List<ErrorCorrecter> getTestErrorCorrectors()
	{
		List<ErrorCorrecter> errorCorrectors = new ArrayList();
		errorCorrectors.add(new TimeOrderCorrecter(5000));
		errorCorrectors.add(new SensorEchoCorrecter());
		errorCorrectors.add(new ExtraneousStrikeCorrector());
		errorCorrectors.add(new RowOverlapCorrector());
		errorCorrectors.add(new StrokeCorrecter());
		errorCorrectors.add(new LeadLieCorrector());
		return errorCorrectors;
	}

	@Override
	protected void processLine(String line)
	{
		if (line.startsWith(FORMAT_STRING))
			return;
		StringTokenizer tok = new StringTokenizer(line, " ");
		if (!tok.hasMoreTokens())
			return;
		
		int stroke = readStrokeCharacter(tok.nextToken().charAt(0));
		if (stroke==Bong.UNKNOWNSTROKE)
			return;
		
		while (tok.hasMoreTokens())
		{
			int b = readBellCharacter(tok.nextToken().charAt(0));
			if (b<=0)
				return;
			if (!tok.hasMoreTokens())
				fInputListener.notifyInputError("Format error in CAS file - no time delta for bell: "+b);
			String timeString = tok.nextToken();
			int t;
			try
			{
				t = Integer.parseInt(timeString);
			}
			catch (NumberFormatException e)
			{
				fInputListener.notifyInputError("Format error in CAS file - bad time delta : "+timeString);
				return;
			}
			fTimestamp+= t;
			Bong bong = new Bong(b, fTimestamp, stroke);
			fInputListener.receiveBong(bong);
		}
	}
	
	public static void outputRowData(TouchStats rowData, PrintWriter out)
	{
		int timestamp = 0;
		boolean start = true;
		for (int i=0; i<rowData.getNRows(); i++)
		{
			AveragedRow row = rowData.getRow(i);
			StringBuilder buf = new StringBuilder();
			if (row.isHandstroke())
				buf.append("H ");
			else
				buf.append("B ");
			Bong bong = null;
			for (int j=0; j<row.getRowSize(); j++)
			{
				bong = row.getBong(j+1);
				buf.append(BELL_CHARS.charAt(bong.bell-1));
				buf.append(" ");
				if (start)
				{
					timestamp = bong.time;
					start = false;
				}
				buf.append(bong.time-timestamp);
				timestamp = bong.time;
				buf.append(" ");
			}
			out.println(buf.toString());
		}
	}
}
