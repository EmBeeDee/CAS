package org.pealfactory.strike.audio;

import org.pealfactory.strike.data.AveragedRowSource;
import org.pealfactory.strike.ui.StrikingDisplay;

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
public abstract class PlaybackController
{
	protected StrikingDisplay fDisplay;
  protected boolean fPerfectRinging = false;
	protected float fPlaybackSpeed = 1.0f;
	/* 5 is middle C */
	protected int fTenorPitch = 5;
	protected boolean fPlaying;

	public final void init(int nbells, StrikingDisplay display, boolean perfectRinging)
	{
    fDisplay = display;
		fPerfectRinging = perfectRinging;
		doInit(nbells);
	}

	protected abstract void doInit(int nbells);

	public final void play(AveragedRowSource data, int startRow)
	{
		fPlaying = true;
		doPlay(data, startRow);
	}

	protected abstract void doPlay(AveragedRowSource data, int startRow);

	public final void stop()
	{
		doStop();
		fDisplay.setPlayingBell(-1);
		fDisplay.setPlayingRow(-1);
		fPlaying = false;
	}

	protected abstract void doStop();

	public void setPlaybackSpeed(float playbackSpeed)
	{
		fPlaybackSpeed = playbackSpeed;
	}

	public void setTenorPitch(int pitch)
	{
		fTenorPitch = pitch;
	}

	public boolean isPlaying()
	{
		return fPlaying;
	}

	public static PlaybackController getWavPlaybackController()
	{
		return new WavPlaybackController();
	}

	public static PlaybackController getMidiPlaybackController()
	{
		return new MidiPlaybackController();
	}
}
