package org.pealfactory.strike;

import org.pealfactory.strike.analyser.*;
import org.pealfactory.strike.input.*;
import org.pealfactory.strike.ui.*;
import org.pealfactory.strike.audio.*;
import org.pealfactory.strike.pipeline.Pipeline;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

/**
 * CAS - Computer Analysis of Striking
 * <p>
 * Copyright 2003-2012 Mark B Davies
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
public class CAS extends JApplet implements CASContainer
{
	public final static double VERSION = 1.4;
	public final static boolean NOWORCESTER = false;
	public final static String APPLET_FILE_LIST = "files.lst";
	public final static String DEFAULT_FRAME_TITLE = "Computer Analysis of Striking";

	private CASWindow fWindow;

	private static File gHomeDirectory;

	public static void main(String[] args)
	{
		if (args==null || args.length>20)
		{
			System.out.println("Usage: CAS [file]");
			System.exit(1);
		}
		String toLoad = null;
		if (args.length>0)
			toLoad = args[0];
		try
		{
			//UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}
		catch (Exception e)
		{
			System.out.println("Unable to set Look and Feel: "+e);
		}
		CASFrame.openNewFrame(DEFAULT_FRAME_TITLE, toLoad, null);
	}

	public static synchronized File getHomeDirectory()
	{
		if (gHomeDirectory==null)
			gHomeDirectory = new File("peal.txt");
		return gHomeDirectory;
	}

	public static synchronized void setHomeDirectory(File homeDir)
	{
		gHomeDirectory = homeDir;
	}

	public static CASWindow createNewCASWindow()
	{
		CASWindow window = new CASWindow();

		List<Visualiser> visualisers = getAvailableVisualisers();
		for (Visualiser v: visualisers)
			window.addVisualiser(v);

		//PlaybackController audio = PlaybackController.getWavPlaybackController();
		PlaybackController audio = PlaybackController.getMidiPlaybackController();
		if (audio!=null)
     	window.setPlaybackController(audio);

		return window;
	}

	public static List<Visualiser> getAvailableVisualisers()
	{
		List<Visualiser> visualisers = new ArrayList<Visualiser>();

		if (!NOWORCESTER)
		{
			visualisers.add(new RodVisualiser2());
			visualisers.add(new RodVisualiser5(1.0));
		}
		visualisers.add(new SimpleAveragedRowVisualiser(1.0));
		visualisers.add(new LastBellPerfectVisualiser(1.0));
		visualisers.add(new SimpleLADVisualiser());

		return visualisers;
	}

	@Override
	public void init()
	{
		super.init();
    fWindow = CAS.createNewCASWindow();
	}

	@Override
	public void start()
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable(){
				public void run()
				{
					fWindow.open(CAS.this);
					System.out.println("New window opened with URL: "+getDocumentBase());
					if (NOWORCESTER)
					{
						String file = "short.txt";
						try
						{
							fWindow.loadFile(file);
						}
						catch (IOException e)
						{
							System.out.println("Failed to load file: "+file);
						}
					}
				}
			});
		}
		catch (Exception e)
		{
			System.out.println("Failed to start CAS: "+e);
		}
	}

	@Override
	public void stop()
	{
		fWindow.close();
		super.stop();
	}

	@Override
	public void destroy()
	{
		super.destroy();
		fWindow = null;
	}

	public void setTitle(String title)
	{
		getAppletContext().showStatus(title);
	}

	public void openFile()
	{
		Object[] files = readFileList().toArray();
		String filename = (String)JOptionPane.showInputDialog(this, "Choose file to open:", "Open striking file", JOptionPane.PLAIN_MESSAGE, null, files, files[0]);
		if (filename!=null)
		{
			try
			{
				fWindow.loadFile(filename);
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(getContentPane(), ex.getMessage(), "Failed to open file", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void openNewCASWindow()
	{
		URL u = getDocumentBase();
		System.out.println("Showing new window: "+u);
		getAppletContext().showDocument(u, "_blank");
	}

	public void openNewCASWindow(String fileToLoad)
	{
		URL appletPage = makePageURL(null, fileToLoad);
		if (appletPage!=null)
		{
			System.out.println("Showing new window: "+appletPage);
			getAppletContext().showDocument(appletPage, "_blank");
		}
	}

	private URL makePageURL(String page, String queryString)
	{
		URL currentPage = getDocumentBase();
		String path = currentPage.getPath();
		if (page!=null)
		{
			int i = path.lastIndexOf('/');
			if (i>=0)
				path = path.substring(0, i);
			path+= page;
		}
		if (queryString!=null)
			path+= queryString;
		try
		{
			return new URL(currentPage, path);
		}
		catch (MalformedURLException e)
		{
			System.out.println("Failed to create URL for new window: "+e);
		}
		return null;
	}

	@Override
	public void closeWindow()
	{
		URL closePage = makePageURL("close.html", null);
		if (closePage!=null)
		{
			fWindow.close();
			getAppletContext().showDocument(closePage, "_blank");
		}
	}

	public void openSummaryWindow()
	{
		URL summaryPage = makePageURL("summary.html", null);
		if (summaryPage!=null)
		{
			System.out.println("Showing summary window: "+summaryPage);
			getAppletContext().showDocument(summaryPage, "summary");
		}
	}

	public Reader getReader(String name) throws FileNotFoundException
	{
		InputStream in = getClass().getResourceAsStream("/"+name);
		if (in==null)
			throw new FileNotFoundException(name);
		return new InputStreamReader(in);
	}

	public void export()
	{
		// Not supported - can't save stuff.
		// Could open a text window to display I guess.

	}

	/**
	 * In applet mode, files.lst gives us the list of striking files we
	 * can open.
	 */
	private List<String> readFileList()
	{
		List<String> files = new ArrayList<String>();
		LineNumberReader in = null;
		try
		{
			in = new LineNumberReader(getReader(APPLET_FILE_LIST));
			String line = in.readLine();
			while (line!=null)
			{
				line = line.trim();
				if (line.length()>0)
					files.add(line);
				line = in.readLine();
			}
		}
		catch (IOException e)
		{
			System.out.println("Failed to read files list: "+e);
			files.add("peal.txt");
			files.add("short.txt");
			files.add("touch3.txt");
			files.add("touch5.txt");
		}
		finally
		{
			if (in!=null)
			{
				try
				{
					in.close();
				}
				catch (IOException e)
				{	}
			}
		}
		return files;
	}
}
