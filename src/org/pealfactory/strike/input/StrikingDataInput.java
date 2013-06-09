package org.pealfactory.strike.input;

import org.pealfactory.strike.errorcorrection.*;
import org.pealfactory.strike.pipeline.Pipeline;

import java.util.List;

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
 *
 * @author MBD
 */
public interface StrikingDataInput
{
	/** Returns a string describing the input format */
	public String getInputFormat();

	/** Type of source - e.g. file, or TCP connection */
	public String getInputSource();

	/** Specific name of input source */
	public String getInputName();

	/** List of ErrorCorrecter instances needed by this input source */
	public List<ErrorCorrecter> getErrorCorrecters();

	/** Sets the pipeline to which Bongs should be delivered. Data load should commence immediately after this has been set */
	public void startLoad(InputStageListener pipeline);

	/** Returns true if the input is in progress and delivering Bongs */
	public boolean isOpen();

	/** Returns true if the input has completed and no more Bongs are to be delivered */
	public boolean isClosed();

}
