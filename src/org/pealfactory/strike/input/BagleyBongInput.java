package org.pealfactory.strike.input;

import org.pealfactory.strike.data.Bong;
import org.pealfactory.strike.Constants;
import org.pealfactory.strike.errorcorrection.*;
import org.pealfactory.strike.pipeline.*;

import java.io.*;
import java.util.*;

/**
 * Format of a Bagley file:
 * <p>
 * 'The format at the moment is the ASCII string "bmmmmmmmm<CR><LF>" where b is
 * the bell number (1234567890E or T), and mmmmmmmm is miliseconds since start.'
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
 * @author MBD
 */
public class BagleyBongInput extends BongInputHelper
{
	public final static String ODDSTRUCKFILE = "oddstruck.txt";

	/** Largest time interval between two uncorrected bell times that could still see the two bell striking simultaneously. */
	public final static int MAXSEQUENCEERROR = 500;

	private final static boolean DROP6DOUBLE = true;

	private Reader fOddstrucknessData;
	int[] fHandOddstruck = new int[MAXNBELLS];
	int[] fBackOddstruck = new int[MAXNBELLS];
	Bong[] fPreviousBongs = new Bong[MAXNBELLS];
	int fCount6th = 0;

	public BagleyBongInput(String filename, Reader bdcFile, Reader oddstruckFile)
	{
		super(filename, bdcFile);
		fOddstrucknessData = oddstruckFile;
	}

	public String getInputFormat()
	{
		return "Bagley";
	}

	public static boolean isMyType(String line)
	{
		if (isComment(line))
			return false;
		if (line.length()==9 && BELL_CHARS.indexOf(line.charAt(0))>=0)
		{
			int i;
			for (i=1; i<9; i++)
				if (!Character.isDigit(line.charAt(i)))
					break;
			if (i>=9)
				return true;
		}
		return false;
	}

	public List<ErrorCorrecter> getErrorCorrecters()
	{
		List<ErrorCorrecter> errorCorrectors = new ArrayList();
		errorCorrectors.add(new TimeOrderCorrecter(MAXSEQUENCEERROR));
		errorCorrectors.add(new SensorEchoCorrecter());
		errorCorrectors.add(new ExtraneousStrikeCorrector());
		return errorCorrectors;
	}

	protected void processLines()
	{
		if (!processOddstruckFile())
			return;
		super.processLines();
	}

	private boolean processOddstruckFile()
	{
		LineNumberReader reader = null;
		try
		{
			reader = new LineNumberReader(fOddstrucknessData);
			doProcessOddstruckFile(reader);
		}
		catch (Exception e)
		{
			fInputListener.notifyInputError("Failed to read oddstruckness file: "+e);
			fClosed = true;
			return false;
		}
		finally
		{
			closeFile(reader);
		}
		return true;
	}

	private void closeFile(Reader r)
	{
		if (r!=null)
		{
			try
			{
				r.close();
			}
			catch (IOException e)
			{
				// Ignore
			}
		}
	}

	private void doProcessOddstruckFile(LineNumberReader reader) throws IOException
	{
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (int i=0; i<fHandOddstruck.length; i++)
		{
			fHandOddstruck[i] = 0;
			fBackOddstruck[i] = 0;
		}

		String line = reader.readLine();
		int i = 0;
		while (line!=null)
		{
			line = line.trim();
			if (line.length()>0)
			{
				StringTokenizer tok = new StringTokenizer(line, ", \t");
				int h = Integer.parseInt(tok.nextToken());
				int b = Integer.parseInt(tok.nextToken());
				min = h<min? h:min;
				min = b<min? b:min;
				max = h>max? h:max;
				max = b>max? b:max;
				fHandOddstruck[i] = h;
				fBackOddstruck[i] = b;
				i++;
			}
			line = reader.readLine();
		}
		if (max-min>MAXSEQUENCEERROR)
			throw new IOException("Difference between min and max oddstruckness values too great: "+(max-min));
	}

	protected void processLine(String line)
	{
		if (line.length()!=9)
		{
			fInputListener.notifyInputError("Format error in Bagley file - line unexpected length: "+line);
			return;
		}
		int b = readBellCharacter(line.charAt(0));
		if (b<=0)
			return;
		if (b>fNBells)
			fNBells = b;

		// Only take every 1st and 4th occurrences of the 6th - drop the 2nd and 3rd, which are sensor ghosts.
		if (DROP6DOUBLE && b==6)
		{
			fCount6th++;
			if (fCount6th>3)
				fCount6th = 0;
			else if (fCount6th>1)
				return;
		}

		int t = 0;
		try
		{
			t = Integer.parseInt(line.substring(1,9));
		}
		catch (NumberFormatException e)
		{
			fInputListener.notifyInputError("Format error in Bagley file - bad time character: "+line);
			return;
		}

		// Problem with Bagley files! The input doesn't have hand/back data, but we can't use an error corrector
		// to infer this, since we need to know which stroke before applying "oddstruck correction" to the strike time.
		// So, make a best guess at strokes here.
		// TODO Really need to do something better if we are ever going to use Bagley input again; for instance,
		// we could apply the handstroke oddstruckness to all generated Bongs, and use a Bagley-specific error corrector
		// to apply any extra backstroke oddstruckness after the other error correctors have run. The fact that backstroke
		// timings are slightly out throughout the main error-correcting phase hopefully wouldn't matter too much.
		int stroke = Bong.HANDSTROKE;
		Bong prevBong = fPreviousBongs[b-1];
		if (prevBong!=null)
		{
			if (t-prevBong.time<QUICKEST_STRIKE_TIME)
				return;
			stroke = -prevBong.stroke;
		}
		if (stroke==Bong.HANDSTROKE)
			t+= fHandOddstruck[b-1];
		else
			t+= fBackOddstruck[b-1];
		Bong bong = new Bong(b, t, stroke);
		fInputListener.receiveBong(bong);
		fPreviousBongs[b-1] = bong;
	}

}
