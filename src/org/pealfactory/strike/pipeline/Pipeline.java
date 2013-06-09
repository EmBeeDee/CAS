package org.pealfactory.strike.pipeline;

import org.pealfactory.strike.analyser.*;
import org.pealfactory.strike.input.*;
import org.pealfactory.strike.data.*;
import org.pealfactory.strike.ui.CASFrame;
import org.pealfactory.strike.ui.CASWindow;
import org.pealfactory.strike.errorcorrection.ErrorCorrecter;

import java.util.List;
import java.util.Iterator;

/**
 * Manages storage and data flow for an entire pipeline of striking data, from source (file or capture)
 * through error-correction, visualisation, and display.
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
public class Pipeline implements InputStageListener, AnalysisStageListener
{
	/** Name of the pipeline for saving purposes */
	private String fName;
	/** Input source */
	private String fInputSource;
	/** Type of input data */
	private String fInputFormat;

	private Thread fInputThread;

  /** A pipeline has one supplier of raw striking data, the input stage. It provides a raw Bong stream. */
	private StrikingDataInput fInputStage;

	/** A pipeline has a list of error correcting stages, mostly advised by the input stage.
	 *  However, the last stage of error correction is always the row accumulator. */
	private List fErrorCorrectors;
	private BongListener fFirstErrorCorrecter;
	private RowAccumulator fRowAccumulator;
	private int fNRowsAvailable = 0;
	private boolean fAllRowsRung = false;

	/** A pipeline can have multiple visualisers applied to it, but only one at a time. */
	private Visualiser fCurrentVisualiser;

	/** All visualiser calls are marshaled off to a worker thread */
	private WorkScheduler fVisualiserWorker = new WorkScheduler("Visualiser Work Thread");

	/** Calls to the UI's loadRows method are marshaled onto another worker thread */
	private WorkScheduler fUIWorker = new WorkScheduler("UI Work Thread");

	/** A pipeline has one fixed UI stage */
	private CASWindow fUI;


  public Pipeline(StrikingDataInput inputter)
	{
    fInputStage = inputter;
		fName = inputter.getInputName();
		fInputFormat = inputter.getInputFormat();
		fInputSource = inputter.getInputSource();
	}

	public String getName()
	{
		return fName;
	}

	public String getInputFormat()
	{
		return fInputFormat;
	}

	public String getInputSource()
	{
		return fInputSource;
	}

	/**
	 * The RowAccumulator stage works out the largest bell to have rung so far.
	 *
	 * @return
	 */
	public int getNBells()
	{
		return fRowAccumulator.getNBells();
	}

	/**
	 * Start the pipeline running. This is done on a separate thread.
	 * An input stage will have been plugged in already (via the Pipeline constructor) so this is started,
	 * causing it to begin to deliver Bongs to the error correction stage.
	 * Visualiser and UI stages do not need to be plugged in yet - but they will start receiving pipeline
	 * events as soon as they are.
	 */
	public void start()
	{
		Runnable r = new Runnable(){
			public void run()
			{
				// Set up the error correctors and row accumulator stage.
				fErrorCorrectors = fInputStage.getErrorCorrecters();
				fRowAccumulator = new RowAccumulator(Pipeline.this);
				if (fErrorCorrectors==null || fErrorCorrectors.size()==0)
				{
					fFirstErrorCorrecter = fRowAccumulator;
				}
				else
				{
					// Chain error correctors together
					Iterator i = fErrorCorrectors.iterator();
					ErrorCorrecter errorCorrecter = (ErrorCorrecter)i.next();
					fFirstErrorCorrecter = errorCorrecter;
					while (i.hasNext())
					{
						ErrorCorrecter nextCorrecter = (ErrorCorrecter)i.next();
						errorCorrecter.setNextStage(nextCorrecter);
						errorCorrecter = nextCorrecter;
					}
					errorCorrecter.setNextStage(fRowAccumulator);
				}

				if (Thread.currentThread().isInterrupted())
					return;

				// The pipeline is started by starting the input stage.
				fInputStage.startLoad(Pipeline.this);
			}
		};

		fInputThread = new Thread(r, "Pipeline Input Thread");
		fInputThread.start();
	}

	/**
	 *  First stage: receive Bongs (and possibly errors) from the StrikingDataInput.
	 *
	 * @param bong
	 */
	public void receiveBong(Bong bong)
	{
		fFirstErrorCorrecter.receiveBong(bong);
	}

	public void notifyInputComplete()
	{
    fFirstErrorCorrecter.notifyInputComplete();
	}

	public void notifyInputError(String msg)
	{
    System.out.println("Input failed: "+msg);
		fUI.notifyInputError(msg);
	}

	/**
	 * Second stage: receive error-corrected Rows, pass on to the visualiser (if present)
	 * The work is marshaled onto another thread so that the visualiser can take as long as
	 * it wants over the operation (which could involved long-running stats calculations and
	 * UI update).
	 * <p>
	 * If another row comes along whilst the first one is still processing, the work is queued.
	 * If a third row comes along before the queued row can start, the queued row is dropped and
	 * the third row takes its place.
	 *
	 * @param nrows
	 */
  public void rowsAvailable(int nrows)
	{
		fNRowsAvailable = nrows;
		final RowSource rowSource = getRowSource(nrows);
		Runnable work = new Runnable(){
			public void run()
			{
				if (fCurrentVisualiser!=null)
					fCurrentVisualiser.newRowsAvailable(rowSource);
			}
		};
    fVisualiserWorker.addWorkItem(work, WorkScheduler.PRIORITY_NORMAL);
	}

	/**
	 * Returns null if input not yet finished.
	 *
	 * @return
	 */
	public RowSource getRawTouchData()
	{
		if (fAllRowsRung)
			return getRowSource(fNRowsAvailable);
		return null;
	}

	private RowSource getRowSource(final int nrows)
	{
		return new RowSource(){
			public int getNRows()
			{
				return nrows;
			}
			public Row getRow(int i)
			{
				return fRowAccumulator.getRow(i);
			}
			public int getNBells()
			{
				if (fRowAccumulator!=null)
					return fRowAccumulator.getNBells();
				return 0;
			}
		};
	}

	/**
	 * Final stage: pass averaged row data to the ui
	 */
	public void notifyLastRowRung()
	{
		fAllRowsRung = true;
		Runnable work = new Runnable(){
			public void run()
			{
				if (fCurrentVisualiser!=null)
					fCurrentVisualiser.notifyLastRowRung();
			}
		};
		fVisualiserWorker.addWorkItem(work, WorkScheduler.PRIORITY_CLEANUP);
	}

	/**
	 * This call will be run on a UI worker thread, so it has to be responsive to thread interrupts.
	 * An interrupt probably means that the user has changed their mind and is selecting a different
	 * visualiser, or the UI is being closed, but in either case we should stop immediately.
	 *
	 * @param visualiser
	 */
	public void setVisualiser(final Visualiser visualiser)
	{
		visualiser.setAnalysisListener(this);
		Runnable work = new Runnable(){
			public void run()
			{
				fCurrentVisualiser = visualiser;
				TouchStats existingVisualiserData = fCurrentVisualiser.getAveragedTouchData();
				if (Thread.interrupted())
					return;
				if (existingVisualiserData.getNRows()>0)
					fUI.loadRows(existingVisualiserData);
				if (Thread.interrupted())
					return;
				RowSource rowSource = getRowSource(fNRowsAvailable);
				fCurrentVisualiser.newRowsAvailable(rowSource);
				if (Thread.interrupted())
					return;
				if (fAllRowsRung)
					fCurrentVisualiser.notifyLastRowRung();
			}
		};
		fVisualiserWorker.addWorkItem(work, WorkScheduler.PRIORITY_HIGH);
	}

	public void setUI(CASWindow ui)
	{
		fUI = ui;
	}

	/**
	 * This call may be run on a worker thread, so it has to be responsive to thread interrupts.
	 * An interrupt probably means that the user has changed their mind and is selecting a different
	 * visualiser, or the UI is being closed, but in either case we should stop as soon as we can.
	 * However, we don't want the visualiser to have to worry about interrupts; so if an interrupted
	 * status is detected, we stop forwarding calls to the fUI, but allow the visualiser the option
	 * of completing the current operation, by leaving the interrupted status set.
	 *
	 */
	public void newAveragedRowAvailable()
	{
		final Visualiser visualiser = fCurrentVisualiser;
		if (Thread.currentThread().isInterrupted())
			return;
		Runnable loadRows = new Runnable(){
			public void run()
			{
				fUI.loadRows(visualiser.getAveragedTouchData());
			}
		};
		fUIWorker.addWorkItem(loadRows, WorkScheduler.PRIORITY_NORMAL);
	}

	/**
	 * Should be called by visualisers when they have sent us the last row.
	 */
	public void analysisComplete()
	{
		Runnable loadRows = new Runnable(){
			public void run()
			{
				fUI.visualisationComplete();
			}
		};
		fUIWorker.addWorkItem(loadRows, WorkScheduler.PRIORITY_CLEANUP);
	}

	/**
	 * Stops load and all other threaded operations
	 */
	public void stop()
	{
    if (fInputThread!=null && fInputThread.isAlive())
			fInputThread.interrupt();
    fVisualiserWorker.interruptWork();
		fUIWorker.interruptWork();
	}
}
