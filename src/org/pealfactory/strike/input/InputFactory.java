package org.pealfactory.strike.input;

import org.pealfactory.strike.Constants;

import java.io.*;
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
public class InputFactory implements Constants
{
	public final static String COMMENT_CHARS = "*#";

	public static boolean isComment(String line)
	{
		if (line.length()==0 || COMMENT_CHARS.indexOf(line.charAt(0))>=0)
			return true;
		return false;
	}

	public InputFactory()
	{
	}

	public StrikingDataInput createInputter(String filename, InputSource source) throws IOException
	{
		LineNumberReader reader = new LineNumberReader(source.getReader(filename));
		StrikingDataInput inputter = null;
		// Work out the file type from the first non-comment line of input.
		String line;
		int bytesLeft = 10000;
		reader.mark(bytesLeft);
		bytesLeft/= 2;
		while (inputter==null && bytesLeft>0)
		{
			line = reader.readLine();
			if (line==null)
				throw new IOException("File is empty");
			bytesLeft-= line.length();
			line = line.trim();

			if (CasBongInput.isMyType(line))
				inputter = new CasBongInput(filename, reader);
			else if (LowndesBongInput.isMyType(line))
				inputter = new LowndesBongInput(filename, reader);
			else if (BagleyBongInput.isMyType(line))
				inputter = new BagleyBongInput(filename, reader, source.getReader(BagleyBongInput.ODDSTRUCKFILE));
		}
		reader.reset();
    if (inputter==null)
			throw new IOException("File is not in a recognised bong input format");
		return inputter;
	}

	public List readBandListFile(String filename) throws IOException
	{
		List bands = new ArrayList();
		LineNumberReader reader = new LineNumberReader(new FileReader(filename));
		String line = reader.readLine();
    while (line!=null)
		{
			if (!isComment(line))
				bands.add(line);
			line = reader.readLine();
		}
		reader.close();
		return bands;
	}
}
