package org.pealfactory.strike.input;

import org.pealfactory.strike.data.Bong;
import org.pealfactory.strike.errorcorrection.*;
import org.pealfactory.strike.Constants;

import java.net.Socket;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

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
public class BagleyClient implements StrikingDataInput
{
	private Socket fSocket;
	private InputStageListener fInputListener;
	private boolean fClosed;

	public BagleyClient()
	{
	}

	public String getInputFormat()
	{
		return "Bagley";
	}

	public String getInputSource()
	{
		return "Capture";
	}

	public String getInputName()
	{
		return "Bagley Server";
	}

	/**
	 * Same error correcters as BagleyBongInput
	 * @return
	 */
	public List getErrorCorrecters()
	{
		List errorCorrectors = new ArrayList();
		errorCorrectors.add(new TimeOrderCorrecter(BagleyBongInput.MAXSEQUENCEERROR));
		return errorCorrectors;
	}

	public void startLoad(InputStageListener pipeline)
	{
		fInputListener = pipeline;
		LineNumberReader reader = null;
		try
		{
			fSocket = new Socket("localhost", BagleyBroadcastServer.PORT);
			reader = new LineNumberReader(new InputStreamReader(fSocket.getInputStream()));
		}
		catch (Exception e)
		{
      fInputListener.notifyInputError("Failed to open socket: "+e);
			return;
		}
		try
		{
	    String line = reader.readLine();
			while (line!=null)
			{
				Bong bong = Bong.fromString(line);
				fInputListener.receiveBong(bong);
				line = reader.readLine();
			}
			fClosed = true;
			fInputListener.notifyInputComplete();
			reader.close();
			fSocket.close();
		}
		catch (Exception e)
		{
			fInputListener.notifyInputError("Failed to read from socket: "+e);
		}
	}

	public boolean isOpen()
	{
		return fSocket!=null && !isClosed();
	}

	public boolean isClosed()
	{
		return fClosed;
	}

}
