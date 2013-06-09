package org.pealfactory.strike.pipeline;

import java.util.*;

/**
 * Provides a simple prioritised worker-thread scheduling mechanism.
 * A WorkScheduler instance manages a single worker thread; only one job can
 * run at a time. If a job is scheduled whilst another job is running, the new
 * job is put into a pending queue. At any given priority level, only one pending
 * job is queued at any one time; if a third job comes in at the same priority
 * level, the other pending job is removed.
 * When the currently-executing job is finished, the scheduler runs the highest-priority
 * pending job.
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
public class WorkScheduler
{
	/** Intended for normal jobs. A job at priority 1 interrupts any pending job at priority 1 */
	public final static int PRIORITY_NORMAL = 1;
	/** Intended for high-priority jobs. Pending jobs at lower priority wait until any pending priority-2 job is complete. */
	public final static int PRIORITY_HIGH = 2;
	/** Intended for cleanup jobs which must only run after all pending higher-priority jobs are complete. */
	public final static int PRIORITY_CLEANUP = 0;

	private String fThreadName;
	private Thread fWorkThread;
	private SortedMap fPendingWork = new TreeMap();
	private Object fWorkLock = new Object();

	public WorkScheduler(String workName)
	{
		fThreadName = workName;
	}

	/**
	 * Could maybe interrupt a running thread if a higher-priority work item comes along?
	 *
	 * @param work
	 * @param priority
	 */
	public void addWorkItem(Runnable work, int priority)
	{
    synchronized (fWorkLock)
		{
      if (fWorkThread==null)
				startWork(work);
			else
      	fPendingWork.put(new Integer(priority), work);
		}
	}

	/**
	 * Need to implement this!!!!
	 */
	public void interruptWork()
	{
    synchronized (fWorkLock)
		{
			if (fWorkThread!=null)
				fWorkThread.interrupt();
		}
	}

	/**
	 * Must be called with fWorkLock locked, no pending work, and fWorkThread null.
	 */
	private void startWork(final Runnable work)
	{
		// Create new Runnable to do waiting work.
		Runnable r = new Runnable(){
			public void run()
			{
				try
				{
					work.run();
				}
				finally
				{
					if (Thread.currentThread().isInterrupted())
					{
						// If we are interrupted, stop everything, and clear pending work.
						fPendingWork.clear();
						return;
					}
					// See if any more work is pending after we've finished the first lot -
					// if so, start another work thread.
					synchronized (fWorkLock)
					{
						fWorkThread = null;
            if (!fPendingWork.isEmpty())
            {
	            // Get the highest-priority pending work item.
	            Runnable pendingWork = (Runnable)fPendingWork.remove(fPendingWork.lastKey());
							startWork(pendingWork);
						}
					}
				}
			}
		};
		fWorkThread = new Thread(r, fThreadName);
    fWorkThread.start();
	}


}
