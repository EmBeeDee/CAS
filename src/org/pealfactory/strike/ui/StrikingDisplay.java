package org.pealfactory.strike.ui;

import org.pealfactory.strike.data.AveragedRowData;
import org.pealfactory.strike.data.TouchStats;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

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
public abstract class StrikingDisplay extends SimpleStrikingDisplay implements Scrollable, MouseListener
{
	protected float fZoomX = 1.0f;
	protected float fZoomY = 1.0f;
	protected float fZoomFont = 1.0f;

	protected int fWidthPerBell = 100;
	protected int fHeightPerRow = 20;
	protected boolean fAdvancedView = false;

	protected int fHighlightedBell;
	protected int fNowPlayingBell = -1;

	private boolean fAutoScroll;
	private Rectangle fPreviousScrollPosition;

	public StrikingDisplay()
	{
		fRowsLoaded = false;
		addMouseListener(this);
	}

	protected abstract void scrollToRow(int row);

	public void setZoom(float zoom)
	{
		fZoomFont = (float)Math.pow((double)zoom, 0.4);
		fZoomX = (float)Math.pow((double)zoom, 0.2);
		fZoomY = zoom;
		if (fData==null)
			fWidthPerBell = zoomX(100);
		else
			fWidthPerBell = round(fZoomX*getTotalWidth()/fData.getNBells());
		fHeightPerRow = zoomY(20);
	}

	protected int zoomX(int d)
	{
		return round(d*fZoomX);
	}

	protected int zoomY(int d)
	{
		return round(d*fZoomY);
	}

	public void setAutoScroll(boolean autoscroll)
	{
		fAutoScroll = autoscroll;
	}

	public void storePreviousScrollPosition()
	{
		fPreviousScrollPosition = getVisibleRect();
	}

	public void scrollToPreviousPosition()
	{
		if (fPreviousScrollPosition!=null)
			scrollRectToVisible(fPreviousScrollPosition);
	}

	protected Runnable doLoadRows(TouchStats data)
	{
		fWidthPerBell = round(fZoomX*getTotalWidth()/data.getNBells());
		return super.doLoadRows(data);
	}

	public void setAdvancedView(boolean advancedView)
	{
		if (fAdvancedView!=advancedView)
		{
			fAdvancedView = advancedView;
			updateDisplayOnEventThread();
		}
	}

	public int getHighlightedBell()
	{
		return fHighlightedBell;
	}

	public void setHighlightedBell(int highlightedBell)
	{
		fHighlightedBell = highlightedBell;
		repaint();
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
					{
						repaintRow(fNowPlayingRow);
						if (fAutoScroll)
							scrollToRow(fNowPlayingRow);
					}
				}
			});
		}
	}

	public void setPlayingBell(final int bell)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				fNowPlayingBell = bell;
				if (fNowPlayingRow>=0)
					repaintRow(fNowPlayingRow);
			}
		});
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		if (orientation==SwingConstants.HORIZONTAL)
			return fWidthPerBell;
		return fHeightPerRow;
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		if (orientation==SwingConstants.HORIZONTAL)
			return 100;
		return 100;
	}

	public boolean getScrollableTracksViewportWidth()
	{
		return false;
	}

	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}

}
