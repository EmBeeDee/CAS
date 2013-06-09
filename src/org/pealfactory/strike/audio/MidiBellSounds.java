package org.pealfactory.strike.audio;

import javax.sound.midi.*;
import java.util.Map;
import java.util.HashMap;

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
public class MidiBellSounds extends BellSounds
{
	public static final String META_ROW_START = "R";
	public static final String META_BELL = "B";

	/** Scale based on middle C as tenor */
	public static final int[] PITCH_SCALE = {60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84, 86};
	public static String[] PITCHES = {"G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#"};

	private int fNBells;
	private Sequencer fSequencer;
	private Sequence fSequence;
	private Synthesizer fSynthesizer;
	private Receiver fMidiOut;
	private MetaEventListener fListener;
	private int fPitchIncrease;

	public MidiBellSounds(MetaEventListener listener)
	{
		fListener = listener;
		fPitchIncrease = 0;
	}

  public boolean init(int nbells)
	{
		fNBells = nbells;
		try
		{
			fSequencer = MidiSystem.getSequencer();
			fSequencer.addMetaEventListener(fListener);
			fSynthesizer = MidiSystem.getSynthesizer();
			fMidiOut = fSynthesizer.getReceiver();
			fSequencer.getTransmitter().setReceiver(fMidiOut);

			/*
			System.out.println(" Listing available midi devices: ");
			MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
			for (int i=0; i<info.length; i++)
			{
				Class c = MidiSystem.getMidiDevice(info[i]).getClass();
				System.out.println(" MIDI device "+i+": "+info[i]+" is a "+c);
			}
			System.out.println("Using Sequencer "+fSequencer.getClass()+" and receiver "+fMidiOut.getClass());
			*/
		}
		catch (MidiUnavailableException e)
		{
			System.out.println("Could not obtain MIDI device: "+e);
			return false;
		}
		return true;
	}

	/**
	 * Play one bell sound immediately, bypassing Sequencer
	 * @param bell
	 */
	public void play(int bell)
	{
		MidiEvent evt;
		try
		{
			evt = createBellNote(bell, 0);
			fMidiOut.send(evt.getMessage(), -1);
		}
		catch (InvalidMidiDataException e)
		{
			System.out.println("Failed to play note; "+e);
		}
	}

	public void setPlaybackSpeed(float speed)
	{
		fSequencer.setTempoFactor(speed);
	}

	public void setTenorPitch(int pitch)
	{
		fPitchIncrease = pitch - 5;
	}

	/**
	 * Plays sequence
	 */
	public void playSequence(float speed)
	{
		try
		{
			fSequencer.open();
			fSynthesizer.open();
			//fSynthesizer.loadAllInstruments(fSynthesizer.getDefaultSoundbank());
			Instrument[] instruments = fSynthesizer.getAvailableInstruments();
			/*
			System.out.println("Instruments: "+fSynthesizer.getLoadedInstruments().length+", "+fSynthesizer.getAvailableInstruments().length);
			for (int i=0; i<instruments.length; i++)
				System.out.println("Instrument "+i+": "+instruments[i].getName()+", "+instruments[i].getPatch().getBank()+", "+instruments[i].getPatch().getProgram());
			*/
			if (instruments.length>180)
			{
				//System.out.println("Remap: "+fSynthesizer.remapInstrument(instruments[14], instruments[157]));
				//fSynthesizer.unloadInstrument(instruments[14]);
			}
		}
		catch (MidiUnavailableException e)
		{
			System.out.println("Error opening sequencer: "+e);
			return;
		}

		try
		{
			fSequencer.setSequence(fSequence);
			setPlaybackSpeed(speed);
			fSequencer.start();
		}
		catch (InvalidMidiDataException e)
		{
			System.out.println("Error playing sequence: "+e);
		}
	}

	public void stop()
	{
		fSequencer.stop();
		fSequencer.close();
		fSynthesizer.close();
		fMidiOut.close();
	}

	public void meta(MetaMessage meta)
	{
		if (meta.getType()==0x2F)
		{
			System.out.println("End of track");
			fSequencer.close();
		}
	}

	public boolean createSequence()
	{
		try
		{
			// Create a sequence with microsecond timing resolution
			// (Although this doesn't seem to have any effect on tick resolution - ticks in MidiEvents always microsecond
			fSequence = new Sequence(Sequence.SMPTE_25, 40000);
			// Create track and add program change to bell sound
			Track track = fSequence.createTrack();
			//track.add(createBankChange(1, 0));
			track.add(createProgramChange(14, 0));
		}
		catch (InvalidMidiDataException e)
		{
			System.out.println("Error creating sequence: "+e);
			return false;
		}
		return true;
	}

	public boolean addStrikeToSequence(int bell, long millis)
	{
		try
		{
			fSequence.getTracks()[0].add(createBellMarker(bell, millis));
			fSequence.getTracks()[0].add(createBellNote(bell, millis));
			fSequence.getTracks()[0].add(createBellNoteOff(bell, millis+2000));
		}
		catch (InvalidMidiDataException e)
		{
			System.out.println("Failed to create Midi note: "+e);
			return false;
		}
		return true;
	}

	public boolean addRowStartMarkToSequence(int rowN, long millis)
	{
		try
		{
			fSequence.getTracks()[0].add(createRowStartMarker(rowN, millis));
		}
		catch (InvalidMidiDataException e)
		{
			System.out.println("Failed to create Midi marker: "+e);
			return false;
		}
		return true;
	}

	/**
	 * Doesn't appear to work!
	 *
	 * @param bank
	 * @param millis
	 * @return
	 * @throws InvalidMidiDataException
	 */
	private MidiEvent createBankChange(int bank, long millis) throws InvalidMidiDataException
	{
		ShortMessage msg = new ShortMessage();
		msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 0, bank);
		return new MidiEvent(msg, millis*1000);
	}

	private MidiEvent createProgramChange(int instrument, long millis) throws InvalidMidiDataException
	{
		ShortMessage msg = new ShortMessage();
		msg.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);
		return new MidiEvent(msg, millis*1000);
	}

	private MidiEvent createBellNote(int bell, long millis) throws InvalidMidiDataException
	{
		ShortMessage msg = new ShortMessage();
		msg.setMessage(ShortMessage.NOTE_ON, 0, PITCH_SCALE[fNBells-bell]+fPitchIncrease, 100);
		return new MidiEvent(msg, millis*1000);
	}

	private MidiEvent createBellNoteOff(int bell, long millis) throws InvalidMidiDataException
	{
		ShortMessage msg = new ShortMessage();
		msg.setMessage(ShortMessage.NOTE_OFF, 0, PITCH_SCALE[fNBells-bell]+fPitchIncrease, 100);
		return new MidiEvent(msg, millis*1000);
	}

	private MidiEvent createRowStartMarker(int rowNumber, long millis) throws InvalidMidiDataException
	{
		MetaMessage msg = new MetaMessage();
		byte[] data = (META_ROW_START+rowNumber).getBytes();
		msg.setMessage(6, data, data.length);
		return new MidiEvent(msg, millis*1000);
	}

	private MidiEvent createBellMarker(int bell, long millis) throws InvalidMidiDataException
	{
		MetaMessage msg = new MetaMessage();
		byte[] data = (META_BELL+bell).getBytes();
		msg.setMessage(6, data, data.length);
		return new MidiEvent(msg, millis*1000);
	}

	/**
	 * Length of entire sequence, microseconds.
	 *
	 * @return
	 */
  public long getPlaybackLength()
	{
		return fSequence.getMicrosecondLength();
	}
}
