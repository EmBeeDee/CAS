package org.pealfactory.strike.summary;

import org.pealfactory.strike.*;
import org.pealfactory.strike.pipeline.*;
import org.pealfactory.strike.ui.*;

import java.awt.print.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
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
public class SummaryWindow extends BaseUI
{
	private SummaryContainer fParent;
	private Summariser fSummariser;

	private JLabel fBandList;
	private SummaryTreeManager fTreeManager;
	private SummaryTableModel fTableModel;

	private boolean fInChangesOnly;
	private boolean fAdvancedView;

	public SummaryWindow()
	{
		//fTableModel = new SummaryTableModel();
		fTreeManager = new SummaryTreeManager();
		fSummariser = new Summariser(this);
		fSummariser.setVisualiserList(CAS.getAvailableVisualisers());
	}

	public void open(SummaryContainer parent)
	{
		fParent = parent;
		super.setRoot(parent);
		JComponent scrollee = createResultsTree();
		createUI(scrollee);
		addAllOpenTouches();
	}

	public void close()
	{
		// no-op
	}

	private void addAllOpenTouches()
	{
		List<CASWindow> windows = CASWindow.getCASWindows();
			
		List<Pipeline> pipelines = new ArrayList<Pipeline>();
		for (CASWindow w: windows)
		{
			if (w.getPipeline()!=null)
				pipelines.add(w.getPipeline());
		}
		fSummariser.addBands(pipelines);
		fSummariser.startWork();
		updateBandInfo();
	}

	private JTable createResultsTable()
	{
		JTable results = new JTable(fTableModel);
		results.setPreferredSize(new Dimension(500,300));
		results.setFillsViewportHeight(true);
		return results;
	}

	private JTree createResultsTree()
	{
		JTree results = new JTree(fTreeManager.getTreeModel());
		results.setPreferredSize(new Dimension(500,300));
		return results;
	}

	@Override
	protected void createToolBar(JToolBar toolbar)
	{
		JButton newBut = createIconButton("general/New", "New", ACTION_NEW, KeyEvent.VK_N);
		JButton openBut = createIconButton("general/Open", "Open", ACTION_OPEN, KeyEvent.VK_O);
		JButton saveBut = createIconButton("general/Save", "Save", ACTION_SAVE, KeyEvent.VK_S);
		JButton reloadBut = createIconButton("general/Refresh", "Reload", ACTION_RELOAD, KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		JButton printBut = createIconButton("general/Print", "Print", ACTION_PRINT, KeyEvent.VK_P);
		JButton infoBut = createIconButton("general/Help", "About", ACTION_ABOUT, -1, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0, true));

		toolbar.add(newBut);
		toolbar.addSeparator();
		toolbar.add(openBut);
		toolbar.add(saveBut);
		toolbar.add(reloadBut);
		toolbar.addSeparator();
		toolbar.add(printBut);
		toolbar.addSeparator();

		toolbar.add(Box.createHorizontalGlue());
		toolbar.add(infoBut);
	}

	@Override
	protected void createSidePanel(JPanel side)
	{
		fBandList = createLabel("");
		fBandList.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

		JPanel bandBox = new JPanel();
		bandBox.setBorder(BorderFactory.createTitledBorder("Bands:"));
		bandBox.setLayout(new BoxLayout(bandBox, BoxLayout.X_AXIS));
		bandBox.add(Box.createHorizontalGlue());
		bandBox.add(fBandList);
		bandBox.add(Box.createHorizontalGlue());

		JPanel controlsBox = new JPanel();
		controlsBox.setBorder(BorderFactory.createTitledBorder("CAS Master Controls:"));
		controlsBox.setLayout(new BoxLayout(controlsBox, BoxLayout.Y_AXIS));
		controlsBox.add(Box.createHorizontalGlue());
		controlsBox.add(createCaptionAndControl(createBellCaption(), createBellCombo()));
		controlsBox.add(createOptionsBox(fInChangesOnly, fAdvancedView));
		controlsBox.add(createCaptionAndControl(new JLabel("Zoom:"), createZoomControl()));

		JPanel side2 = new JPanel();
		side2.setLayout(new BoxLayout(side2, BoxLayout.Y_AXIS));
		side2.add(controlsBox);
		side2.add(Box.createVerticalStrut(2));
		side2.add(bandBox);
		side2.add(Box.createVerticalStrut(2));

		side.add(side2, BorderLayout.NORTH);
	}

	private void updateBandInfo()
	{
    StringBuffer s = new StringBuffer();
		s.append("<html><table>");
		char bandNumber = 'A';
		String filename = fSummariser.getBandFilename(bandNumber);
		while (filename!=null)
		{
			s.append("<tr><td>");
			s.append(bandNumber);
			s.append("</td><td>");
			s.append(getFileOnly(filename));
			s.append("</td></tr>");
			bandNumber++;
			filename = fSummariser.getBandFilename(bandNumber); 
		}
		s.append("</table></html");
		fBandList.setText(s.toString());
	}

	@Override
	protected void registerActionListeners()
	{
		super.registerActionListeners();
	}

	@Override
	protected void actionInChangesOnly(boolean inChangesOnly)
	{
		fInChangesOnly = inChangesOnly;
		for (CASWindow w: CASWindow.getCASWindows())
			w.setInChangesOnly(fInChangesOnly);
		reloadAll();
	}

	@Override
	protected void actionAdvancedView(boolean advancedView)
	{
		fAdvancedView = advancedView;
		for (CASWindow w: CASWindow.getCASWindows())
			w.setAdvancedView(fAdvancedView);
	}

	@Override
	protected void actionNewBell(int bell)
	{
		for (CASWindow w: CASWindow.getCASWindows())
			w.setSelectedBell(bell);
	}

	@Override
	protected void actionZoom(float zoom)
	{
		for (CASWindow w: CASWindow.getCASWindows())
			w.setZoom(zoom);
	}

	@Override
	protected void actionNewWindow()
	{
		List<CASWindow> casWindows = new ArrayList<CASWindow>();
		for (CASWindow w: CASWindow.getCASWindows())
			casWindows.add(w);
		for (CASWindow w: casWindows)
			w.getParent().closeWindow();
	}

	@Override
	protected void actionFileOpen()
	{
		fParent.openNewCASWindow();
	}

	@Override
	protected void actionReload()
	{
		reloadAll();
	}

	@Override
	protected void actionFileSave()
	{
		// TO-DO
	}

	private void reloadAll()
	{
		fSummariser.clearBands();
		addAllOpenTouches();
	}

	/**
 	* To be called on a worker thread.
 	*/
	public void newResults()
	{
		//fTableModel.newResults(fSummariser);
		fTreeManager.newResults(fSummariser);
	}

	@Override
	protected void setupPrintable() throws PrinterException
	{
	}

	@Override
	protected String getPrintPageHeaderLeft()
	{
		return "";
	}

	@Override
	protected String getPrintPageHeaderCentre()
	{
		return "CAS Band Summary";
	}

	@Override
	protected String getPrintPageHeaderRight()
	{
		return "";
	}
}
