package org.pealfactory.strike.audio;

import org.pealfactory.strike.data.AveragedRowData;
import org.pealfactory.strike.data.AveragedRow;
import org.pealfactory.strike.data.AveragedRowSource;
import sun.misc.*;

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
public class ReplayThread implements Runnable
{
	private boolean fRunning = false;
	private Thread fThread = null;
	private AveragedRowSource fData;
	private int fRowPlaying;
	private BellSounds fSounds;
	private float fSpeedFactor = 1.0f;
	private boolean fPerfectRinging = false;

	private int fNextBell;
	private long fExpectedTime;
	private HiResTimer fTimer;

	public ReplayThread(BellSounds sounds)
	{
		fSounds = sounds;
	}

	public void setPerfectRinging(boolean perfect)
	{
		fPerfectRinging = perfect;
	}

	public void setSpeedFactor(float speedFactor)
	{
		fSpeedFactor = speedFactor;
	}

	public synchronized void play(AveragedRowSource data, int startRow)
	{
		stop();
		fData = data;
		fRowPlaying = startRow;
		fThread = new Thread(this);
		fThread.setPriority(Thread.MAX_PRIORITY);
		fThread.start();
	}

	public synchronized void stop()
	{
		if (fRunning)
		{
			fThread.interrupt();
			try
			{
				wait();
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	public void run()
	{
		fTimer = new HiResTimer();

		fRunning = true;
		AveragedRow row = fData.getRow(fRowPlaying);
		int startRowTime = row.getRowEndTime()-row.getRowDuration();
		long startSysTime = fTimer.currentTimeMillis();
		long lastTime = startSysTime;
		//long lastTime = row.getCorrectStrikeTime(1, fData.getHandstrokeGap());
		while (fRowPlaying<fData.getNRows())
		{
			if (checkInterrupt())
				return;
			row = fData.getRow(fRowPlaying);
			for (int i=0; i<row.getRowSize(); i++)
			{
				int b = row.getBellAt(i+1);
				long strikeTime;
				if (fPerfectRinging)
					strikeTime = (long)((row.getCorrectStrikeTime(i+1)-startRowTime)*fSpeedFactor)+startSysTime;
				else
					strikeTime = (long)((row.getStrikeTime(i+1)-startRowTime)*fSpeedFactor)+startSysTime;
				long timeNow = fTimer.currentTimeMillis();
				long prevTime = timeNow;
				long waitTime = strikeTime-timeNow-20;
				int c = -1;
				if (waitTime>0)
				{
					c++;
					synchronized (this)
					{
						try
						{
							wait(waitTime);
						}
						catch (InterruptedException e)
						{
							notifyAll();
							fRunning = false;
							return;
						}
					}
					timeNow = fTimer.currentTimeMillis();
					waitTime = strikeTime-timeNow;
					while (waitTime>0)
					{
						prevTime = timeNow;
						c++;
						try
						{
							Thread.sleep(1);
						}
						catch (InterruptedException e)
						{
							synchronized (this)
							{
								notifyAll();
								fRunning = false;
								return;
							}
						}
						timeNow = fTimer.currentTimeMillis();
						waitTime = strikeTime-timeNow;
					}
				}
				timeNow = fTimer.currentTimeMillis();
				System.out.println(b+" "+(timeNow-lastTime)+" "+(timeNow-strikeTime)+" "+(timeNow-prevTime)+" "+c);
				lastTime = timeNow;
				fSounds.play(b);
				if (checkInterrupt())
					return;
			}
			fRowPlaying++;
		}
		synchronized (this)
		{
			if (checkInterrupt())
				return;
			fRunning = false;
		}
	}

	private synchronized boolean checkInterrupt()
	{
		if (fThread.isInterrupted())
		{
			notifyAll();
			fRunning = false;
			return true;
		}
		return false;
	}

	class HiResTimer
	{
		Perf hiResTimer;
		long freq;

		public HiResTimer()
		{
			hiResTimer = Perf.getPerf();
			freq = hiResTimer.highResFrequency();
		}
		public long currentTimeMillis()
		{
			return hiResTimer.highResCounter()*1000/freq;
		}
	}
}
