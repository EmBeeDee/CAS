package org.pealfactory.strike.errorcorrection;

import java.util.*;

import org.pealfactory.strike.input.InputStageListener;
import org.pealfactory.strike.data.Bong;

/**
 * Sorts incoming Bongs in time order; necessary for Bagley input files.
 * Note that later error correctors, in particular the RowOverlapCorrector, may re-order
 * bongs so they are once more out of strict time order, if that is necessary to de-interleave
 * whole rows.
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
public class TimeOrderCorrecter extends ErrorCorrectionHelper
{
  private LinkedList fQueue = new LinkedList();

	private int fMaxTimeErrorMs;

	public TimeOrderCorrecter(int maxTimeErrorMs)
	{
		fMaxTimeErrorMs = maxTimeErrorMs;
	}

	public void receiveBong(Bong bong)
	{
		while (!fQueue.isEmpty())
		{
			Bong firstBong = (Bong)fQueue.getFirst();
			if (bong.time-firstBong.time > fMaxTimeErrorMs)
			{
				fQueue.removeFirst();
				fNextStage.receiveBong(firstBong);
			}
			else
			{
				break;
			}
		}
		ListIterator i = fQueue.listIterator();
		while (i.hasNext())
		{
      Bong oldBong = (Bong)i.next();
			if (bong.time<oldBong.time)
			{
				i.previous();
				i.add(bong);
				bong = null;
				break;
			}
		}
		if (bong!=null)
			fQueue.add(bong);
	}

	public void notifyInputComplete()
	{
		ListIterator i = fQueue.listIterator();
		while (i.hasNext())
		{
      Bong bong = (Bong)i.next();
      fNextStage.receiveBong(bong);
		}
		fNextStage.notifyInputComplete();
	}

}
