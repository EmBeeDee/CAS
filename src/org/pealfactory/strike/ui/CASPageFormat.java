package org.pealfactory.strike.ui;

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
 */
public class CASPageFormat
{
	/** java.awt PageFormat to be used for printing every page */
	protected PageFormat pageFormat;
	/** Height of the header on every page */
	protected int headerHeight;
	/** Height of the footer on every page */
	protected int footerHeight;
	/** Extra vertical space reserved on the first page, before normal printing starts */
	protected int initialReservedHeight;

	public CASPageFormat(PageFormat pageFormat, int headerHeight, int footerHeight, int initialReservedHeight)
	{
		this.pageFormat = pageFormat;
		this.headerHeight = headerHeight;
		this.footerHeight = footerHeight;
		this.initialReservedHeight = initialReservedHeight;
	}
}
