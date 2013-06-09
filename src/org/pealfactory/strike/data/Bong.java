package org.pealfactory.strike.data;

import org.pealfactory.strike.Constants;

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
public class Bong implements Constants
{
	public static final int HANDSTROKE = -1;
	public static final int BACKSTROKE = +1;
	public static final int UNKNOWNSTROKE = 0;

	/** Bell number 1 - 16 */
	public int bell;
	/** Strike time in milliseconds */
	public int time;
	/** -1 handstroke, +1 for backstroke, 0 for not known. */
	public int stroke;

	public Bong(int b, int t, int s)
	{
		bell = b;
		time = t;
		stroke = s;
	}

	public Bong(Bong bong)
	{
		this(bong.bell, bong.time, bong.stroke);
	}

	/**
	 * Returns the same Bong, with the stroke swapped.
	 * @return
	 */
	public Bong swapStroke()
	{
		stroke = -stroke;
		return this;
	}

	/**
	 * Two Bongs are equal if they have the same bell - don't care about the timestamp.
	 * @param o
	 * @return
	 */
	public boolean equals(Object o)
	{
		if (o instanceof Bong)
		{
			Bong b = (Bong) o;
			if (bell==b.bell)
				return true;
		}
		return false;
	}

	public int compareTo(Bong b)
	{
		if (time<b.time)
			return -1;
		if (time>b.time)
			return +1;
		return 0;
	}

	public String toString()
	{
		return bell+" "+time+" "+(stroke==HANDSTROKE ? "H": (stroke==BACKSTROKE ? "B" : ""));
	}

	public static Bong fromString(String s)
	{
		int i = s.indexOf(" ");
		if (i<0)
			throw new IllegalArgumentException("Bad bong: "+s);
		String b = s.substring(0, i).trim();
		String t = s.substring(i+1).trim();
		int stroke = UNKNOWNSTROKE;
		int j = s.indexOf(" ", i+1);
		if (j>0)
		{
			t = s.substring(i+1, j).trim();
			if (s.substring(j+1).equals("H"))
				stroke = HANDSTROKE;
			else if (s.substring(j+1).equals("B"))
				stroke = BACKSTROKE;
		}
		return new Bong(Integer.parseInt(b), Integer.parseInt(t), stroke);
	}
}
