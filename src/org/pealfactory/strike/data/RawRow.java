package org.pealfactory.strike.data;

import org.pealfactory.strike.*;

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
public class RawRow implements Row, Iterable<Bong>
{
	private boolean fHandstroke;
	private List<Bong> fBells;
	private int fNBells = 0;

  public RawRow(boolean stroke)
	{
		fHandstroke = stroke;
		fBells = new ArrayList();
	}

	public String toString()
	{
		StringBuffer s = new StringBuffer();
		if (fBells.size()==0)
			s.append("Empty Row");
		else
		{
			s.append(fBells.get(0).toString());
			for (int i=1; i<fBells.size(); i++)
			{
				s.append(", ");
				s.append(fBells.get(i).toString());
			}
		}
		return s.toString();
	}

	public String rowAsString()
	{
		StringBuffer s = new StringBuffer();
		if (fBells.size()==0)
			s.append("Empty Row");
		else
		{
			for (int i=0; i<fBells.size(); i++)
			{
				Bong bong = fBells.get(i);
				s.append(Constants.BELL_CHARS.charAt(bong.bell-1));
				// Add a stroke indicator if this bell has a different stroke to the row!
				if ((bong.stroke==Bong.HANDSTROKE)!=fHandstroke)
					s.append(bong.stroke==Bong.HANDSTROKE? "h": "b");
			}
		}
		return s.toString();
	}

	@Override
	public Iterator<Bong> iterator()
	{
		return fBells.iterator();
	}

	/**
	 * @param place 1..n
	 * @return
	 */
	public Bong getBong(int place)
	{
		return fBells.get(place-1);
	}

	public int getBellAt(int place)
	{
		return getBong(place).bell;
	}

	public Bong getLastBong()
	{
		return fBells.get(getRowSize()-1);
	}

	public Bong removeBong(int place)
	{
		return fBells.remove(place-1);
	}

	public Bong removeLastBong()
	{
		return fBells.remove(getRowSize()-1);
	}

	public int getStrikeTime(int place)
	{
		return getBong(place).time;
	}

	public int getFirstStrikeTime()
	{
		return getStrikeTime(1);
	}

	public int getLastStrikeTime()
	{
		return getStrikeTime(getRowSize());
	}

	public int findBell(int bell)
	{
		for (int place=1; place<=fBells.size(); place++)
		{
			if (getBellAt(place)==bell)
				return place;
		}
		return -1;
	}

	public boolean isHandstroke()
	{
		return fHandstroke;
	}
	
	public boolean isMatchingStroke(Bong bong)
	{
		if (isHandstroke())
			return bong.stroke==Bong.HANDSTROKE;
		else
			return bong.stroke==Bong.BACKSTROKE;
	}

	public void setHandstroke(boolean handstroke)
	{
		fHandstroke = handstroke;
	}

	/**
	 * Adds a new bong to the end of the row.
	 *
	 * @param bong
	 */
	public void addBong(Bong bong)
	{
		fBells.add(bong);
		fNBells = Math.max(fNBells, bong.bell);
	}

	/**
	 * Adds a new bong to the start of the row.
	 *
	 * @param bong
	 */
	public void addBongAtLead(Bong bong)
	{
		fBells.add(0, bong);
		fNBells = Math.max(fNBells, bong.bell);
	}

	public void setBells(Bong[] bells, int first, int last)
	{
		for (int i=first; i<=last; i++)
			fBells.add(bells[i]);
	}

	public int getNBells()
	{
		return fNBells;
	}

	public int getRowSize()
	{
		return fBells.size();
	}

	/**
	 * A row is "close" to rounds if all bells strike in increasing order of size, or if occasional pairs
	 * are swapped but are close to each other in time - say up to 90ms apart.
	 * Note that this new algorithm works even if bells are missing from the change completely.
	 *
	 * @return
	 */
	public boolean isCloseToRounds()
	{
		int b1 = getBellAt(1);
		for (int i=2; i<=getRowSize(); i++)
		{
			int b2 = getBellAt(i);
			if (b1>b2)
			{
				// Allow two bells to be swapped if they are close to each other - adjacent bell numbers,
				// and say up to 90ms apart.
				if (b1-b2>1 || getBong(i).time-getBong(i-1).time > 90)
						return false;
			}
			b1 = b2;
		}
		return true;
	}

	/**
	 * Returns the time difference between the last and first bells in the row (so does not include the inter-row gap).
	 *
	 * @return
	 */
	public int getRowDuration()
	{
		return getBong(getRowSize()).time-getBong(1).time;
	}

}
