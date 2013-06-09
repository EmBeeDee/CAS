package org.pealfactory.strike.summary;

import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;

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
 */
public class SummaryTreeManager implements Runnable
{
	private Summariser fSummariser;
	private DefaultTreeModel fTreeModel;
	private DefaultMutableTreeNode fRoot;

	public SummaryTreeManager()
	{
		fRoot = new DefaultMutableTreeNode("Orders predicted by each analyser:");
		DefaultTreeModel treeModel = new DefaultTreeModel(fRoot);
		fTreeModel = treeModel;
	}

	public DefaultTreeModel getTreeModel()
	{
		return fTreeModel;
	}

	/**
	 * To be called on a worker thread.
	 * @param s
	 */
	public void newResults(Summariser s)
	{
		fSummariser = s;
		try
		{
			SwingUtilities.invokeAndWait(this);
		}
		catch (Exception e)
		{

		}
	}

	public void run()
	{
		SortedSet<CountedBandOrder> orders = fSummariser.getPopularOrders();
		fRoot.removeAllChildren();
		for (CountedBandOrder order: orders)
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(order);
			for (BandOrder bo: order.getAllMatchingResults())
			{
				node.add(new DefaultMutableTreeNode(getLeafText(bo)));
			}
			fRoot.add(node);
		}
		fTreeModel.nodeStructureChanged(fRoot);
	}

	private String getLeafText(BandOrder bo)
	{
		StringBuilder buf = new StringBuilder();
		buf.append("<html><table><tr><td width=100>");
		buf.append(bo.getAnalyserName());
		buf.append("</td><td>");
		buf.append(bo.getBandSpacingAsString(100, "."));
		buf.append("</td></tr></table></html>");
		return buf.toString();
	}
}
