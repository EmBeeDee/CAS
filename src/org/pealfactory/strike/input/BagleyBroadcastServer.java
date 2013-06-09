package org.pealfactory.strike.input;

import org.pealfactory.strike.data.Bong;

import java.net.ServerSocket;
import java.net.Socket;
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
public class BagleyBroadcastServer
{
	public final static int PORT = 11001;

	private ServerSocket fServerSocket;

	public static void main(String[] args)
	{
		try
		{
    	BagleyBroadcastServer server = new BagleyBroadcastServer(PORT);
			server.listenAndProcess();
		}
		catch (IOException e)
		{
			System.out.println("Failed to start server: "+e);
		}
	}

	public BagleyBroadcastServer(int port) throws IOException
	{
		fServerSocket = new ServerSocket(port);
	}

	public void listenAndProcess()
	{
		while (true)
		{
			try
			{
				Socket socket = fServerSocket.accept();
				Thread thread = new Thread(new OutputThread(socket));
				thread.start();
			}
			catch (IOException e)
			{
				System.out.println("Failed to accept connection: "+e);
			}
		}
	}

	class OutputThread implements Runnable
	{
		private Socket fSocket;
		private PrintWriter fOut;

		public OutputThread(Socket s) throws IOException
		{
			fSocket = s;
			fOut = new PrintWriter(new OutputStreamWriter(fSocket.getOutputStream()));
		}

		public void run()
		{
			System.out.println("Data load begins on socket "+fSocket);
			File bf = new File("peal.txt");
			BagleyBongInput inputter = null;
			try
			{
				inputter = new BagleyBongInput(bf.getName(), new FileReader(bf), new FileReader("oddstruck.txt"));
			}
			catch (IOException e)
			{
				System.out.println("Failed to load input data: "+e);
				return;
			}
			InputStageListener inputListener = new InputStageListener(){
				public void receiveBong(Bong bong)
				{
					fOut.println(bong.toString());
				}

				public void notifyInputComplete()
				{
					fOut.close();
				}

				public void notifyInputError(String msg)
				{
					System.out.println("Input Error: "+msg);
				}

			};
			inputter.startLoad(inputListener);
			System.out.println("Data load complete on socket "+fSocket);
			try
			{
				fSocket.close();
			}
			catch (IOException e)
			{
				System.out.println("Failed to close socket: "+e);
			}
		}
	}

}
