package org.pealfactory.strike.ui;

import org.pealfactory.strike.input.InputSource;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

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
public interface CASContainer extends InputSource, RootPaneContainer
{
	void setTitle(String title);

	void openFile();

	void openNewCASWindow();

	void openNewCASWindow(String fileToLoad);

	void openSummaryWindow();

  void export();

	void closeWindow();
}