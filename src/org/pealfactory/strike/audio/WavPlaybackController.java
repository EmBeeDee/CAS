package org.pealfactory.strike.audio;

import org.pealfactory.strike.data.AveragedRowData;
import org.pealfactory.strike.data.AveragedRowSource;

import java.io.File;

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
public class WavPlaybackController extends PlaybackController
{
	ReplayThread fThread;

	public void doInit(int nbells)
	{
		BellSounds audio = new WavBellSounds(new File("bell.wav"));
		audio.init(nbells);
    fThread = new ReplayThread(audio);
		fThread.setPerfectRinging(fPerfectRinging);
	}

	protected void doPlay(AveragedRowSource data, int startRow)
	{
		fThread.setSpeedFactor(1/fPlaybackSpeed);
		fThread.play(data, startRow);
	}

	protected void doStop()
	{
		fThread.stop();
	}

}
