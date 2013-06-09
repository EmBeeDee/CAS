package org.pealfactory.strike.audio;

import org.pealfactory.strike.Constants;

import javax.sound.sampled.*;
import java.io.*;

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
public class WavBellSounds extends BellSounds implements LineListener
{
	public static final double[] PITCH_SCALE =
	    {1.0, Math.pow(2.0, 1.0/6), Math.pow(2.0, 2.0/6), Math.pow(2.0, 5.0/12), Math.pow(2.0, 7.0/12),
			Math.pow(2.0, 9.0/12), Math.pow(2.0, 11.0/12), 2.0, Math.pow(2.0, 7.0/6), Math.pow(2.0, 8.0/6),
			Math.pow(2.0, 17.0/12), Math.pow(2.0, 19.0/12), Math.pow(2.0, 21.0/12), Math.pow(2.0, 23.0/12),
	    Math.pow(2.0, 4.0), Math.pow(2.0, 13.0/6)};

	File fWavFile;
	Mixer fMixer;
	AudioFormat fFormat;
	byte[] fWavData;
	Clip[] fBells = new Clip[MAXNBELLS];

	public WavBellSounds(File wavFile)
	{
		fWavFile = wavFile;
	}

	public boolean init(int nbells)
	{
    if (!loadFile())
    	return false;
		fMixer = AudioSystem.getMixer(null);
		System.out.println("Using "+fMixer);
		for (int i=0; i<nbells; i++)
		{
			Clip clip = getClip((float)(PITCH_SCALE[nbells-i-1]*0.5));
			if (clip==null)
				return false;
			fBells[i] = clip;
		}
		//Clip clip = getClip(1.0f);
		//clip.start();
		return true;
	}

	public void play(int bell)
	{
		Clip c = fBells[bell-1];
		c.setFramePosition(0);
		c.start();
	}

	private Clip getClip(float sampleRateScale)
	{
		DataLine.Info clipInfo = new DataLine.Info(Clip.class, fFormat);
		Clip clip;
		try
		{
			clip = (Clip)fMixer.getLine(clipInfo);
			clip.open(fFormat, fWavData, 0, 102400);
			clip.addLineListener(this);
			FloatControl control = (FloatControl)clip.getControl(FloatControl.Type.SAMPLE_RATE);
			float sampleRate = control.getValue();
			control.setValue(sampleRate*sampleRateScale);
		}
		catch (LineUnavailableException e)
		{
			System.out.println("Failed to get Clip dataline: "+e);
			return null;
		}
		return clip;
	}

	private boolean loadFile()
	{
		AudioInputStream in = null;
		try
		{
			in = AudioSystem.getAudioInputStream(fWavFile);
      fFormat = in.getFormat();

			byte[] buffer = new byte[(int)in.getFrameLength()*100];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int c = in.read(buffer);
			while (c>=0)
			{
				baos.write(buffer, 0 , c);
				c = in.read(buffer);
			}
			fWavData = baos.toByteArray();
		}
		catch (UnsupportedAudioFileException e)
		{
			System.out.println("Cannot read audio file "+fWavFile+": "+e);
      return false;
		}
		catch (IOException e)
		{
			System.out.println("Error reading file "+fWavFile+": "+e);
      return false;
		}
		finally
		{
			if (in!=null)
			{
				try
				{
					in.close();
				}
				catch (IOException e)
				{
				}
			}
		}
		return true;
	}

	public void update(LineEvent event)
	{
		if (event.getType()==LineEvent.Type.STOP)
			fPlaying = false;
	}
}
