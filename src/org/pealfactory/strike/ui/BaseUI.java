package org.pealfactory.strike.ui;

import org.pealfactory.strike.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.List;

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
public abstract class BaseUI implements ActionListener, Printable, Constants
{
	protected static final String ACTION_NEW = "NEW";
	protected static final String ACTION_OPEN = "OPEN";
	protected static final String ACTION_RELOAD = "RELOAD";
	protected static final String ACTION_SAVE = "SAVE";
	protected static final String ACTION_PAGESETUP = "PAGESETUP";
	protected static final String ACTION_PRINT = "PRINT";
	protected static final String ACTION_ABOUT = "ABOUT";

	protected static final String ACTION_SETBELL = "BELL";
	protected static final String ACTION_INCHANGESONLY = "INCHANGES";
	protected static final String ACTION_ADVANCEDVIEW = "ADVANCEDVIEW";

	protected Map<String, ActionListener> fActionMap = new HashMap<String, ActionListener>();

	private RootPaneContainer fRoot;
	protected Font fLabelFont = new JTextArea(10,100).getFont();
	private NumberFormat fPercentFormat;
	private NumberFormat fDecimalFormat;

	private JCheckBox fAdvancedViewButton;
	private JCheckBox fInChangesOnlyButton;
	private JComboBox fBellCombo;
	private JSpinner fZoomSpinner;

	private Map<String, Float> fPercentageToZoom = new HashMap<String, Float>();
	private Map<Float, String> fZoomToPercentage = new HashMap<Float, String>();
	private List<String> fZoomDisplay = new ArrayList<String>();

	protected PrintableUI fPrintable = null;
	private PrinterJob fPrinterJob = null;
	private PageFormat fPageFormat = null;

	protected BaseUI()
	{
		fPercentFormat = NumberFormat.getPercentInstance();
		fDecimalFormat = NumberFormat.getNumberInstance();
		fDecimalFormat.setMaximumFractionDigits(2);
		fDecimalFormat.setMinimumFractionDigits(2);
		registerActionListeners();
		setupZoomLevels();
	}

	private void setupZoomLevels()
	{
		addZoomLevel(0.12f);
		addZoomLevel(0.2f);
		addZoomLevel(0.25f);
		addZoomLevel(0.33f);
		addZoomLevel(0.5f);
		addZoomLevel(0.75f);
		addZoomLevel(1.0f);
		addZoomLevel(1.25f);
		addZoomLevel(1.5f);
	}

	private void addZoomLevel(float zoom)
	{
		String display = ""+(zoom*100)+"%";
		fPercentageToZoom.put(display, zoom);
		fZoomToPercentage.put(zoom, display);
		fZoomDisplay.add(display);
	}

	protected void setRoot(RootPaneContainer root)
	{
		fRoot = root;
	}

	protected void createUI(JComponent scrollPane)
	{
		JScrollPane scroller = new JScrollPane(scrollPane);

		JPanel main = new JPanel();
    main.setLayout(new BorderLayout());

		JPanel scrollContainer = new JPanel();
		scrollContainer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createEmptyBorder(2,2,2,2)));
		scrollContainer.setLayout(new BorderLayout());
		scrollContainer.add(scroller, BorderLayout.CENTER);
		main.add(scrollContainer, BorderLayout.CENTER);

		JPanel side = new JPanel(new BorderLayout());
		side.setBorder(BorderFactory.createRaisedBevelBorder());
		createSidePanel(side);
		main.add(side, BorderLayout.EAST);

		Container contentPane = fRoot.getContentPane();
		contentPane.add(main, BorderLayout.CENTER);
		JToolBar toolbar = new JToolBar();
		createToolBar(toolbar);
		contentPane.add(toolbar, BorderLayout.NORTH);
	}

	protected abstract void createToolBar(JToolBar toolbar);

	protected abstract void createSidePanel(JPanel sidebar);

	protected JLabel createBellCaption()
	{
		return new JLabel("Selected Bell:");
	}

	protected JComboBox createBellCombo()
	{
		String[] bells = new String[1+BELL_CHARS.length()];
		bells[0] = "None";
		for (int i=0; i<BELL_CHARS.length(); i++)
			bells[i+1] = BELL_CHARS.substring(i, i+1);
		fBellCombo = new JComboBox(bells);
		fBellCombo.addActionListener(this);
		fBellCombo.setActionCommand(ACTION_SETBELL);
		return fBellCombo;
	}

	protected JSpinner createZoomControl()
	{
		SpinnerModel model = new SpinnerListModel(fZoomDisplay);
		JSpinner spinner = new JSpinner(model);
		spinner.setValue("100.0%");
		spinner.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e)
			{
				JSpinner spinner = (JSpinner)e.getSource();
				actionZoom(fPercentageToZoom.get(spinner.getValue()).floatValue());
			}
		});
		fZoomSpinner = spinner;
		return spinner;
	}

	protected JPanel createOptionsBox(boolean inChangesSelected, boolean advancedViewSelected)
	{
		JPanel optionsBox = new JPanel();
		optionsBox.setLayout(new BoxLayout(optionsBox, BoxLayout.X_AXIS));
		fInChangesOnlyButton = createCheckBox("In changes only", ACTION_INCHANGESONLY, inChangesSelected, "Enable this checkbox to analyse 'in changes' rows only - excludes rounds");
		optionsBox.add(Box.createGlue());
		optionsBox.add(fInChangesOnlyButton);
		optionsBox.add(Box.createGlue());
		fAdvancedViewButton = createCheckBox("Advanced View", ACTION_ADVANCEDVIEW, advancedViewSelected, "Enable this checkbox to see millisecond figures for lead gap, interbell gap and row duration.");
		optionsBox.add(fAdvancedViewButton);
		optionsBox.add(Box.createGlue());
		return optionsBox;
	}

	protected JLabel createLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(fLabelFont);
		return label;
	}

	protected JCheckBox createCheckBox(String label, String action, boolean selected, String tooltip)
	{
		JCheckBox checkBox = new JCheckBox(label);
		checkBox.setSelected(selected);
		checkBox.setActionCommand(action);
		checkBox.addActionListener(this);
		checkBox.setToolTipText(tooltip);
		return checkBox;
	}


	protected JButton createIconButton(String iconName, String desc, String command)
	{
		return createIconButton(iconName, desc, command, -1, null);
	}

	protected JButton createIconButton(String iconName, String desc, String command, int mnemonicKey)
	{
		KeyStroke keyStroke = null;
		if (mnemonicKey!=-1)
			keyStroke = KeyStroke.getKeyStroke(mnemonicKey, InputEvent.CTRL_MASK, true);
		return createIconButton(iconName, desc, command, mnemonicKey, keyStroke);
	}

	/**
	 * Create a button, using an Action registered against the root input map to provide a hotkey.
	 *
	 * @param iconName
	 * @param desc
	 * @param command
	 * @return
	 */
	protected JButton createIconButton(String iconName, String desc, String command, int mnemonicKey, KeyStroke hotkey)
	{
		AbstractAction action = createAction(iconName, desc, command);
		JButton but = new JButton();
		but.setAction(action);
		if (mnemonicKey!=-1)
			but.setMnemonic(mnemonicKey);
		if (hotkey!=null)
		{
			fRoot.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(hotkey,command);
			fRoot.getRootPane().getActionMap().put(command, action);
		}
		return but;
	}

	/**
	 * Adds glue either side to centre the caption and control.
	 *
	 * @param caption
	 * @param control
	 * @return
	 */
	protected JPanel createCaptionAndControl(JLabel caption, JComponent control)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(Box.createHorizontalGlue());
		panel.add(caption);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(control);
		panel.add(Box.createHorizontalGlue());
		return panel;
	}

	protected JPanel createCaptionAndControlLeftAligned(JLabel caption, JComponent control)
	{
		JPanel panel = createCaptionAndControl(caption, control);
		panel.remove(0);
		return panel;
	}

 	/**
	 * Create an Action which delegates to the actionPerformed listener of the BaseUI's implementing class.
	 *
	 * @return
	 */
	protected AbstractAction createAction(String iconName, String desc, final String actionCommand)
	{
		AbstractAction action = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				BaseUI.this.actionPerformed(e);
			}
		};
		action.putValue(Action.SHORT_DESCRIPTION, desc);
		action.putValue(Action.ACTION_COMMAND_KEY, actionCommand);
		Icon icon = createIcon(iconName, desc);
		if (icon!=null)
			action.putValue(Action.SMALL_ICON, icon);
		return action;
	}

	protected Icon createIcon(String iconName, String desc)
	{
		URL u = getClass().getClassLoader().getResource("toolbarButtonGraphics/"+iconName+"16.gif");
		if (u!=null)
			return new ImageIcon(u, desc);
		return null;
	}

	protected Icon createIcon(String path)
	{
		URL u = getClass().getClassLoader().getResource(path);
		if (u!=null)
			return new ImageIcon(u);
		return null;
	}

	protected JLabel createIconLabel(String iconName, String desc)
	{
		Icon icon = createIcon(iconName, desc);
		if (icon==null)
			return new JLabel(desc);
		return new JLabel(icon);
	}

	protected String toPercentage(double d)
	{
		String s = fPercentFormat.format(d);
		while (s.length()<3)
			s = " "+s;
		return s;
	}

	protected String toDecimal(double d)
	{
		return fDecimalFormat.format(d);
	}

	protected String toMilliseconds(int i)
	{
		return i+"ms";
	}

	protected String toMilliseconds(double d)
	{
		return toMilliseconds((int)d);
	}

	protected String getFileOnly(String fullPath)
	{
		int i = fullPath.lastIndexOf(File.separator);
		if (i>=0)
			return fullPath.substring(i+1);
		return fullPath;
	}

	protected void setInChangesOnly(boolean inChangesOnly)
	{
		fInChangesOnlyButton.setSelected(inChangesOnly);
	}

	protected abstract void actionInChangesOnly(boolean inChangesOnly);

	protected void setAdvancedView(boolean advancedView)
	{
		fAdvancedViewButton.setSelected(advancedView);
	}

	protected abstract void actionAdvancedView(boolean advancedView);

	protected void setSelectedBell(int bell)
	{
		fBellCombo.setSelectedIndex(bell);
	}

	protected abstract void actionNewBell(int bell);

	protected abstract void actionNewWindow();

	protected abstract void actionFileOpen();

	protected abstract void actionReload();

	protected abstract void actionFileSave();

	protected void setZoom(float zoom)
	{
		String percentage = fZoomToPercentage.get(zoom);
		if (percentage==null)
			System.out.println("Unknown zoom level: "+zoom);
		else
			fZoomSpinner.setValue(percentage);
	}
	protected abstract void actionZoom(float zoom);

	protected void showHelp()
	{
		String msg = "Computer Analysis of Striking, version "+CAS.VERSION;
		JPanel info = new JPanel();
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		info.add(createCaptionAndControl(new JLabel(msg), new JPanel()));
		info.add(Box.createVerticalStrut(10));
		info.add(createCaptionAndControlLeftAligned(new JLabel("Toolbar icons:"), new JPanel()));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/New", "New"), new JLabel("Open new CAS window")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/Open", "Open"), new JLabel("Open new touch file, or touch list")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/Save", "Save"), new JLabel("Save data to disk (not yet implemented)")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/Import", "Capture"), new JLabel("Capture live ringing data (not yet implemented)")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/Refresh", "Reload"), new JLabel("Reload or refresh data")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/Edit", "Summariser"), new JLabel("Open band Summary window")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/Zoom", "Zoom"), new JLabel("Control zoom level of touch display")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("media/Play", "Play"), new JLabel("Play back ringing from selected change")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("media/Stop", "Stop"), new JLabel("Stop playback")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/Print", "Print"), new JLabel("Print CAS visualisation")));
		info.add(createCaptionAndControlLeftAligned(createIconLabel("general/PageSetup", "Page Setup"), new JLabel("Printer page setup")));
		JOptionPane.showMessageDialog(fRoot.getContentPane(), info, "About CAS", JOptionPane.INFORMATION_MESSAGE);
	}

	public void actionPerformed(ActionEvent e)
	{
		String action = e.getActionCommand();
		ActionListener listener = fActionMap.get(action);
		if (listener==null)
			System.out.println("No listener registered for action "+action);
		else
			listener.actionPerformed(e);
	}

	protected void registerActionListeners()
	{
		fActionMap.put(ACTION_NEW, new ActionNewWindow());
		fActionMap.put(ACTION_OPEN, new ActionOpenFile());
		fActionMap.put(ACTION_RELOAD, new ActionReload());
		fActionMap.put(ACTION_SAVE, new ActionSaveFile());
		fActionMap.put(ACTION_ABOUT, new ActionHelp());
		fActionMap.put(ACTION_SETBELL, new ActionSetBell());
		fActionMap.put(ACTION_INCHANGESONLY, new ActionInChangesOnly());
		fActionMap.put(ACTION_ADVANCEDVIEW, new ActionAdvancedView());
		fActionMap.put(ACTION_PRINT, new ActionPrint());
		fActionMap.put(ACTION_PAGESETUP, new ActionPageSetup());
	}

	class ActionNewWindow implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			actionNewWindow();
		}
	}

	class ActionOpenFile implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			actionFileOpen();
		}
	}

	class ActionReload implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			actionReload();
		}
	}

	class ActionSaveFile implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			actionFileSave();
		}
	}

	class ActionHelp implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			showHelp();
		}
	}

	class ActionSetBell implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JComboBox bellCombo = (JComboBox)e.getSource();
			actionNewBell(bellCombo.getSelectedIndex());
		}
	}

	class ActionInChangesOnly implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JCheckBox checkBox = (JCheckBox)e.getSource();
			actionInChangesOnly(checkBox.isSelected());
		}
	}

	class ActionAdvancedView implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JCheckBox checkBox = (JCheckBox)e.getSource();
			actionAdvancedView(checkBox.isSelected());
		}
	}

	class ActionPrint implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			doPrint();
		}
	}

	class ActionPageSetup implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			PrinterJob job = getPrinterJob();
			if (fPageFormat==null)
				fPageFormat = job.defaultPage();
   		fPageFormat = job.pageDialog(fPageFormat);
		}
	}

	private void doPrint()
	{
		PrinterJob job = getPrinterJob();
		job.setPrintable(this, fPageFormat);
		if (job.printDialog())
		{
			try
			{
				setupPrintable();
				job.print();
			}
			catch (PrinterException ex)
			{
				System.out.println("Failed to print: "+ex);
				JOptionPane.showMessageDialog(fRoot.getContentPane(), ex.getMessage(), "Failed to print", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	protected abstract void setupPrintable() throws PrinterException;

	protected abstract String getPrintPageHeaderLeft();

	protected abstract String getPrintPageHeaderCentre();

	protected abstract String getPrintPageHeaderRight();

	@Override
	public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException
	{
		Font font = new Font("Serif", Font.PLAIN, 8);
		int fontHeight = graphics.getFontMetrics(font).getHeight();

		CASPageFormat caspf = new CASPageFormat(pf, fontHeight*2, fontHeight*2, 0);
		//CASPageFormat caspf = new CASPageFormat(pf, 0, 0, 0);
		if (fPrintable==null)
			throw new PrinterException("Nothing to print - UI failed to set up a PrintableUI");
		int npages = fPrintable.pagesToPrint(caspf);
		if (npages==0)
			throw new PrinterException("No pages to print - header and footer too big to allow printing?");
		if (pageIndex>=npages)
		{
			// Safe to assume Printable can be disposed of here?
			// A bit inefficient if multiple copies are being printed, and all pages are going to be iterated through again;
			// however that is probably unlikely to happen from CAS!
			fPrintable = null;
			return NO_SUCH_PAGE;
		}

		// User (0,0) is typically outside the imageable area, so we must
		// translate by the X and Y values in the PageFormat to avoid clipping
		Graphics2D g2d = (Graphics2D) graphics;
		g2d.translate(pf.getImageableX(), pf.getImageableY());

		// Render header/footer
		printHeaderAndFooter(g2d, caspf, pageIndex, font);

		// Render the striking display - note clip bounds set after this call
		fPrintable.print(g2d, caspf, pageIndex);

		return PAGE_EXISTS;
	}

	private void printHeaderAndFooter(Graphics2D g2d, CASPageFormat pf, int page, Font font)
	{
		g2d.setFont(font);
		String headerLeft = getPrintPageHeaderLeft();
		String headerMid = getPrintPageHeaderCentre();
		String headerRight = getPrintPageHeaderRight();
		String footerLeft = "CAS (C) 2011 MBD";
		String footerMid = "Page "+(page+1);
		String footerRight = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Calendar.getInstance().getTime());
		float pageCentre = (float)pf.pageFormat.getImageableWidth()/2;
		FontMetrics metrics = g2d.getFontMetrics(font);
		float footerBaseline = (float)(pf.pageFormat.getImageableHeight()-metrics.getDescent());
		float headerBaseline = (float)metrics.getHeight();

		Rectangle2D r = metrics.getStringBounds(headerLeft, g2d);
		g2d.drawString(headerLeft, 0.0f, headerBaseline);
		r = metrics.getStringBounds(headerMid, g2d);
		g2d.drawString(headerMid, (float)(pageCentre-r.getWidth()/2), headerBaseline);
		r = metrics.getStringBounds(headerRight, g2d);
		g2d.drawString(headerRight, (float)(pf.pageFormat.getImageableWidth()-r.getWidth()), headerBaseline);

		r = metrics.getStringBounds(footerLeft, g2d);
		g2d.drawString(footerLeft, 0.0f, footerBaseline);
		r = metrics.getStringBounds(footerMid, g2d);
		g2d.drawString(footerMid, (float)(pageCentre-r.getWidth()/2), footerBaseline);
		r = metrics.getStringBounds(footerRight, g2d);
		g2d.drawString(footerRight, (float)(pf.pageFormat.getImageableWidth()-r.getWidth()), footerBaseline);
	}

	private PrinterJob getPrinterJob()
	{
		if (fPrinterJob==null)
			fPrinterJob = PrinterJob.getPrinterJob();
		if (fPageFormat==null)
			fPageFormat = fPrinterJob.defaultPage();
		return fPrinterJob;
	}
}
