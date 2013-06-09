package org.pealfactory.strike.errorcorrection;

import org.pealfactory.strike.data.Bong;
import org.pealfactory.strike.Constants;

/**
 * An error corrector which ignores sensor echoes.
 * A sensor echo is defined as a second strike note from the same bell within a short interval.
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
public class SensorEchoCorrecter extends ErrorCorrectionHelper
{
	private Bong[] fBongs = new Bong[Constants.MAXNBELLS];

	private int fQuickestStrikeTime;

	public SensorEchoCorrecter()
	{
		this(Constants.QUICKEST_STRIKE_TIME);
	}

	public SensorEchoCorrecter(int quickestStrikeTime)
	{
		fQuickestStrikeTime = quickestStrikeTime;
	}

	public void receiveBong(Bong bong)
	{
		Bong prevBong = fBongs[bong.bell-1];
		if (prevBong==null || bong.time-prevBong.time>=fQuickestStrikeTime)
		{
      fBongs[bong.bell-1] = bong;
			fNextStage.receiveBong(bong);
		}
	}

	public void notifyInputComplete()
	{
		fNextStage.notifyInputComplete();
	}
}
