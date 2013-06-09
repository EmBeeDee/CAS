package org.pealfactory.strike.input;

import org.pealfactory.strike.Constants;
import org.pealfactory.strike.data.*;

import java.io.*;

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
public abstract class BongInputHelper implements StrikingDataInput, Constants
{
	protected Reader fInputReader;
	protected String fFilename;
	protected int fNBells;
	protected InputStageListener fInputListener;
	protected boolean fClosed;

	protected BongInputHelper(String name, Reader input)
	{
		fInputReader = input;
		fFilename = name;
		fNBells = -1;
	}

	public static boolean isComment(String line)
	{
		return InputFactory.isComment(line);
	}

	public String getInputSource()
	{
		return "File";
	}

	public String getInputName()
	{
		return fFilename;
	}

	public int getNBells()
	{
		return fNBells;
	}

	public boolean isOpen()
	{
		return fInputListener!=null && !isClosed();
	}

	public boolean isClosed()
	{
		return fClosed;
	}

	public void startLoad(InputStageListener pipeline)
	{
		fInputListener = pipeline;
		processLines();
	}

	protected abstract void processLine(String line);

	protected void processLines()
	{
		LineNumberReader reader = null;
		try
		{
			reader = new LineNumberReader(fInputReader);
			String line = reader.readLine();
			while (line!=null)
			{
				line = line.trim();
				if (!isComment(line))
					processLine(line);
				line = reader.readLine();
			}
			fClosed = true;
			fInputListener.notifyInputComplete();
		}
		catch (IOException e)
		{
			fInputListener.notifyInputError("Failed to read input file: "+e);
			fClosed = true;
		}
		finally
		{
			if (reader!=null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					// Ignore
				}
			}
		}
	}

	protected int readStrokeCharacter(char stroke)
	{
		if (stroke=='H')
			return Bong.HANDSTROKE;
		if (stroke=='B')
			return Bong.BACKSTROKE;
		fInputListener.notifyInputError("Format error in "+getInputFormat()+" file - expecting stroke H or B but got: '"+stroke+"'");
		return Bong.UNKNOWNSTROKE;
	}

	/** Parses bell character 1234567890ETABCD into number 1-16, or returns 0 if character not recognised */
	protected int readBellCharacter(char bell)
	{
		if (bell=='O')
			bell = '0';
		int b = BELL_CHARS.indexOf(bell)+1;
		if (b<=0)
			fInputListener.notifyInputError("Format error in "+getInputFormat()+" file - bad bell character: "+bell);
		return b;
	}
}
