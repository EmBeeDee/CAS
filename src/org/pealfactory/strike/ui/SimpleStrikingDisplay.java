package org.pealfactory.strike.ui;

import org.pealfactory.strike.data.TouchStats;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Non-scrolling base class for striking displays and striking summary displays.
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
public abstract class SimpleStrikingDisplay extends JComponent implements MouseListener, PrintableUI
{
	protected boolean fRowsLoaded;
	protected TouchStats fData;
	protected boolean fInChangesOnly = true;

	/** Audio playback */
	protected int fPlaybackStartRow = -1;
	protected int fPlaybackEndRow = -1;
	protected int fNowPlayingRow = -1;

	public SimpleStrikingDisplay()
	{
		fRowsLoaded = false;
	}

	protected abstract void repaintRow(int row);

	/**
	 * Returns a Runnable which does the work of updating the display, and which the
	 * caller must run on the event thread.
	 *
	 * @param data
	 * @return
	 */
	public final Runnable loadRows(TouchStats data)
	{
		if (data.getNBells()==0)
			return null;
		return doLoadRows(data);
	}

	public int getTotalWidth()
	{
		return getPreferredSize().width;
	}

	protected Runnable doLoadRows(final TouchStats data)
	{
		return new Runnable(){
			public void run()
			{
				fData = data;
				fRowsLoaded = true;
				updateDisplay();
			}
		};
	}

	public void setInChangesOnly(boolean inChangesOnly)
	{
		if (fInChangesOnly!=inChangesOnly)
		{
			fInChangesOnly = inChangesOnly;
			updateDisplayOnEventThread();
		}
	}

	public void updateDisplay()
	{
		revalidate();
		repaint(new Rectangle(new Point(0,0), getPreferredSize()));
	}

	protected void updateDisplayOnEventThread()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				updateDisplay();
			}
		});
	}

	public void setPlayingRow(final int row)
	{
		if (row!=fNowPlayingRow)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (fNowPlayingRow>=0)
						repaintRow(fNowPlayingRow);
					fNowPlayingRow = row;
					if (fNowPlayingRow>=0)
						repaintRow(fNowPlayingRow);
				}
			});
		}
	}

	public int getPlaybackStartRow()
	{
		return fPlaybackStartRow;
	}

	public int getPlaybackEndRow()
	{
		return fPlaybackEndRow;
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	protected int round(float f)
	{
		return (int)(0.5+f);
	}

	protected int round(double d)
	{
		return (int)(0.5+d);
	}
}
