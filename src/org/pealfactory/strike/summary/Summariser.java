package org.pealfactory.strike.summary;

import org.pealfactory.strike.analyser.*;
import org.pealfactory.strike.data.*;
import org.pealfactory.strike.pipeline.*;

import java.util.*;

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
public class Summariser
{
	private SummaryWindow fSummaryWindow;

	private List<Visualiser> fVisualisers;
	/** Pipeline filename -> band number */
	private Map<String, Character> fBandNumbers = Collections.synchronizedMap(new HashMap<String, Character>());
	/** Band number -> Pipeline filename  */
	private Map<Character, String> fBands = Collections.synchronizedMap(new HashMap<Character, String>());
	/** Band number -> raw touch data */
	private Map<Character, RowSource> fTouches = Collections.synchronizedMap(new HashMap<Character, RowSource>());
	/** Band order creators by analysis name */
	private Map<String, BandOrderCreator> fBandOrderCreators = Collections.synchronizedMap(new HashMap<String, BandOrderCreator>());
	/** Band order strings -> band order counts*/
	private Map<BasicBandOrder, CountedBandOrder> fBandOrderCounts = new HashMap<BasicBandOrder, CountedBandOrder>(); 
	/** Collated orders  */
	private Set<CountedBandOrder> fPopularOrders = Collections.synchronizedSet(new HashSet<CountedBandOrder>());

	private BandOrder fAverageOrder;

	private Queue<SummariserWorkItem> fWorkItemQueue = new LinkedList<SummariserWorkItem>();

	private final Object fAddBandLock = new Object();
	private final Object fBandOrderLock = new Object();
	private final Object fWorkItemLock = new Object();

	private char fNextBandNumber = 'A';
	private boolean fInChangesOnly;
	private WorkScheduler fAnalysisScheduler = new WorkScheduler("Summary Work Thread");
	private WorkScheduler fUIScheduler = new WorkScheduler("Summary UI Thread");

	public Summariser(SummaryWindow summaryTable)
	{
		fSummaryWindow = summaryTable;
	}

	public void setInChangesOnly(boolean inChangesOnly)
	{
		fInChangesOnly = inChangesOnly;
	}

	public void clearBands()
	{
		synchronized (fAddBandLock)
		{
			synchronized (fWorkItemLock)
			{
				synchronized (fBandOrderLock)
				{
					fNextBandNumber = 'A';
					fBandNumbers.clear();
					fBands.clear();
					fTouches.clear();
					fBandOrderCreators.clear();
					fBandOrderCounts.clear();
					fAverageOrder = null;
					fWorkItemQueue.clear();
				}
			}
		}
	}

	public void addBands(List<Pipeline> pipelines)
	{
		List<Character> newBands = new ArrayList<Character>();
		for (Pipeline pipeline: pipelines)
		{
			String name = pipeline.getName();
			if (!fBandNumbers.containsKey(name))
			{
				RowSource touch = pipeline.getRawTouchData();
				if (touch!=null)
				{
					newBands.add(fNextBandNumber);
					synchronized (fAddBandLock)
					{
						fBands.put(fNextBandNumber, name);
						fBandNumbers.put(name, fNextBandNumber);
						fTouches.put(fNextBandNumber, touch);
						fNextBandNumber++;
					}
				}
			}
		}
		synchronized (fWorkItemLock)
		{
			for (Visualiser v: fVisualisers)
				for (Character band: newBands)
					fWorkItemQueue.offer(new SummariserWorkItem(band, v));
			addFaultsAnalyser(newBands, TouchStats.FAULTFACTOR*0.9);
			addFaultsAnalyser(newBands, TouchStats.FAULTFACTOR);
			addFaultsAnalyser(newBands, TouchStats.FAULTFACTOR*1.1);
		}
	}

	private void addFaultsAnalyser(List<Character> newBands, double faultFactor)
	{
		// Add faults analyser - doesn't matter what visualiser we use, since only raw row data is actually acted on,
		// so pick a simple one with minimal running time.
		for (Character band: newBands)
			fWorkItemQueue.offer(new FaultSummariserWorkItem(band, new LastBellPerfectVisualiser(1.0), faultFactor));
	}

	public String getBandFilename(Character bandNumber)
	{
		return fBands.get(bandNumber);
	}

	public SortedSet<BandOrder> getBandOrders()
	{
		SortedSet<BandOrder> orders = new TreeSet<BandOrder>();
		synchronized (fBandOrderLock)
		{
			for (BandOrderCreator orderCreator: fBandOrderCreators.values())
				orders.add(orderCreator.getBandOrder());
		}
		return orders;
	}

	public SortedSet<CountedBandOrder> getPopularOrders()
	{
		SortedSet<CountedBandOrder> orders = new TreeSet<CountedBandOrder>();
		synchronized (fBandOrderLock)
		{
			for (CountedBandOrder order: fPopularOrders)
				orders.add(order);
		}
		return orders;
	}

	public BandOrder getAverageOrder()
	{
		synchronized(fBandOrderLock)
		{
			return fAverageOrder;
		}
	}

	public void startWork()
	{
		Runnable r = new Runnable(){
			public void run()
			{
				SummariserWorkItem workItem = getNextWorkItem();
				while (workItem!=null)
				{
					workItem.summarize(Summariser.this);
					addNewResult(workItem);
					workItem = getNextWorkItem();
				}
			}
		};
		fAnalysisScheduler.addWorkItem(r, WorkScheduler.PRIORITY_NORMAL);
	}


	private SummariserWorkItem getNextWorkItem()
	{
		synchronized (fWorkItemLock)
		{
			return fWorkItemQueue.poll();
		}
	}

	private void addNewResult(SummariserWorkItem workItem)
	{
		String analysisName = workItem.getAnalysisName();
		BandResult result = new BandResult(workItem.getBandNumber(), workItem.getResult());
		synchronized (fBandOrderLock)
		{
			BandOrderCreator order = fBandOrderCreators.get(analysisName);
			if (order==null)
			{
				order = new BandOrderCreator(analysisName);
				fBandOrderCreators.put(analysisName, order);
			}
			order.addResult(result);
		}
		notifyUI();
	}

	private void notifyUI()
	{
		Runnable scheduleRun = new Runnable(){
			public void run()
			{
				// Do this here rather than in addNewResult() in order to minimize the number of times we call it
				// whilst updating the UI.
				calculateDerivedResults();
				fSummaryWindow.newResults();
			}
		};
		fUIScheduler.addWorkItem(scheduleRun, WorkScheduler.PRIORITY_NORMAL);
	}

	/**
	 * Some stats must be reworked if new results have arrived, before the UI displays them.
	 */
	private void calculateDerivedResults()
	{
		synchronized (fBandOrderLock)
		{
			calculateAverageOrder();
			collateCommonOrders();
		}
	}

	private void calculateAverageOrder()
	{
		BandOrderCreator averageOrder = new BandOrderCreator("Average of all visualisers");
		for (char band='A'; band<fNextBandNumber; band++)
		{
			int nbands = 0;
			double totalResult = 0.0;
			for (BandOrderCreator orderCreator: fBandOrderCreators.values())
			{
				BandOrder order = orderCreator.getBandOrder();
				if (order.getBand(band)!=null)
				{
					nbands++;
					totalResult+= order.getNormalisedResult(band);
				}
			}
			if (nbands>0)
				averageOrder.addResult(new BandResult(band, totalResult/nbands));
			fAverageOrder = averageOrder.getBandOrder();
		}
	}

	private void collateCommonOrders()
	{
		fBandOrderCounts.clear();
		for (BandOrderCreator orderCreator: fBandOrderCreators.values())
		{
			BandOrder order = orderCreator.getBandOrder();
			addBandOrderCount(order);
		}
		addBandOrderCount(fAverageOrder);
		fPopularOrders.clear();
		for (CountedBandOrder cbo: fBandOrderCounts.values())
		{
			fPopularOrders.add(cbo);
		}
	}

	private void addBandOrderCount(BandOrder order)
	{
		CountedBandOrder cbo = fBandOrderCounts.get(order);
		if (cbo==null)
		{
			cbo = new CountedBandOrder(order);
			fBandOrderCounts.put(order, cbo);
		}
		cbo.addOrder(order);
	}

	public void setVisualiserList(List<Visualiser> visualisers)
	{
	  fVisualisers = visualisers;
	}

	protected RowSource getTouchData(char bandNumber)
	{
		return fTouches.get(bandNumber);
	}
	
	public boolean isInChangesOnly()
	{
		return fInChangesOnly;
	}

}
