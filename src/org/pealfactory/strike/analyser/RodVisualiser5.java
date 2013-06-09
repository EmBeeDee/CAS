package org.pealfactory.strike.analyser;

import org.pealfactory.strike.data.*;

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
public class RodVisualiser5 extends RodBaseVisualiser
{
	public final static String NAME = "RodModel5";
	public final static String INFO = "The RodModel5 visualiser calculates the desired length of a whole pull, minus handstroke gap, by averaging the difference between the midpoint of the bells striking in the next whole pull and that of the previous whole pull. Handstroke gap is a constant ";

	/** As a proportion of inter-bell gap, i.e. 1.0 is nominal. */
	private double fHandstrokeGap;

	public RodVisualiser5(double handstrokeGap)
	{
		super(NAME, INFO+handstrokeGap+".");
		fHandstrokeGap = handstrokeGap;
	}

	protected double getCurrentHandstrokeGap()
	{
		return fHandstrokeGap;
	}

}
