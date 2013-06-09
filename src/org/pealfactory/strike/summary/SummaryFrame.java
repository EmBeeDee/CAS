package org.pealfactory.strike.summary;

import org.pealfactory.strike.*;
import org.pealfactory.strike.ui.*;

import javax.swing.*;
import java.awt.event.*;

/**
 * Frame providing a summary of a number of bands (all open CAS windows)
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
 */
public class SummaryFrame extends JFrame implements SummaryContainer
{
	private SummaryWindow fWindow;

  public SummaryFrame(String title, SummaryWindow window)
	{
		super(title);
		fWindow = window;
	}

	public void open()
	{
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e)
			{
				fWindow.close();
			}
		});
    fWindow.open(this);
		pack();
		setVisible(true);
	}

	@Override
	public void openNewCASWindow()
	{
		CASFrame.openNewFrame(CAS.DEFAULT_FRAME_TITLE, null, null);
	}
}
