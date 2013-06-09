package org.pealfactory.strike.ui;

import org.pealfactory.strike.data.TouchStats;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;

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
public class StrikingSummaryDisplay extends SimpleStrikingDisplay
{
	protected void repaintRow(int row)
	{
	}

	protected Runnable loadRows(TouchStats data, int totalWidth)
	{
		return null;
	}

	@Override
	public int pagesToPrint(CASPageFormat pf)
	{
		return 0;
	}

	@Override
	public void print(Graphics2D g2d, CASPageFormat pf, int page) throws PrinterException
	{
	}
}
