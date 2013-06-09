package org.pealfactory.strike.ui;

import org.pealfactory.strike.data.AveragedRowData;
import org.pealfactory.strike.data.TouchStats;
import org.pealfactory.strike.Constants;
import org.pealfactory.strike.CAS;
import org.pealfactory.strike.input.InputFactory;
import org.pealfactory.strike.input.StrikingDataInput;
import org.pealfactory.strike.pipeline.Pipeline;
import org.pealfactory.strike.analyser.*;
import org.pealfactory.strike.audio.ReplayThread;
import org.pealfactory.strike.audio.PlaybackController;
import org.pealfactory.strike.summary.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.URL;
import java.io.*;
import java.text.NumberFormat;

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
public class CASFrame extends JFrame implements CASContainer
{
	private CASWindow fWindow;
	private static File gExportDirectory = null;
	private static int gWindowOpenedCount = 0;
	private static Object gWindowOpenLock = new Object();

  public CASFrame(String title, CASWindow window)
	{
		super(title);
		fWindow = window;
	}

	public static void openNewFrame(String title, final String fileToOpen, Point screenPos)
	{
		final CASWindow window = CAS.createNewCASWindow();
		CASFrame frame = new CASFrame(title, window);

		if (screenPos!=null)
		{
			frame.setLocation(screenPos);
		}
		else
		{
			synchronized (gWindowOpenLock)
			{
				gWindowOpenedCount = 0;
			}
		}

		frame.open();

		if (fileToOpen!=null)
		{
			SwingUtilities.invokeLater(new Runnable(){
				public void run()
				{
					try
					{
						File homeDir = CAS.getHomeDirectory();
						File f = new File(homeDir, fileToOpen);
						window.loadFile(f.getPath());
					}
					catch (IOException e)
					{
						System.out.println("ERROR: failed to load file: "+e);
					}
				}
			});
		}
	}

	public void open()
	{
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e)
			{
				closeWindow();
			}
		});
    fWindow.open(this);
		pack();
		setVisible(true);
	}

	public void openFile()
	{
		File homeDirectory = CAS.getHomeDirectory();
		JFileChooser fc = new JFileChooser(homeDirectory);
		if (fc.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION)
		{
			File f = fc.getSelectedFile();
			homeDirectory = f.getParentFile();
			try
			{
				fWindow.loadFile(f.getPath());
				CAS.setHomeDirectory(homeDirectory);
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(getContentPane(), ex.getMessage(), "Failed to open file", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void openNewCASWindow()
	{
		openNewCASWindow(null);
	}

	public void openNewCASWindow(String fileToLoad)
	{
		CASFrame.openNewFrame(getTitle(), fileToLoad, getNextWindowLocation());
	}

	private Point getNextWindowLocation()
	{
		Point p = getLocationOnScreen();
		synchronized (gWindowOpenLock)
		{
			gWindowOpenedCount++;
			if (gWindowOpenedCount>10)
				gWindowOpenedCount = 1;
			p.x+= 30*gWindowOpenedCount;
			p.y+= 20*gWindowOpenedCount;
		}
		return p;
	}

	public void openSummaryWindow()
	{
		SummaryWindow window = new SummaryWindow();
		SummaryFrame frame = new SummaryFrame("CAS Band Summary", window);
		frame.setLocation(getNextWindowLocation());
		frame.open();
	}

	public Reader getReader(String name) throws FileNotFoundException
	{
		return new FileReader(name);
	}

	public void export()
	{
		File homeDirectory = CAS.getHomeDirectory();
		if (gExportDirectory==null)
			gExportDirectory = homeDirectory;
		JFileChooser fc = new JFileChooser(gExportDirectory);
		if (fc.showSaveDialog(getContentPane()) == JFileChooser.APPROVE_OPTION)
		{
			File f = fc.getSelectedFile();
			gExportDirectory = f.getParentFile();
			try
			{
				PrintWriter writer = new PrintWriter(new FileWriter(f));
				fWindow.saveFile(writer);
				//fWindow.exportStats(writer);
				writer.close();
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(getContentPane(), ex.getMessage(), "Failed to save file", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	@Override
	public void closeWindow()
	{
		dispose();
		fWindow.close();
	}
}
