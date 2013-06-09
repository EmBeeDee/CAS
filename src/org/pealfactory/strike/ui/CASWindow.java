package org.pealfactory.strike.ui;

import org.pealfactory.strike.CAS;
import org.pealfactory.strike.input.*;
import org.pealfactory.strike.analyser.Visualiser;
import org.pealfactory.strike.data.TouchStats;
import org.pealfactory.strike.audio.PlaybackController;
import org.pealfactory.strike.audio.MidiBellSounds;
import org.pealfactory.strike.pipeline.Pipeline;

import java.awt.print.*;
import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.*;
import java.awt.*;
import java.io.*;

/**
 * Provides the UI for the main CAS window, whether running as an applet or an application.
 * CASWindow is not a Swing component in its own right - instead it pokes components into
 * its parent CASContainer. A CASContainer is either a CAS instance (which is an applet),
 * or a CASFrame (when running as an application).
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
public class CASWindow extends BaseUI implements ChangeListener
{
	private static List<CASWindow> gOpenWindows = Collections.synchronizedList(new ArrayList<CASWindow>()); 

	private static final String ACTION_CAPTURE = "CAPTURE";
	private static final String ACTION_SUMMARISE = "SUMMARISE";
	private static final String ACTION_PLAY = "PLAY";
	private static final String ACTION_STOP = "STOP";
	private static final String ACTION_SETVISUALISER = "VISUALISER";
	private static final String ACTION_TOGGLESCROLL = "TOGGLESCROLL";
	private static final String ACTION_RESETSPEED = "RESETSPEED";
	private static final String ACTION_CHANGEPITCH = "CHANGEPITCH";
	private static final String ID_PLAYBACKSPEED = "PLAYBACKSPEED";

	private static final String EXT_BAND_LIST = ".lst";

	private CASContainer fParent;

	private Pipeline fPipeline;
	private String fCurrentFile;
	private StrikingDisplay fDisplay;
	private PlaybackController fPlayback;
	private TouchStats fData;
	private MutableComboBoxModel fVisualiserNames;
	private Map<String, Visualiser> fVisualisers = new HashMap<String, Visualiser>();
	private Visualiser fCurrentVisualiser;

	private JLabel fLoadingIndicator;
	private JLabel fFileInfo;
	private JTextArea fVisualiserInfo;
	private JLabel fTouchStats;
	private JLabel fBellStats;
	private JSlider fSpeedSlider;

	private int fSelectedBell;
	private boolean fInChangesOnly = true;

	public CASWindow()
	{
		super();
		fVisualiserNames = new DefaultComboBoxModel();
		fDisplay = new AbelDisplay();
	}

	public static List<CASWindow> getCASWindows()
	{
		return gOpenWindows;
	}

	public void open(CASContainer parent)
	{
		fParent = parent;
		super.setRoot(parent);

		fDisplay.setOpaque(true);
		fDisplay.setBackground(new Color(246, 252, 255));
		fDisplay.setInChangesOnly(fInChangesOnly);
		createUI(fDisplay);
		if (fCurrentVisualiser!=null)
			setVisualiser(fCurrentVisualiser);

		gOpenWindows.add(this);
	}

	public void close()
	{
		gOpenWindows.remove(this);
	}

	public CASContainer getParent()
	{
		return fParent;
	}

	public void loadRows(TouchStats data)
	{
		fData = data;
		updateStats();
		SwingUtilities.invokeLater(fDisplay.loadRows(fData));
	}

	public void visualisationComplete()
	{
		SwingUtilities.invokeLater(new Runnable(){
				public void run()
				{
					fLoadingIndicator.setEnabled(false);
					fDisplay.scrollToPreviousPosition();
				}
			});
	}

	public void setPlaybackController(PlaybackController controller)
	{
		fPlayback = controller;
	}

	public Pipeline getPipeline()
	{
		return fPipeline;
	}

	public void setPipeline(Pipeline pipeline)
	{
		// Need to stop old pipeline!!!!
		fPipeline = pipeline;
		fPipeline.setUI(this);
		if (fCurrentVisualiser!=null)
			setVisualiser(fCurrentVisualiser);
	}

	public void notifyInputError(final String msg)
	{
		SwingUtilities.invokeLater(new Runnable(){
			public void run()
			{
				JOptionPane.showMessageDialog(fParent.getContentPane(), msg, "Failed to load file", JOptionPane.ERROR_MESSAGE);
				// @todo Need to disable loading indicator?
			}
		});
	}

	public void addVisualiser(Visualiser visualiser)
	{
		fVisualisers.put(visualiser.getName(), visualiser);
		fVisualiserNames.addElement(visualiser.getName());
		if (fCurrentVisualiser==null)
			fCurrentVisualiser = visualiser;
	}

	public void setVisualiser(Visualiser visualiser)
	{
		fCurrentVisualiser = visualiser;
		fVisualiserInfo.setText(fCurrentVisualiser.getInfo());
		// Pipeline marshals work off onto a separate thread, also handles queued visualiser changes.
		if (fPipeline!=null)
			fPipeline.setVisualiser(fCurrentVisualiser);
	}

	private void clearVisualiserData()
	{
		for (Visualiser visualiser1 : fVisualisers.values())
		{
			Visualiser visualiser = visualiser1;
			visualiser.clearData();
		}
	}

	@Override
	protected void createToolBar(JToolBar toolbar)
	{
		JButton newBut = createIconButton("general/New", "New", ACTION_NEW, KeyEvent.VK_N);
		JButton openBut = createIconButton("general/Open", "Open", ACTION_OPEN, KeyEvent.VK_O);
		JButton reloadBut = createIconButton("general/Refresh", "Reload", ACTION_RELOAD, KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		JButton captureBut = createIconButton("general/Import", "Capture", ACTION_CAPTURE, KeyEvent.VK_C );
		JButton saveBut = createIconButton("general/Save", "Save", ACTION_SAVE, KeyEvent.VK_S);
		JButton printBut = createIconButton("general/Print", "Print", ACTION_PRINT, KeyEvent.VK_P);
		JButton pageSetupBut = createIconButton("general/PageSetup", "Page Setup", ACTION_PAGESETUP, KeyEvent.VK_A);
		JButton summariseBut = createIconButton("general/Edit", "Summariser", ACTION_SUMMARISE, KeyEvent.VK_U);

		toolbar.add(newBut);
		toolbar.add(openBut);
		toolbar.add(saveBut);
		toolbar.addSeparator();
		toolbar.add(captureBut);
		toolbar.add(reloadBut);
		toolbar.addSeparator();
		toolbar.add(summariseBut);
		toolbar.addSeparator();
		toolbar.add(printBut);
		toolbar.add(pageSetupBut);
		toolbar.addSeparator();

		toolbar.add(new JLabel("Visualiser:"));
		toolbar.addSeparator();
		JComboBox visualiserCombo = new JComboBox(fVisualiserNames);
		visualiserCombo.addActionListener(this);
		visualiserCombo.setActionCommand(ACTION_SETVISUALISER);
		toolbar.add(visualiserCombo);
		toolbar.addSeparator();

		toolbar.add(new JLabel(createIcon("general/Zoom", "Zoom")));
		toolbar.add(createZoomControl());
		toolbar.addSeparator();

    Icon redButton = createIcon("button_red.gif");
		Icon greyButton = createIcon("button_grey.gif");
		fLoadingIndicator = new JLabel("Loading Data", redButton, JLabel.LEADING);
		fLoadingIndicator.setDisabledIcon(greyButton);
		fLoadingIndicator.setHorizontalTextPosition(JLabel.LEADING);
		fLoadingIndicator.setEnabled(false);
		fLoadingIndicator.setForeground(Color.red);
		toolbar.add(fLoadingIndicator);
		toolbar.addSeparator();

		toolbar.addSeparator();
		toolbar.add(createBellCaption());
		toolbar.addSeparator();
		toolbar.add(createBellCombo());
		toolbar.addSeparator();

		JButton infoBut = createIconButton("general/Help", "About", ACTION_ABOUT, -1, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0, true));
		JButton playBut = createIconButton("media/Play", "Play", ACTION_PLAY, -1, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true));
		JButton stopBut = createIconButton("media/Stop", "Stop", ACTION_STOP);

		toolbar.add(playBut);
		toolbar.add(stopBut);

		toolbar.addSeparator();
		toolbar.add(infoBut);
	}

	@Override
	protected void createSidePanel(JPanel side)
	{
		fVisualiserInfo = new JTextArea(5, 20);

    fFileInfo = createLabel("");
		fFileInfo.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		updateFileInfo("none loaded", "Source", "-");

		JPanel fileBox = new JPanel();
		fileBox.setBorder(BorderFactory.createTitledBorder("Striking Data"));
		fileBox.setLayout(new BoxLayout(fileBox, BoxLayout.X_AXIS));
		fileBox.add(Box.createHorizontalGlue());
		fileBox.add(fFileInfo);
		fileBox.add(Box.createHorizontalGlue());

		fVisualiserInfo.setEditable(false);
		fVisualiserInfo.setLineWrap(true);
		fVisualiserInfo.setWrapStyleWord(true);
		fVisualiserInfo.setMargin(new Insets(3, 3, 3, 3));

		JPanel infoContainer = new JPanel(new BorderLayout());
		infoContainer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Visualiser information"), BorderFactory.createLoweredBevelBorder()));
		infoContainer.add(fVisualiserInfo, BorderLayout.NORTH);

		JPanel optionsBox = createOptionsBox(fInChangesOnly, false);
		optionsBox.setBorder(BorderFactory.createTitledBorder("Options"));
		fDisplay.setAdvancedView(false);

		JPanel touchStatsBox = new JPanel();
		touchStatsBox.setBorder(BorderFactory.createTitledBorder("Touch statistics"));
		touchStatsBox.setLayout(new BoxLayout(touchStatsBox, BoxLayout.X_AXIS));
		fTouchStats = createLabel(getTouchStats());
		touchStatsBox.add(Box.createGlue());
		touchStatsBox.add(fTouchStats);
		touchStatsBox.add(Box.createGlue());

		JPanel selectedBellBox = new JPanel();
		selectedBellBox.setLayout(new BoxLayout(selectedBellBox, BoxLayout.X_AXIS));
		selectedBellBox.setBorder(BorderFactory.createTitledBorder("Bell Statistics"));
		fBellStats = createLabel(getBellStats());
		selectedBellBox.add(Box.createGlue());
		selectedBellBox.add(fBellStats);
		selectedBellBox.add(Box.createGlue());

		JPanel audioBox = new JPanel();
		audioBox.setLayout(new BoxLayout(audioBox, BoxLayout.X_AXIS));
		audioBox.setBorder(BorderFactory.createTitledBorder("Audio Controls"));

		JPanel audioControls = new JPanel();
		audioControls.setLayout(new BoxLayout(audioControls, BoxLayout.Y_AXIS));
		JPanel audio1 = new JPanel();
		audio1.setLayout(new BoxLayout(audio1, BoxLayout.X_AXIS));
		audio1.add(Box.createHorizontalGlue());
		audio1.add(new JLabel("Playback speed"));
		audio1.add(Box.createHorizontalGlue());
		audio1.add(Box.createHorizontalGlue());
		JButton resetBut = new JButton("Reset to 1x");
		resetBut.setMargin(new Insets(0, 2, 0, 2));
		//resetBut.setFont(fLabelFont);
		resetBut.setActionCommand(ACTION_RESETSPEED);
		resetBut.addActionListener(this);
		audio1.add(resetBut);
		audio1.add(Box.createHorizontalGlue());
		audioControls.add(audio1);

    fSpeedSlider = new JSlider(JSlider.HORIZONTAL, 5, 40, 10);
		fSpeedSlider.setMajorTickSpacing(5);
		fSpeedSlider.setMinorTickSpacing(1);
		fSpeedSlider.setPaintTicks(true);
		Dictionary<Integer, JLabel> speedTable = new Hashtable<Integer, JLabel>();
		speedTable.put( new Integer(5), new JLabel("2x") );
		speedTable.put( new Integer(10), new JLabel("1x") );
		speedTable.put( new Integer(20), new JLabel("1/2") );
		speedTable.put( new Integer(30), new JLabel("1/3") );
		speedTable.put( new Integer(40), new JLabel("1/4") );
		fSpeedSlider.setLabelTable(speedTable);
		fSpeedSlider.setPaintLabels(true);
		fSpeedSlider.setName(ID_PLAYBACKSPEED);
		fSpeedSlider.addChangeListener(this);
		audioControls.add(fSpeedSlider);
		audioControls.add(Box.createGlue());

		JPanel audio2 = new JPanel();
		audio2.setLayout(new BoxLayout(audio2, BoxLayout.X_AXIS));
		JCheckBox scrollWithPlayback = createCheckBox("Scroll with playback", ACTION_TOGGLESCROLL, true, "Enable this checkbox to scroll the display as the sound plays");
		fDisplay.setAutoScroll(scrollWithPlayback.isSelected());
		audio2.add(scrollWithPlayback);
		audio2.add(Box.createHorizontalGlue());
		audio2.add(new JLabel("Pitch: "));
		JComboBox pitchChooser = new JComboBox(MidiBellSounds.PITCHES);
		pitchChooser.setSelectedItem("C");
		pitchChooser.setActionCommand(ACTION_CHANGEPITCH);
		pitchChooser.addActionListener(this);
    audio2.add(pitchChooser);
		audioControls.add(audio2);
		audioBox.add(audioControls);

		JPanel side2 = new JPanel();
		side2.setLayout(new BoxLayout(side2, BoxLayout.Y_AXIS));
		side2.add(fileBox);
		side2.add(Box.createVerticalStrut(2));
		side2.add(optionsBox);
		side2.add(Box.createVerticalStrut(2));
		side2.add(touchStatsBox);
		side2.add(Box.createVerticalStrut(2));
		side2.add(selectedBellBox);
		side2.add(Box.createVerticalStrut(2));
		side2.add(infoContainer);
		side2.add(Box.createVerticalGlue());
		side2.add(audioBox);

		side.add(side2, BorderLayout.NORTH);
	}

	/**
	 * Expects to be called on an application thread (not the event thread, although will work from event thread).
	 */
	private void updateBellStats()
	{
		if (fBellStats==null)
			return;
		final String s = getBellStats();
		SwingUtilities.invokeLater(new Runnable(){
			public void run()
			{
				fBellStats.setText(s);
			}
		});
	}

	private String getTouchStats()
	{
		TouchStats.HandBackWhole strikingRMSE = new TouchStats.HandBackWhole();
		TouchStats.HandBackWhole discreteRMSE = new TouchStats.HandBackWhole();
		TouchStats.HandBackWhole rowLengthSD = new TouchStats.HandBackWhole();
		TouchStats.HandBackWholeInt maxDuration = new TouchStats.HandBackWholeInt();
		TouchStats.HandBackWholeInt minDuration = new TouchStats.HandBackWholeInt();
		TouchStats.HandBackWhole avGap = new TouchStats.HandBackWhole();
		int nfaults = 0;
		double faultPercentage = 0;
		if (fData!=null)
		{
			strikingRMSE = fData.getStrikingRMSE(fInChangesOnly);
			discreteRMSE = fData.getDiscreteStrikingRMSE(fInChangesOnly);
			rowLengthSD = fData.getRowLengthSD(fInChangesOnly);
			maxDuration = fData.getMaxDuration(fInChangesOnly);
			minDuration = fData.getMinDuration(fInChangesOnly);
			avGap = fData.getMeanInterbellGap( fInChangesOnly);
			nfaults = fData.getFaults(fInChangesOnly);
			faultPercentage = fData.getFaultPercentage(fInChangesOnly);
		}
		final StringBuffer s = new StringBuffer();
		s.append("<html><table>");
		s.append("<tr><td><b></b></td><td>Whole</td><td>Hand</td><td>Back</td></tr>");
		rowHtml(s, TouchStats.TEXT_STRIKING_RMSE, strikingRMSE);
		rowHtml(s, TouchStats.TEXT_DISCRETE_RMSE, discreteRMSE);
		rowHtml(s, TouchStats.TEXT_INTERVAL_MEAN, avGap);
		rowHtml(s, TouchStats.TEXT_QUICKEST_ROW, minDuration);
		rowHtml(s, TouchStats.TEXT_SLOWEST_ROW, maxDuration);
		rowHtml(s, TouchStats.TEXT_ROW_LENGTH_SD, rowLengthSD);
		s.append("<tr><td>");
		s.append(TouchStats.TEXT_FAULTS);
		s.append("</td><td>");
		s.append(nfaults);
		s.append("</td><td>");
		s.append(toPercentage(faultPercentage));
		s.append("</td><td></td></tr>");
		s.append("</table></html>");
		return s.toString();
	}

	private String getBellStats()
	{
		TouchStats.HandBackWhole bellSD = new TouchStats.HandBackWhole();
		TouchStats.HandBackWhole bellRMSE = new TouchStats.HandBackWhole();
		TouchStats.HandBackWhole bellLate = new TouchStats.HandBackWhole();
		if (fData!=null && fSelectedBell>0)
		{
			bellSD = fData.getBellSD(fSelectedBell, fInChangesOnly);
			bellRMSE = fData.getBellRMSE(fSelectedBell, fInChangesOnly);
			bellLate = fData.getLateness(fSelectedBell, fInChangesOnly);
		}
		final StringBuffer s = new StringBuffer();
		s.append("<html><table>");
		s.append("<tr><td><b>Selected:</b></td><td>");
		if (fSelectedBell>0)
			s.append(fSelectedBell);
		else
			s.append("none");
		s.append("</td><td></td><td></td></tr>");
		s.append("<tr><td><b></b></td><td>Whole</td><td>Hand</td><td>Back</td></tr>");
		rowHtml(s, TouchStats.TEXT_RMSE, bellRMSE);
		rowHtml(s, TouchStats.TEXT_SD, bellSD);
		rowHtml(s, TouchStats.TEXT_AV_MS_LATE, bellLate);
		s.append("</table></html>");
		return s.toString();
	}

	private void rowHtml(StringBuffer s, String rowTitle, TouchStats.HandBackWholeInt statsInt)
	{
		TouchStats.HandBackWhole stats = new TouchStats.HandBackWhole();
		stats.hand = statsInt.hand;
		stats.back = statsInt.back;
		stats.whole = statsInt.whole;
		rowHtml(s, rowTitle, stats);
	}
	private void rowHtml(StringBuffer s, String rowTitle, TouchStats.HandBackWhole stats)
	{
		s.append("<tr><td>");
		s.append(rowTitle);
		s.append("</td><td>");
		s.append(toMilliseconds(stats.whole));
		s.append("</td><td>");
		s.append(toMilliseconds(stats.hand));
		s.append("</td><td>");
		s.append(toMilliseconds(stats.back));
		s.append("</td></tr>");

	}

	public void stateChanged(ChangeEvent e)
	{
		JComponent source = (JComponent)e.getSource();
		if (source.getName().equals(ID_PLAYBACKSPEED))
		{
			JSlider speedSlider = (JSlider)source;
			if (!speedSlider.getValueIsAdjusting())
			{
      	fPlayback.setPlaybackSpeed(10.0f/speedSlider.getValue());
			}
		}
	}

	protected void registerActionListeners()
	{
		super.registerActionListeners();
		fActionMap.put(ACTION_CAPTURE, new ActionCapture());
		fActionMap.put(ACTION_SUMMARISE, new ActionSummarise());
		fActionMap.put(ACTION_PLAY, new ActionStartPlayback());
		fActionMap.put(ACTION_STOP, new ActionStopPlayback());
		fActionMap.put(ACTION_SETVISUALISER, new ActionSetVisualiser());
		fActionMap.put(ACTION_TOGGLESCROLL, new ActionToggleScroll());
		fActionMap.put(ACTION_RESETSPEED, new ActionResetSpeed());
		fActionMap.put(ACTION_CHANGEPITCH, new ActionChangePitch());
	}

	class ActionCapture implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			// TO-DO
		}
	}

	class ActionSetVisualiser implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JComboBox visualiserCombo = (JComboBox)e.getSource();
      setVisualiser(fVisualisers.get(visualiserCombo.getSelectedItem()));
		}
	}

	class ActionSummarise implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			fParent.openSummaryWindow();
		}
	}

	class ActionStartPlayback implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (fPlayback.isPlaying())
			{
				fPlayback.stop();
			}
			else
			{
				fPlayback.init(fData.getNBells(), fDisplay, false);
				fPlayback.play(fData, Math.max(0, fDisplay.getPlaybackStartRow()));
			}
		}
	}

	class ActionStopPlayback implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			fPlayback.stop();
		}
	}

	class ActionToggleScroll implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JCheckBox checkBox = (JCheckBox)e.getSource();
			fDisplay.setAutoScroll(checkBox.isSelected());
		}
	}

	class ActionResetSpeed implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			fSpeedSlider.setValue(10);
		}
	}

	class ActionChangePitch implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JComboBox combo = (JComboBox)e.getSource();
      fPlayback.setTenorPitch(combo.getSelectedIndex());
		}
	}

	@Override
	public void setInChangesOnly(boolean inChangesOnly)
	{
		super.setInChangesOnly(inChangesOnly);
		actionInChangesOnly(inChangesOnly);
	}

	@Override
	protected void actionInChangesOnly(boolean inChangesOnly)
	{
		fInChangesOnly = inChangesOnly;
		fDisplay.setInChangesOnly(fInChangesOnly);
		updateStats();
	}

	@Override
	public void setAdvancedView(boolean advancedView)
	{
		super.setAdvancedView(advancedView);
		actionAdvancedView(advancedView);
	}

	@Override
	protected void actionAdvancedView(boolean advancedView)
	{
		fDisplay.setAdvancedView(advancedView);
	}

	@Override
	public void setSelectedBell(int bell)
	{
		super.setSelectedBell(bell);
		actionNewBell(bell);
	}

	@Override
	protected void actionNewBell(int bell)
	{
		fSelectedBell = bell;
		fDisplay.setHighlightedBell(bell);
		updateBellStats();
	}

	public void setZoom(float zoom)
	{
		actionZoom(zoom);
	}

	@Override
	protected void actionZoom(float zoom)
	{
		super.setZoom(zoom);
		fDisplay.setZoom(zoom);
	}

	@Override
	protected void actionNewWindow()
	{
		fParent.openNewCASWindow();
	}

	@Override
	protected void actionFileOpen()
	{
		fParent.openFile();
	}

	@Override
	protected void actionReload()
	{
		try
		{
			fDisplay.storePreviousScrollPosition();
			loadOneFile(fCurrentFile);
		}
		catch (IOException ex)
		{
			JOptionPane.showMessageDialog(fParent.getContentPane(), ex.getMessage(), "Failed to open file", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	protected void actionFileSave()
	{
		// This is export not save - really need a separate button
		fParent.export();
	}

	/**
	 * Should only be called from the event thread
	 *
	 * @param filename
	 * @throws IOException
	 */
	public void loadFile(String filename) throws IOException
	{
		if (filename.toLowerCase().endsWith(EXT_BAND_LIST))
			loadMultipleFiles(filename);
		else
			loadOneFile(filename);
	}

	private void loadMultipleFiles(String bandList) throws IOException
	{
		java.util.List bands = new InputFactory().readBandListFile(bandList);
		File newHome = new File(bandList).getParentFile();
		Iterator i=bands.iterator();
		if (i.hasNext())
		{
			// Load the first file into the current window
			File firstFile = new File(newHome, i.next().toString());
			loadFile(firstFile.getPath());
			while (i.hasNext())
			{
				fParent.openNewCASWindow(i.next().toString());
			}
		}
		CAS.setHomeDirectory(newHome);
	}

	private void loadOneFile(String filename) throws IOException
	{
		try
		{
			fLoadingIndicator.setEnabled(true);
			InputFactory factory = new InputFactory();
			StrikingDataInput inputter = factory.createInputter(filename, fParent);
			Pipeline newPipeline = new Pipeline(inputter);
			updateFileInfo(newPipeline.getName(), newPipeline.getInputSource(), newPipeline.getInputFormat());
			fParent.setTitle("CAS ("+newPipeline.getName()+")");
			clearVisualiserData();
			setPipeline(newPipeline);
			// Pipeline starts a new thread to do the load.
			newPipeline.start();
		}
		catch (IOException e)
		{
			fLoadingIndicator.setEnabled(false);
			throw e;
		}
	}

	public void saveFile(final PrintWriter out)
	{
		out.println("CAS export for "+this.getPipeline().getName());
		PrintWriter prefixedOut = new PrintWriter(out){
			@Override
			public void println(String x)
			{
				super.println("* "+x);
			}
		};
		exportStats(prefixedOut);
		CasBongInput.outputRowData(fData, out);
	}

	public void exportStats(PrintWriter out)
	{
		out.println("Analysing: "+fCurrentFile+" with "+fCurrentVisualiser.getName());
		if (fData==null)
			out.println("No data");
		else
			fData.outputStats(out, fInChangesOnly);
	}

	private void updateFileInfo(String filename, String source, String format)
	{
		fCurrentFile = filename;
    StringBuffer s = new StringBuffer();
		s.append("<html><table><tr><td>");
		s.append(source);
		s.append(":</td><td>");
		// Can't really display the full path, because it makes the right-hand pane too wide.
		// So, strip off any directory part we find.
		s.append(getFileOnly(filename));
		s.append("</td></tr><tr><td>");
		s.append("Format:");
		s.append("</td><td>");
		s.append(format);
		s.append("</td></tr></table></html");
		fFileInfo.setText(s.toString());
	}

	private void updateStats()
	{
		updateTouchStats();
		updateBellStats();
	}

	/**
	 * Expects to be called on an application thread (not the event thread).
	 */
	private void updateTouchStats()
	{
		if (fTouchStats==null)
			return;
		final String s = getTouchStats();
		SwingUtilities.invokeLater(new Runnable(){
			public void run()
			{
				fTouchStats.setText(s);
			}
		});
	}

	/**
	 * We must create a separate AbelDisplay for printing purposes; if we tried to use the main fDisplay for printing
	 * there would likely be bad interactions between the printing thread and the Swing paint thread, caused for
	 * instance by the different font heights and widths between the two contexts. Using a separate display object
	 * for printing also allows us to be independent of the zoom level and other display-only features set on the main
	 * display object.
	 *
	 * @return
	 * @throws PrinterException
	 */
	@Override
	protected void setupPrintable() throws PrinterException
	{
		StrikingDisplay printableStrikingDisplay = new AbelDisplay();
		printableStrikingDisplay.setOpaque(false);
		printableStrikingDisplay.setInChangesOnly(fInChangesOnly);
		printableStrikingDisplay.setAdvancedView(true);
		// It seems a standalone component won't have a Font of its own, so set it from fDisplay, to prevent NPE later.
		printableStrikingDisplay.setFont(fDisplay.getFont());
		printableStrikingDisplay.setZoom(0.5f);

		// We're on the event thread here... should marshall this off to a worker?
		printableStrikingDisplay.loadRows(fData).run();
		
		fPrintable = printableStrikingDisplay;
	}

	@Override
	protected String getPrintPageHeaderLeft()
	{
		return fCurrentVisualiser.getName();
	}

	@Override
	protected String getPrintPageHeaderCentre()
	{
		return "CAS Striking Graph";
	}

	@Override
	protected String getPrintPageHeaderRight()
	{
		return getFileOnly(fPipeline.getName());
	}

}
