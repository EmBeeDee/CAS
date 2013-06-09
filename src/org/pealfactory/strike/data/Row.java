package org.pealfactory.strike.data;

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
public interface Row
{
	/**
	 * @param place 1..n
	 * @return
	 */
	public Bong getBong(int place);

	/**
	 * @param place 1..n
	 * @return
	 */
	public int getBellAt(int place);

	/**
	 * @param place 1..n
	 * @return
	 */
	public int getStrikeTime(int place);

	/**
	 * @param bell 1..n
	 * @return
	 */
	public int findBell(int bell);

  /**
   *
   */
	public boolean isHandstroke();

  /**
   * @return number of bells
   */
	public int getNBells();

	/**
	 * @return number of bells striking in row
	 */
	public int getRowSize();

	/**
	 * Return true if it looks like the band was trying to strike rounds in this change.
	 *
	 * @return
	 */
	public boolean isCloseToRounds();

}
