package org.pealfactory.strike.audio;

import org.pealfactory.strike.data.*;
import org.pealfactory.strike.data.AveragedRowSource;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;

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
public class MidiPlaybackController extends PlaybackController implements MetaEventListener
{
	private MidiBellSounds fMidi;

	protected void doInit(int nbells)
	{
		fMidi = new MidiBellSounds(this);
		fMidi.init(nbells);
	}

	public void meta(MetaMessage meta)
	{
		// Test for end of stream first
	  if (meta.getType()==47)
	  {
		  stop();
		  return;
		}
		// Other meta messages are probably our own row start or bell messages
		String s = new String(meta.getData());
		if (s.startsWith(MidiBellSounds.META_BELL))
		{
			try
			{
				int b = Integer.parseInt(s.substring(MidiBellSounds.META_ROW_START.length()));
				fDisplay.setPlayingBell(b);
			}
			catch (NumberFormatException e)
			{
        System.out.println("WARNING - midi system notified bad bell: "+s);
			}
		}
		else if (s.startsWith(MidiBellSounds.META_ROW_START))
		{
			try
			{
				int r = Integer.parseInt(s.substring(MidiBellSounds.META_ROW_START.length()));
				fDisplay.setPlayingRow(r);
			}
			catch (NumberFormatException e)
			{
        System.out.println("WARNING - midi system notified bad row: "+s);
			}
		}
	}

	public void setPlaybackSpeed(float playbackSpeed)
	{
		super.setPlaybackSpeed(playbackSpeed);
		if (isPlaying())
			fMidi.setPlaybackSpeed(fPlaybackSpeed);
	}

	protected void doPlay(AveragedRowSource data, int startRow)
	{
		fMidi.createSequence();
		AveragedRow row = data.getRow(startRow);
		int startRowTime = row.getRowEndTime()-row.getRowDuration();
		long strikeTime = 0;
		long t = System.currentTimeMillis();
    fMidi.setTenorPitch(fTenorPitch);
		for (int i=startRow; i<data.getNRows(); i++)
		{
			row = data.getRow(i);
			for (int j=0; j<row.getRowSize(); j++)
			{
				if (fPerfectRinging)
					strikeTime = row.getCorrectStrikeTime(j+1)-startRowTime;
				else
					strikeTime = row.getStrikeTime(j+1)-startRowTime;
				if (j==0)
					if (!fMidi.addRowStartMarkToSequence(i, strikeTime))
						break;
				if (!fMidi.addStrikeToSequence(row.getBellAt(j+1), strikeTime))
					break;
			}
		}
		t = System.currentTimeMillis() - t;
		System.out.println("Sequence build took "+t+"ms");
		fMidi.playSequence(fPlaybackSpeed);
	}

	protected void doStop()
	{
		fMidi.stop();
	}

}
