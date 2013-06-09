package org.pealfactory.strike.input;

import org.pealfactory.strike.data.Bong;
import org.pealfactory.strike.errorcorrection.*;
import org.pealfactory.strike.Constants;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Format of a Lowndes file:
 * <p>
 * The format of each line is the ASCII string "s b 0xTTTT", where s is the handstroke or backstroke
 * flag "H" or "B"; b is the bell number 1-0, E, T; and 0xTTTT is a 16-bit hex number giving the time
 * in milliseconds.
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
public class LowndesBongInput extends BongInputHelper
{
	private int fLastTime;
	private int fHighTime;
	private int fTimesHeardTreble = 0;
	private boolean fSeenFirstBong = false;

	public LowndesBongInput(String filename, Reader input)
	{
		super(filename, input);
		fLastTime = 0;
		fHighTime = 0;
	}

	public String getInputFormat()
	{
		return "Lowndes";
	}

	public static boolean isMyType(String line)
	{
		if (isComment(line))
			return false;
		if (line.length()==10 && line.startsWith("H ") || line.startsWith("B "))
			return true;
		return false;
	}

	public List<ErrorCorrecter> getErrorCorrecters()
	{
		List<ErrorCorrecter> errorCorrectors = new ArrayList();
		// Lownes data is typically coming either from unreliable light sensors on the bells, or equally unreliable
		// "Hawkear" audio analysis. In either case, we probably don't have handstroke/backstroke data, and there
		// may be sensor artifacts and erros we need to clean up. To deal with these problems, we add lots of
		// error conversion layers, as follows:
		// First, remove any sensor echoes - basically, double strikes of bells.
		errorCorrectors.add(new SensorEchoCorrecter());
		// We can still have rogue extra strikes of bells in the middle of the change; try and get rid of these next.
		errorCorrectors.add(new ExtraneousStrikeCorrector());
		// Up to now the data has been in time-sorted order, however now we attempt to correct "row overlaps", where
		// one bell has struck so late it is in the next change, or vice versa. We want the bells in the same row
		// all together, even if this breaks strike time order.
		errorCorrectors.add(new RowOverlapCorrector());
		// Finally we can look at assigning correct hand/back flags.
		errorCorrectors.add(new StrokeCorrecter());
		// But do a final pass to cope with missing or misaligned data, causing a bell to be treated as ringing at the end
		// of a row when it should have been at the other stroke at the start of the next. This also tries to deal with
		// recording where we come in halfway through a change.
		errorCorrectors.add(new LeadLieCorrector());
		return errorCorrectors;
	}

	protected void processLine(String line)
	{
		if (line.length()!=10)
		{
			fInputListener.notifyInputError("Format error in Lowndes file - line unexpected length: "+line);
			return;
		}
		// Although Lowndes files contain a handstroke indicator, in fact they provide no indication of stroke,
		// since the data has generally come from audio transcription where the stroke is not discernable.
		// Hence, although we read and check the information, we do not currently use it in the creation of the Bong.
		int stroke = readStrokeCharacter(line.charAt(0));

		int b = readBellCharacter(line.charAt(2));
		if (b<=0)
			return;
		if (b>fNBells)
			fNBells = b;

		int t = 0;
		try
		{
			t = Integer.parseInt(line.substring(6,10), 16);
		}
		catch (NumberFormatException e)
		{
			fInputListener.notifyInputError("Format error in Lowndes file - bad hex time: "+line);
			return;
		}

		if (t<fLastTime)
			fHighTime+= 0x10000;
		fLastTime = t;

		Bong bong = new Bong(b, t+fHighTime, Bong.UNKNOWNSTROKE);
		fInputListener.receiveBong(bong);
		fSeenFirstBong = true;
	}
}
