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

import java.util.ArrayList;
import java.util.IdentityHashMap;

import javax.annotation.Nullable;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.swt.SWT;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.layouts.algorithms.AbstractLayoutAlgorithm;
import org.eclipse.zest.layouts.dataStructures.InternalNode;
import org.eclipse.zest.layouts.dataStructures.InternalRelationship;

/**
 * Hierarchical layout algorithm for visualizing the Control Flow Graph.
 *
 * @author André Pankraz
 */
public class HierarchicalLayoutAlgorithm extends AbstractLayoutAlgorithm {

	/**
	 * Constructor.
	 *
	 * @param styles
	 *            LayoutStyles
	 */
	public HierarchicalLayoutAlgorithm(final int styles) {
		super(styles);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void applyLayoutInternal(@Nullable final InternalNode[] entitiesToLayout,
			@Nullable final InternalRelationship[] relationshipsToConsider, final double boundsX,
			final double boundsY, final double boundsWidth, final double boundsHeight) {
		final IdentityHashMap<InternalNode, Node> mapping = new IdentityHashMap<InternalNode, Node>(
				entitiesToLayout.length);
		final DirectedGraph graph = new DirectedGraph();
		for (final InternalNode internalNode : entitiesToLayout) {
			final Node node = new Node(internalNode);
			node.setSize(new Dimension((int) internalNode.getWidthInLayout(), (int) internalNode
					.getHeightInLayout()));
			mapping.put(internalNode, node);
			graph.nodes.add(node);
		}
		for (final InternalRelationship relationship : relationshipsToConsider) {
			final Node source = mapping.get(relationship.getSource());
			final Node dest = mapping.get(relationship.getDestination());
			// this algorithm cannot handle cycles, ignore them here...
			if (((GraphConnection) relationship.getLayoutRelationship().getGraphData())
					.getLineColor() == ColorConstants.red) {
				continue;
			}
			// alternative way...
			/*
			 * final int sourcePostorder = ((BB) ((GraphNode) relationship
			 * .getSource().getLayoutEntity().getGraphData()).getData()) .getPostorder(); final int
			 * destPostorder = ((BB) ((GraphNode) relationship
			 * .getDestination().getLayoutEntity().getGraphData()) .getData()).getPostorder(); if
			 * (sourcePostorder <= destPostorder) { continue; }
			 */
			final Edge edge = new Edge(relationship, source, dest);
			graph.edges.add(edge);
		}
		final DirectedGraphLayout directedGraphLayout = new DirectedGraphLayout();
		directedGraphLayout.visit(graph);

		for (final Node node : (ArrayList<Node>) graph.nodes) {
			final InternalNode internalNode = (InternalNode) node.data;
			// For horizontal layout transpose the x and y coordinates
			if ((this.layout_styles & SWT.HORIZONTAL) == SWT.HORIZONTAL) {
				internalNode.setInternalLocation(node.y, node.x);
			} else {
				internalNode.setInternalLocation(node.x, node.y);
			}
		}
		updateLayoutLocations(entitiesToLayout);
	}

	@Override
	protected int getCurrentLayoutStep() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getTotalNumberOfLayoutSteps() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected boolean isValidConfiguration(final boolean asynchronous, final boolean continuous) {
		return true;
	}

	@Override
	protected void postLayoutAlgorithm(@Nullable final InternalNode[] entitiesToLayout,
			@Nullable final InternalRelationship[] relationshipsToConsider) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void preLayoutAlgorithm(@Nullable final InternalNode[] entitiesToLayout,
			@Nullable final InternalRelationship[] relationshipsToConsider, final double x,
			final double y, final double width, final double height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLayoutArea(final double x, final double y, final double width,
			final double height) {
		// TODO Auto-generated method stub

	}

}
