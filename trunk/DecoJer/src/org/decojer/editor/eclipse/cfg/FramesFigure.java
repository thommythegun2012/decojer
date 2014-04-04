/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  André Pankraz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every Java Source Code
 * that is created using DecoJer.
 */
package org.decojer.editor.eclipse.cfg;

import org.decojer.cavaj.model.code.BB;
import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Insets;

/**
 * Frames Figure.
 *
 * @author André Pankraz
 */
public class FramesFigure extends Figure {

	private static final Border LEFT_BORDER = new AbstractBorder() {

		@Override
		public Insets getInsets(final IFigure figure) {
			return new Insets(0, 5, 0, 0);
		}

		@Override
		public void paint(final IFigure figure, final Graphics graphics, final Insets insets) {
			tempRect.setBounds(getPaintRectangle(figure, insets));
			tempRect.shrink(1, 0);
			graphics.drawLine(tempRect.getTopLeft(), tempRect.getBottomLeft());
		}

	};

	private static final Border LEFT_BORDER_3 = new AbstractBorder() {

		@Override
		public Insets getInsets(final IFigure figure) {
			return new Insets(0, 5, 0, 0);
		}

		@Override
		public void paint(final IFigure figure, final Graphics graphics, final Insets insets) {
			tempRect.setBounds(getPaintRectangle(figure, insets));
			tempRect.shrink(1, 0);
			graphics.setLineWidth(3);
			graphics.drawLine(tempRect.getTopLeft(), tempRect.getBottomLeft());
		}

	};

	/**
	 * Constructor.
	 *
	 * @param bb
	 *            BB
	 */
	public FramesFigure(final BB bb) {
		final int stackRegs = bb.getStackRegs();
		final String[][] frameInfos = bb.getFrameInfos();
		final String[] header = frameInfos[0];
		final GridLayout gridLayout = new GridLayout(header.length, false);
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
		setLayoutManager(gridLayout);
		for (final String[] row : frameInfos) {
			for (int j = 0; j < row.length; ++j) {
				final String str = row[j];
				final Label label = new Label(str == null ? "" : str);
				if (j == 1 || j - 1 == stackRegs) {
					label.setBorder(LEFT_BORDER_3);
				} else if (j != 0) {
					label.setBorder(LEFT_BORDER);
				}
				add(label);
			}
		}
	}

}