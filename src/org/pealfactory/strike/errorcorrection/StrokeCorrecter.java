package org.pealfactory.strike.errorcorrection;

import org.pealfactory.strike.data.Bong;
import org.pealfactory.strike.Constants;

/**
 * An error corrector which attempts to assign correct handstroke/backstroke flags to each incoming row;
 * it is necessary for Lowndes-format input files, which do not guarantee correct stroke information.
 * The StrokeCorrector assumes that any "row overlaps" have been corrected, i.e. that the bongs are arriving
 * in sets of rows, without any interleaving caused by, for example, a quick leading bell striking before the
 * tenor in the previous row. This is done by the RowOverlapCorrector.
 * <p>
 * If we can assume rows are not interleaved, it is simple enough to detect the start of a new row: it is when
 * we see a bell which has already rung in the previous row. We swap the stroke at this point.
 * However, a complicating factor is the possibility of missing sensor bongs at the start of a row.
 * Consider these two changes:
 * <pre>
 *    23456 H
 *   123456 B
 * </pre>
 * Here the treble strike has been missed from the start of the first, handstroke, row. Unfortunately, this
 * means that when the first treble bong does come through, the error corrector assumes that it must be
 * at the end of the handstroke, not the start of the next backstroke. In fact there is no way, in the absence
 * of other information, to come to any better conclusion. Hence, the job of sorting out these problems is
 * left to a later corrector, the LeadLieCorrector.
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
public class StrokeCorrecter extends ErrorCorrectionHelper
{
	private Bong[] fBongs = new Bong[Constants.MAXNBELLS];

	private boolean fHandstrokeStart = true;

	private int fStroke;

	public StrokeCorrecter()
	{
		this(true);
	}

	public StrokeCorrecter(boolean handstrokeStart)
	{
		fHandstrokeStart = handstrokeStart;
		fStroke = fHandstrokeStart? Bong.HANDSTROKE : Bong.BACKSTROKE;
	}

	public void receiveBong(Bong bong)
	{
		Bong prevBong = fBongs[bong.bell-1];
		if (prevBong!=null)
		{
			fStroke = -fStroke;
			for (int i=0; i<fBongs.length; i++)
				fBongs[i] = null;
		}
		fBongs[bong.bell-1] = bong;
		bong.stroke = fStroke;
		fNextStage.receiveBong(bong);
	}

	public void notifyInputComplete()
	{
		fNextStage.notifyInputComplete();
	}
}
