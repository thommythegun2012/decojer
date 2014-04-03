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

import java.util.IdentityHashMap;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.Container;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.Frame;
import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.transformers.TrCalculatePostorder;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polyline;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphItem;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;

import com.google.common.base.Strings;

/**
 * CFG Viewer.
 *
 * @author André Pankraz
 */
@Slf4j
public class CfgViewer extends Composite {

	private final Button cfgAntialiasingCheckbox;

	private final Combo cfgViewModeCombo;

	private final Graph graph;

	private Container selectedD;

	/**
	 * Constructor.
	 *
	 * @param parent
	 *            parent composite
	 * @param style
	 *            component style
	 */
	public CfgViewer(@Nonnull final Composite parent, final int style) {
		super(parent, style);
		final GridLayout layout = new GridLayout(2, false);
		setLayout(layout);

		this.cfgAntialiasingCheckbox = new Button(this, SWT.CHECK);
		GridData gridData = new GridData();
		this.cfgAntialiasingCheckbox.setLayoutData(gridData);
		this.cfgAntialiasingCheckbox.setText("Antialiasing");
		this.cfgAntialiasingCheckbox.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				initGraph();
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				initGraph();
			}

		});
		this.cfgViewModeCombo = new Combo(this, SWT.READ_ONLY);
		this.cfgViewModeCombo.setItems(new String[] { "Data Flow Analysis", "Java Expressions",
				"Control Flow Analysis", "Control Flow Statements" });
		this.cfgViewModeCombo.setText("Control Flow Statements");
		this.cfgViewModeCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				initGraph();
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				initGraph();
			}

		});
		gridData = new GridData();
		this.cfgViewModeCombo.setLayoutData(gridData);
		// draw graph
		// Graph will hold all other objects
		this.graph = new Graph(this, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		this.graph.setLayoutData(gridData);
		this.graph.setLayoutAlgorithm(new HierarchicalLayoutAlgorithm(
				LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
		this.graph.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				for (final GraphItem g : (List<GraphItem>) ((Graph) e.widget).getSelection()) {
					final Object data = g.getData();
					if (data instanceof BB) {
						log.info(renderBbInfo((BB) data));
					}
				}
			}

		});
	}

	private GraphNode addToGraph(@Nonnull final BB bb,
			@Nonnull final IdentityHashMap<BB, GraphNode> map) {
		final GraphNode node = new GraphNode(this.graph, SWT.NONE, bb.toString(), bb);
		if (bb.getStruct() != null) {
			node.setTooltip(new Label(bb.getStruct().toString()));
		} else if (bb.getCfg().isFrames()) {
			node.setTooltip(new FramesFigure(bb));
		} else {
			node.setTooltip(null);
		}
		map.put(bb, node);

		for (final E out : bb.getOuts()) {
			GraphNode succNode = map.get(out.getEnd());
			if (succNode == null) {
				succNode = addToGraph(out.getEnd(), map);
			}
			final GraphConnection connection = new GraphConnection(this.graph,
					ZestStyles.CONNECTIONS_DIRECTED, node, succNode);
			if (this.cfgAntialiasingCheckbox.getSelection()) {
				((Polyline) connection.getConnectionFigure()).setAntialias(SWT.ON);
			}
			connection.setText(out.getValueString());
			if (out.isBack()) {
				connection.setCurveDepth(50);
				connection.setLineColor(ColorConstants.red);
			} else if (out.isCatch()) {
				connection.setLineColor(ColorConstants.yellow);
			}
		}
		return node;
	}

	public void initGraph() {
		CFG cfg = null;
		if (this.selectedD instanceof M) {
			cfg = ((M) this.selectedD).getCfg();
		} else if (this.selectedD instanceof T) {
			for (final Element declaration : ((T) this.selectedD).getDeclarations()) {
				if (declaration instanceof M && ((M) declaration).isConstructor()) {
					cfg = ((M) declaration).getCfg();
					if (cfg != null) {
						break;
					}
				}
			}
		} else {
			return;
		}
		if (cfg == null) {
			return;
		}
		try {
			final int stage = this.cfgViewModeCombo.getSelectionIndex();
			cfg.decompile(stage);
		} catch (final Throwable e) {
			TrCalculatePostorder.transform(cfg);
			log.warn("Cannot transform '" + cfg + "'!", e);
		}
		initGraph(cfg);
	}

	private void initGraph(@Nonnull final CFG cfg) {
		// dispose old graph content, first connections than nodes
		Object[] objects = this.graph.getConnections().toArray();
		for (final Object object : objects) {
			((GraphConnection) object).dispose();
		}
		objects = this.graph.getNodes().toArray();
		for (final Object object : objects) {
			((GraphNode) object).dispose();
		}
		// add graph content
		final BB startBb = cfg.getStartBb();
		assert startBb != null;
		addToGraph(startBb, new IdentityHashMap<BB, GraphNode>());
		this.graph.applyLayout();
	}

	protected String renderBbInfo(@Nonnull final BB bb) {
		final CFG cfg = bb.getCfg();
		final int regs = cfg.getRegs();
		final int ops = bb.getOps();
		final int[] sizes = new int[1 + regs + cfg.getMaxStack()];
		final String[][] table = new String[1 + ops][];
		for (int i = -1; i < ops; ++i) {
			final String[] row = new String[1 + cfg.getRegs() + cfg.getMaxStack()];
			table[1 + i] = row;
			if (i == -1) {
				row[0] = "Operation";
				sizes[0] = row[0].length();
				for (int j = 0; j < cfg.getRegs(); ++j) {
					row[1 + j] = "r" + j;
					sizes[1 + j] = row[1 + j].length();
				}
				for (int j = 0; j < cfg.getMaxStack(); ++j) {
					row[1 + regs + j] = "s" + j;
					sizes[1 + regs + j] = row[1 + regs + j].length();
				}
				continue;
			}
			final Op op = bb.getOp(i);
			row[0] = op.toString();
			if (sizes[0] < row[0].length()) {
				sizes[0] = row[0].length();
			}
			final Frame frame = bb.getCfg().getInFrame(op);
			if (frame == null) {
				continue;
			}
			for (int j = 0; j < regs + frame.getTop(); ++j) {
				final R r = frame.load(j);
				if (r != null) {
					row[1 + j] = r.toString();
					if (sizes[1 + j] < row[1 + j].length()) {
						sizes[1 + j] = row[1 + j].length();
					}
				}
			}
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < table.length; ++i) {
			final String[] row = table[i];
			sb.append("\n");
			for (int j = 0; j < row.length; ++j) {
				String str = row[j];
				if (str == null) {
					str = "";
				}
				sb.append(str);
				sb.append(Strings.repeat(" ", sizes[j] - str.length()));
				if (j == 0 || j == bb.getRegs()) {
					sb.append(" # ");
					continue;
				}
				sb.append(" | ");
			}
			if (i == 0) {
				sb.append('\n').append(Strings.repeat("-", sb.length() - 3));
			}
		}
		return sb.toString();
	}

	/**
	 * Select declaration.
	 *
	 * @param selectedD
	 *            selected declaration
	 */
	public void setlectD(@Nonnull final Container selectedD) {
		this.selectedD = selectedD;
		initGraph();
	}

}