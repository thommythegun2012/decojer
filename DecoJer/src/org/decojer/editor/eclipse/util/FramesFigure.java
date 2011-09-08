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
package org.decojer.editor.eclipse.util;

import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.vm.intermediate.Frame;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.decojer.cavaj.model.vm.intermediate.Var;
import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
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

	private static final GridData GRID_DATA = new GridData(
			GridData.FILL_HORIZONTAL);

	private static final Border LEFT_BORDER = new AbstractBorder() {

		@Override
		public Insets getInsets(final IFigure figure) {
			return new Insets(0, 5, 0, 0);
		}

		@Override
		public void paint(final IFigure figure, final Graphics graphics,
				final Insets insets) {
			tempRect.setBounds(getPaintRectangle(figure, insets));
			tempRect.shrink(1, 0);
			graphics.drawLine(tempRect.getTopLeft(), tempRect.getBottomLeft());
		}

	};

	/**
	 * Constructor.
	 * 
	 * @param bb
	 *            basic block
	 */
	public FramesFigure(final BB bb) {
		final int maxRegs = bb.getCfg().getMaxRegs();
		final int maxStack = bb.getCfg().getMaxStack();
		final GridLayout gridLayout = new GridLayout(1 + maxRegs + maxStack,
				false);
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
		setLayoutManager(gridLayout);

		for (final Operation operation : bb.getOperations()) {
			final Frame frame = bb.getCfg().getInFrame(operation);
			add(new Label(operation.getClass().getSimpleName() + " "));
			final int regsSize = frame.getRegsSize();
			for (int index = 0; index < regsSize; ++index) {
				final Var var = frame.getReg(index);
				final Label label = new Label(var == null ? "    "
						: var.toString());
				label.setBorder(LEFT_BORDER);
				add(label);
			}
			for (int index = maxStack; index-- > 0;) {
				final Label label = new Label(index >= frame.getStackTop()
						|| frame.getStack(index) == null ? "    " : frame
						.getStack(index).toString());
				label.setBorder(LEFT_BORDER);
				add(label);
			}
		}

		for (int i = 0; i < maxStack; ++i) {
			add(new Label("s" + i), GRID_DATA, 0);
		}
		for (int i = maxRegs; i-- > 0;) {
			add(new Label("r" + i), GRID_DATA, 0);
		}
		add(new Label(""), 0);
	}
}