package org.pealfactory.strike.ui;

import org.pealfactory.strike.data.*;
import org.pealfactory.strike.Constants;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.font.*;
import java.awt.print.*;

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
public class AbelDisplay extends StrikingDisplay implements Constants
{
	public final static int EXTRA_WIDTH = 150;
	public final static int ROW_WIDTH = 300;
	public final static int SPEED_WIDTH = 200;
	public final static Color CORRECT_LINE_COLOUR = new Color(0,150,255);
	public final static Color GRID_COLOUR = new Color(150,150,150);

	private final static float MIN_ZOOM_TO_SHOW_BELLS = 0.3f;

	private final static Color BACKGROUND_HAND = new Color(242,242,242);
	private final static Color BACKGROUND_BACK = new Color(230,230,230);
	private final static Color BACKGROUND_HANDBAD = new Color(245,220,220);
	private final static Color BACKGROUND_BACKBAD = new Color(230,207,207);
	private final static Color BACKGROUND_HANDGOOD = new Color(230,245,230);
	private final static Color BACKGROUND_BACKGOOD = new Color(216,230,216);

	private Font fNormalBellFont;
	private Font fBoldBellFont;
	private Font fBigBellFont;
	private Font fTitleFont;
	private Font fNormalAdvancedViewFont;
	private Font fSmallAdvancedViewFont;

	private double fPixelsPerMs;
	private double fEffectivePixelsPerMs;
	private int fInterbellGap;
	private int fAvBackDuration;
	private int fMinDur;
	private int fMaxMinusMinDur;
	private int[] fThisActualX = new int[MAXNBELLS];
	private int[] fThisCorrectX = new int[MAXNBELLS];
	private int[] fLastPlace = new int[MAXNBELLS];
	private int[] fLastActualX = new int[MAXNBELLS];
	private int[] fLastCorrectX = new int[MAXNBELLS];
	private Graphics2D fG2D;
	private Insets fInsets;
	private Rectangle fClip;
	private int fCharWidth;
	private int fTitleFontHeight;
	private int fSmallFontHeight;
	private int fRowRight;
	private int fRowLeft;
	private int fY0;
	private boolean fInChanges;

	@Override
	public void setZoom(float zoom)
	{
		super.setZoom(zoom);
		setupFonts();
		revalidate();
		repaint();
	}

	public int getTotalWidth()
	{
		return zoomX(ROW_WIDTH);
	}

	protected void repaintRow(int row)
	{
		if (!measureUp())
			return;
		int y = getTopYFromRowNumber(row);
		repaint(fInsets.left, y, fRowRight+1000, fHeightPerRow*2);
	}

	/**
	 * Can return -1 or > max rows.
	 *
	 * @param y
	 * @return
	 */
	private int getRowNumberFromYOrdinate(int y)
	{
		return (y+fHeightPerRow-fY0)/fHeightPerRow;
	}

	private int getTopYFromRowNumber(int rowNumber)
	{
		return fY0+(rowNumber-1)*fHeightPerRow;
	}

	protected void scrollToRow(int row)
	{
		if (!measureUp())
			return;
		int y = getTopYFromRowNumber(row);
		Rectangle r = new Rectangle(0, y-fHeightPerRow, fRowRight, fHeightPerRow*4);
		scrollRectToVisible(r);
	}

	private boolean measureUp()
	{
		if (!fRowsLoaded)
			return false;
		fInsets = getInsets();
		if (fNormalBellFont==null)
			setupFonts();

		TouchStats.HandBackWholeInt minDuration = fData.getMinDuration(false);
		TouchStats.HandBackWholeInt maxDuration = fData.getMaxDuration(false);
		fAvBackDuration = (minDuration.back+maxDuration.back)/2;
		fMinDur = Math.min(minDuration.hand, minDuration.back);
		fMaxMinusMinDur = Math.max(1, Math.max(maxDuration.hand, maxDuration.back) - fMinDur);

		fPixelsPerMs = ((double)fWidthPerBell*fData.getNBells()*fZoomX)/fAvBackDuration;

		// Would really like to use the title font height in the calculation of fY0, since it must reserve space for the title.
		// However, fTitleFontHeight can only be determined once we get a Graphics object and hence a FontRenderContext.
		// These are only available in paintComponent(), not in other methods which might like to call measureUp(), e.g.
		// printing or repaint rectangle invalidation. The safest thing to do is to assume a fixed space for the title,
		// so it is set here to 20 pixels. If you make the title font bigger, this might not be enough!
		fY0 = 20+fHeightPerRow+fInsets.top;
		fRowRight = fWidthPerBell*2 + fInsets.left + fWidthPerBell*fData.getNBells();
		return true;
	}

	private void setupFonts()
	{
		Font baseFont = getFont();
		// Title font is never scaled by zoom
		fTitleFont = baseFont.deriveFont(Font.BOLD, 12.0f);

		// Advanced-view fonts are scaled only slightly - same as X zoom ratio - and not less than 75%
		float z = Math.max(0.75f, fZoomX);
		fNormalAdvancedViewFont = baseFont.deriveFont(12.0f*z);
		fSmallAdvancedViewFont = baseFont.deriveFont(10.0f*z);

		// Other fonts are scaled by a font factor between X (slight) and Y (actual zoom) ratios,
		// But note they are not usually displayed below a zoom ratio of 30%
		fNormalBellFont = baseFont.deriveFont(12.0f*fZoomFont);
		fBoldBellFont = fNormalBellFont.deriveFont(Font.BOLD);
		fBigBellFont = baseFont.deriveFont(16.0f*fZoomFont);
	}

	protected void paintComponent(Graphics g)
	{
		if (isOpaque())
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		fClip = g.getClipBounds();
		if (!measureUp())
			return;

		fG2D = (Graphics2D)g;
		Rectangle2D charSize = fNormalBellFont.getStringBounds("0", fG2D.getFontRenderContext());
		fCharWidth = round(charSize.getWidth()/2);
		Rectangle2D titleCharSize = fTitleFont.getStringBounds("0", fG2D.getFontRenderContext());
		fTitleFontHeight = round(titleCharSize.getHeight());
		Rectangle2D smallCharSize = fSmallAdvancedViewFont.getStringBounds("0", fG2D.getFontRenderContext());
		fSmallFontHeight = round(smallCharSize.getHeight()*0.6);

		for (int i=0; i<fData.getNBells(); i++)
		{
			fLastActualX[i] = -1;
			fLastCorrectX[i] = -1;
		}
		fInChanges = false;

		// Do column headings
		if (fClip.y<=fY0)
			renderColumnHeadings();

		// First loop does background, annotations and lines.
		fG2D.setFont(fNormalBellFont);
		fG2D.setColor(Color.black);
		int firstRow = Math.max(0, getRowNumberFromYOrdinate(fClip.y)-1);
		int y = fY0+firstRow*fHeightPerRow;
		for (int i=firstRow; i<fData.getNRows(); i++)
    {
	    AveragedRow r = fData.getRow(i);
	    if (r!=null)
	    {
		    calcEffectivePixelsPerMs(r);

				fRowLeft = fRowRight - (int)(r.getRowDuration()*fEffectivePixelsPerMs);
				fInterbellGap = (int)(r.getAveragedGap()*fEffectivePixelsPerMs);

		    // Draw row and duration backgrounds
		    drawRowBackground(y, r, i, i>0? fData.getRow(i-1):null);

		    // Print row duration and interbell+handstroke gaps every ten rows
		    annotateRows(y, r, i);

		    // Connect bells up with lines
		    drawLines(y, r);
	    }
	    if (y-fHeightPerRow > fClip.y+fClip.height)
	    	break;
	    y+= fHeightPerRow;
    }

		// Second loop does bell numbers, to ensure they overwrite lines.
		y = fY0+firstRow*fHeightPerRow;
		for (int i=firstRow; i<fData.getNRows(); i++)
    {
	    AveragedRow r = fData.getRow(i);
	    if (r!=null)
	    {
		    calcEffectivePixelsPerMs(r);
				drawBellNumbers(y, r, i);
	    }
	    if (y-fHeightPerRow > fClip.y+fClip.height)
	    	break;
	    y+= fHeightPerRow;
    }

    fG2D.dispose();
	}

	private void renderColumnHeadings()
	{
		fG2D.setFont(fTitleFont);
		fG2D.setColor(Color.blue);
		fG2D.drawString("Row", fInsets.left+1,  fInsets.top+fTitleFontHeight);
		fG2D.drawString("Striking Graph", fRowLeft+(fRowRight-fRowLeft)/2-20,  fInsets.top+fTitleFontHeight);
		fG2D.drawString("Row Length Graph", fRowRight+fInterbellGap+10,  fInsets.top+fTitleFontHeight);
	}

	private void drawRowBackground(int y, AveragedRow row, int rowNumber, AveragedRow prevRow)
	{
		int durPixels = round((SPEED_WIDTH*fZoomX*(row.getRowDuration()-fMinDur))/fMaxMinusMinDur);
		Color rowBackground = getRowColour(row, rowNumber);
    int depress = rowNumber==fPlaybackStartRow? 1:0;

		// Fill rectangle for row length, at right of display
		paintRectangle(fRowRight+fInterbellGap+zoomX(10), y-fHeightPerRow, durPixels, fHeightPerRow, rowBackground, depress);
		// Fill rectangle for row itself - always a fixed, normalised length
		paintRectangle(fRowLeft+fInterbellGap, y-fHeightPerRow, fRowRight-fRowLeft, fHeightPerRow, rowBackground, depress);

    // Draw marker for end of previous row - shows up if calculated row duration doesn't match difference
    // between row end times.
    if (prevRow!=null)
    {
	    int rowStart = row.getRowEndTime()-row.getRowDuration();
	    int delta = rowStart-prevRow.getRowEndTime();
	    if (delta!=0)
	    {
		    int h = round(fHeightPerRow*0.25f);
		    delta*= fEffectivePixelsPerMs;
		    if (delta>0)
		    {
			    // Gap between end of last row and start of this row.
			    paintRectangle(fRowLeft+fInterbellGap-delta, y-(h+fHeightPerRow)/2, delta, h, rowBackground, depress*2);
		    }
		    else
		    {
			    // Last row cuts into this one.
			    paintRectangle(fRowLeft+fInterbellGap, y-(h+fHeightPerRow)/2, -delta, h, getBackground(), -depress);
		    }
	    }
    }
	}

	private void paintRectangle(int x, int y, int width, int height, Color c, int depressedBorder)
	{
		fG2D.setColor(c);
		if (depressedBorder==0)
			fG2D.fillRect(x, y, width, height);
		else if (depressedBorder>0)
		{
			fG2D.fillRect(x, y, width+1, height);
			if (depressedBorder==2)
				width--;
			fG2D.setColor(Color.WHITE);
			fG2D.drawLine(x, y+height-1, x+width, y+height-1);
			fG2D.drawLine(x, y+height-2, x+width, y+height-2);
			if (depressedBorder<2)
			{
      	fG2D.drawLine(x+width, y, x+width, y+height-1);
				fG2D.drawLine(x+width-1, y, x+width-1, y+height-1);
			}
			fG2D.setColor(Color.LIGHT_GRAY);
			if (depressedBorder==2)
				width++;
			fG2D.drawLine(x, y, x+width, y);
     	fG2D.drawLine(x, y, x, y+height-1);
		}
		else if (depressedBorder<0)
		{
			width+= 2;
			height+= 2;
			fG2D.fillRect(x, y, width, height);
			y--;
			fG2D.setColor(Color.WHITE);
			fG2D.drawLine(x, y, x+width, y);
			fG2D.drawLine(x, y+1, x+width, y+1);
			fG2D.setColor(Color.LIGHT_GRAY);
			fG2D.drawLine(x, y+height, x+width, y+height);
			fG2D.drawLine(x+width, y, x+width, y+height);
		}
	}

	private void annotateRows(int y, AveragedRow row, int i)
	{
		fG2D.setColor(Color.black);
		fG2D.setFont(fNormalAdvancedViewFont);
		if (!row.isHandstroke())
		{
			if (i%10==9 || fZoomY>MIN_ZOOM_TO_SHOW_BELLS)
				// Draw row number on every even (backstroke) row, or every tenth row for higher levels of zoom-out
				fG2D.drawString(""+(i+1), fInsets.left,  y);
		}
		if (fAdvancedView && i%10==0)
		{
			y-= fHeightPerRow/2;
			// Every tenth row, display row duration...
			int x = fRowRight+(fZoomY>MIN_ZOOM_TO_SHOW_BELLS? fInterbellGap+zoomX(20): SPEED_WIDTH);
			fG2D.drawString(""+row.getRowDuration()+"ms" , x, y+fTitleFontHeight/3);

			if (fZoomY>MIN_ZOOM_TO_SHOW_BELLS)
			{
				fG2D.setFont(fSmallAdvancedViewFont);
				// ...and handstroke gap.
				fG2D.drawString(""+(int)(row.getAveragedGap()*row.getHandstrokeGap()), fRowLeft, y);
				fG2D.drawString(" ms", fRowLeft, y+fSmallFontHeight);
				// ...and average gap...
				int xAG = fRowRight+fInterbellGap+zoomX(100);
				fG2D.drawString(""+(int)(row.getAveragedGap()), xAG, y);
				fG2D.drawString(" ms", xAG, y+fSmallFontHeight);
			}
		}
		fG2D.setFont(fNormalBellFont);
	}

	private void drawLines(int y, AveragedRow row)
	{
		// Loop over row - draw lines
		for (int j=0; j<row.getRowSize(); j++)
		{
			fThisActualX[j] = strikeTimeToPixelX(row, row.getStrikeTime(j+1));
			fThisCorrectX[j] = strikeTimeToPixelX(row, row.getCorrectStrikeTime(j+1));

			int b = row.getBellAt(j+1);
			if (fLastActualX[j]>0)
			{
				if ((j&1)==1)
				{
					// For even bell position, draw straight "perfect" place line down the screen.
					fG2D.setColor(Color.lightGray);
					fG2D.drawLine(fLastCorrectX[j], y-fHeightPerRow*3/2, fThisCorrectX[j], y-fHeightPerRow/2);
				}
				if (!fInChangesOnly || fInChanges)
				{
					if (b==fHighlightedBell)
					{
						// For highlighted bell, draw blue line indicating perfect ringing position
						fG2D.setColor(CORRECT_LINE_COLOUR);
						fG2D.drawLine(fLastCorrectX[fLastPlace[b-1]], y-fHeightPerRow*3/2, fThisCorrectX[j], y-fHeightPerRow/2);
						fG2D.setColor(Color.black);
					}
					else
						fG2D.setColor(GRID_COLOUR);
					// Draw lines for each bell, joining up actual strike positions
					fG2D.drawLine(fLastActualX[fLastPlace[b-1]], y-fHeightPerRow*3/2, fThisActualX[j], y-fHeightPerRow/2);
				}
			}
		}
		fInChanges = row.isInChanges();
		// Loop over row - update previous bell x ordinates (actual and correct) for lines to next row.
		for (int j=0; j<row.getRowSize(); j++)
		{
			int b = row.getBellAt(j+1);
			fLastPlace[b-1] = j;
			fLastActualX[j] = fThisActualX[j];
			fLastCorrectX[j] = fThisCorrectX[j];
		}
	}

	private void drawBellNumbers(int y, AveragedRow row, int rowNumber)
	{
		y-= zoomY(5);
		int xOff = -fCharWidth;
		if (rowNumber==fPlaybackStartRow)
		{
			y++;
			xOff++;
		}
		// Loop over row - draw bell numbers
		for (int j=0; j<row.getRowSize(); j++)
		{
			int b = row.getBellAt(j+1);
			int x = strikeTimeToPixelX(row, row.getStrikeTime(j+1));
			String s = BELL_CHARS.substring(b-1, b);
			fG2D.setColor(Color.black);
			if (rowNumber==fNowPlayingRow && b==fNowPlayingBell)
			{
				// If audio playing this bell, draw big bell number
				fG2D.setFont(fBigBellFont);
				fG2D.drawString(s, x+xOff-1, y+1);
				fG2D.setFont(fNormalBellFont);
			}
			else if (fZoomY>MIN_ZOOM_TO_SHOW_BELLS)
			{
				if (row.getStrikeTime(j+1)==row.getCorrectStrikeTime(j+1))
				{
					// For bells in exactly the right place, draw bold bell number
					fG2D.setFont(fBoldBellFont);
					fG2D.drawString(s, x+xOff, y);
					fG2D.setFont(fNormalBellFont);
				}
				else
				{
					// Draw bell number
					fG2D.drawString(s, x+xOff, y);
				}
			}
		}

	}

	private void calcEffectivePixelsPerMs(AveragedRow row)
	{
		if (row.isHandstroke())
		{
			// fEffectivePixelsPerMs is the value needed to make this row the same pixel width as the average row (i.e. normalised width)
			fEffectivePixelsPerMs = fPixelsPerMs*fAvBackDuration/(row.getRowDuration()-row.getAveragedGap()*row.getHandstrokeGap());
		}
		else
		{
			fEffectivePixelsPerMs = fPixelsPerMs*fAvBackDuration/row.getRowDuration();
		}

	}

	private int strikeTimeToPixelX(AveragedRow row, int strikeTime)
	{
		int t = row.getRowEndTime()-strikeTime;
		t*= fEffectivePixelsPerMs;
		return fRowRight - t;
	}

	private Color getRowColour(AveragedRow row, int rowNumber)
	{
		Color rowBackground;
		if (row.isHandstroke())
		{
			if (row.isGood())
				rowBackground = BACKGROUND_HANDGOOD;
			else if (row.isBad())
				rowBackground = BACKGROUND_HANDBAD;
			else
				rowBackground =  BACKGROUND_HAND;
		}
		else
		{
			if (row.isGood())
				rowBackground = BACKGROUND_BACKGOOD;
			else if (row.isBad())
				rowBackground = BACKGROUND_BACKBAD;
			else
				rowBackground =  BACKGROUND_BACK;
		}
		if (rowNumber==fNowPlayingRow)
		{
			float[] col = rowBackground.getRGBColorComponents(null);
			int best = 0;
			for (int i=1; i<3; i++)
				if (col[i]>=col[best])
					best = i;
			for (int i=0; i<3; i++)
				if (i==best)
					col[i] = Math.min(col[i]*1.2f, 1.0f);
				else
					col[i] = col[i]*0.9f;
			rowBackground = new Color(col[0], col[1], col[2]);
		/*
			float rowPlayingDarken = 0.8f/256;
    	rowBackground = new Color(rowBackground.getRed()*rowPlayingDarken, rowBackground.getGreen()*rowPlayingDarken, rowBackground.getBlue()*rowPlayingDarken);
	   */
		}
		return rowBackground;
	}

	public Dimension getPreferredSize()
	{
		int x = EXTRA_WIDTH+ROW_WIDTH+SPEED_WIDTH;
		if (!fRowsLoaded)
			return new Dimension(x, 800);
		int y = fHeightPerRow*(fData.getNRows()+2);
		return new Dimension(x, y);
	}

	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	public void mouseClicked(MouseEvent e)
	{
		if (fPlaybackStartRow>=0)
			repaintRow(fPlaybackStartRow);
		int row = getRowNumberFromYOrdinate(e.getY());
		if (row==fPlaybackStartRow)
			fPlaybackStartRow = -1;
		else
		{
			fPlaybackStartRow = row;
			if (fPlaybackStartRow>=0)
				repaintRow(fPlaybackStartRow);
		}
	}

	private int fPageBreaks[];
	private int fPageHeaderHeights[];

	public int pagesToPrint(CASPageFormat pf)
	{
		int npages = 1;
		if (measureUp())
		{
			// First calculate how much space is available on normal pages
			int pageHeight = (int)pf.pageFormat.getImageableHeight();
			// Header and footer takes away height from every page
			pageHeight-= pf.headerHeight+pf.footerHeight;
			if (pageHeight<=0)
				return 0;

			// First page is different - less space because of initial reserved vertical space
			// Subtract reserved height from the first page (or pages if more than a page's worth of height is reserved)
			int firstPageHeight = pageHeight-pf.initialReservedHeight;
			int reservedPages = 0;
			while (firstPageHeight<=fY0+fHeightPerRow)
			{
				reservedPages++;
				firstPageHeight+= pageHeight;
			}
			npages+= reservedPages;

			// Now see how many rows fit on each page
			int rowsOnFirstPage = (firstPageHeight-fY0)/fHeightPerRow;
			int rowsPerPage = pageHeight/fHeightPerRow;

			// Using the total number of rows, can now calculate the number of pages required.
			int nrows = fData.getNRows();
			nrows-= rowsOnFirstPage;
			if (nrows>0)
				npages+= 1 + nrows/rowsPerPage;

			// Finally set up page break arrays, which will be used to set origin and clip bounds for each page when printing.
			fPageBreaks = new int[npages+1];
			fPageHeaderHeights = new int[npages];
			int i = reservedPages;
			// First printing page is different - need to remember to make space for any reserved vertical space.
			fPageHeaderHeights[i] = pf.headerHeight + (pageHeight-firstPageHeight);
			fPageBreaks[i++] = 0;
			fPageBreaks[i++] = fY0+fHeightPerRow*rowsOnFirstPage;
			while (i<=npages)
			{
				fPageHeaderHeights[i-1] = pf.headerHeight;
				fPageBreaks[i] = fPageBreaks[i-1]+fHeightPerRow*rowsPerPage;
				i++;
			}
		}
		return npages;
	}

	public void print(Graphics2D g2d, CASPageFormat pf, int page) throws PrinterException
	{
		if (measureUp())
		{
			int top = fPageBreaks[page];
			int bot = fPageBreaks[page+1];
			// bot==0 implies a page used by reserved initial space, i.e. our rendering mustn't start yet.
			if (bot==0)
				return;
			// Translate the Graphics2D instance so that a pixel written to (0,top) will appear at the top of *this* page,
			// i.e. (0,0). Take into account any vertical header space
			g2d.translate(0.0f, (float)fPageHeaderHeights[page]-top);
			// Set the clipping area to be between the top and bottom page breaks
			g2d.setClip(0, top, (int)pf.pageFormat.getImageableWidth(), bot-top);
			// Use normal painting to print
			paintComponent(g2d);
			// Undo translation on the g2d.
			g2d.translate(0.0f, (float)top);
		}
	}
}
