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
package org.decojer.editor.eclipse;

import java.util.IdentityHashMap;
import java.util.List;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.PF;
import org.decojer.cavaj.transformer.TrControlFlowAnalysis;
import org.decojer.cavaj.transformer.TrDataFlowAnalysis;
import org.decojer.cavaj.transformer.TrIvmCfg2JavaExprStmts;
import org.decojer.cavaj.transformer.TrJvmCode2IvmCfg;
import org.decojer.cavaj.transformer.TrStructCfg2JavaControlFlowStmts;
import org.decojer.editor.eclipse.util.StringInput;
import org.decojer.editor.eclipse.util.StringStorage;
import org.decojer.editor.eclipse.viewer.cfg.HierarchicalLayoutAlgorithm;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polyline;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaOutlinePage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;

/**
 * An example showing how to create a multi-page editor. This example has 3
 * pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
@SuppressWarnings("restriction")
public class ClassEditor extends MultiPageEditorPart implements
		IResourceChangeListener {

	private Button antialiasingButton;

	private ClassFileEditor classFileEditor;

	private Combo combo;

	private CompilationUnitEditor compilationUnitEditor;

	private CU cu;

	private Graph graph;

	private JavaOutlinePage javaOutlinePage;

	/**
	 * Constructor.
	 */
	public ClassEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	private GraphNode addToGraph(final BB bb,
			final IdentityHashMap<BB, GraphNode> map) {
		final GraphNode node = new GraphNode(this.graph, SWT.NONE,
				bb.toString(), bb);
		if (bb.getStruct() != null) {
			node.setTooltip(new Label(bb.getStruct().toString()));
		} else {
			node.setTooltip(null);
		}
		map.put(bb, node);
		final List<BB> succBBs = bb.getSuccBbs();
		final List<Object> succValues = bb.getSuccValues();
		for (int i = 0; i < succBBs.size(); ++i) {
			final BB succBB = succBBs.get(i);
			GraphNode succNode = map.get(succBB);
			if (succNode == null) {
				succNode = addToGraph(succBB, map);
			}
			final GraphConnection connection = new GraphConnection(this.graph,
					ZestStyles.CONNECTIONS_DIRECTED, node, succNode);
			if (this.antialiasingButton.getSelection()) {
				((Polyline) connection.getConnectionFigure())
						.setAntialias(SWT.ON);
			}
			final Object value = succValues.get(i);
			if (value != null) {
				connection.setText(value.toString());
			}
			if (succBB.getPostorder() >= bb.getPostorder()) {
				connection.setCurveDepth(50);
				connection.setLineColor(ColorConstants.red);
			}
		}
		return node;
	}

	private void createClassFileEditor() {
		this.classFileEditor = new ClassFileEditor();
		try {
			addPage(0, this.classFileEditor, getEditorInput());
		} catch (final PartInitException e) {
			ErrorDialog.openError(getSite().getShell(),
					"Error creating nested text editor", null, e.getStatus());
		}
		setPageText(0, "Class File Editor");
		setPartName(this.classFileEditor.getTitle()); // multi page editor name
	}

	private void createControlFlowGraphViewer() {
		final Composite composite = new Composite(getContainer(), SWT.NONE);
		final GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		this.antialiasingButton = new Button(composite, SWT.CHECK);
		GridData gridData = new GridData();
		this.antialiasingButton.setLayoutData(gridData);
		this.antialiasingButton.setText("Antialiasing");
		this.antialiasingButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				initGraph();
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				initGraph();
			}

		});
		this.combo = new Combo(composite, SWT.READ_ONLY);
		this.combo.setItems(new String[] { "IVM CFG", "Java Expr",
				"Control Flow" });
		this.combo.setText("Control Flow");
		this.combo.addSelectionListener(new SelectionListener() {

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
		this.combo.setLayoutData(gridData);
		// draw graph
		// Graph will hold all other objects
		this.graph = new Graph(composite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		this.graph.setLayoutData(gridData);
		this.graph.setLayoutAlgorithm(new HierarchicalLayoutAlgorithm(
				LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);

		addPage(0, composite);
		setPageText(0, "CFG Viewer");
	}

	private void createDecompilationUnitEditor() {
		this.compilationUnitEditor = new CompilationUnitEditor();
		String sourceCode;
		try {
			// create editor input, in-memory string with decompiled source
			final IClassFileEditorInput classFileEditorInput = (IClassFileEditorInput) this.classFileEditor
					.getEditorInput();
			final IClassFile classFile = classFileEditorInput.getClassFile();
			final String typeFileName = classFile.getElementName();

			final PF pf = DecoJer
					.createPF(new EclipsePackageClassStreamProvider(classFile));
			final String typeName = typeFileName.substring(0,
					typeFileName.length() - 6);
			this.cu = DecoJer.createCU(pf.getTd(typeName));
			sourceCode = DecoJer.decompile(this.cu);
		} catch (final Throwable e) {
			e.printStackTrace();
			sourceCode = "// Decompilation error!";
		}
		try {
			addPage(0, this.compilationUnitEditor, new StringInput(
					new StringStorage(sourceCode)));
		} catch (final PartInitException e) {
			ErrorDialog.openError(getSite().getShell(),
					"Error creating nested text editor", null, e.getStatus());
		}
		setPageText(0, "Source");
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	@Override
	protected void createPages() {
		// for debugging purposes:
		createControlFlowGraphViewer();
		// initialization comes first, delivers IClassFileEditorInput
		createClassFileEditor();
		createDecompilationUnitEditor();
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		// TODO JavaOutlinePage.removeListener necessary???
		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	@Override
	public void doSave(final IProgressMonitor monitor) {
		getEditor(0).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the
	 * text for page 0's tab, and updates this multi-page editor's input to
	 * correspond to the nested editor's.
	 */
	@Override
	public void doSaveAs() {
		final IEditorPart editor = getEditor(0);
		editor.doSaveAs();
		setPageText(0, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class required) {
		final Object adapter;
		if (IContentOutlinePage.class.equals(required)) {
			// the plan is, to initialize the Compilation Unit Editor with the
			// decompiled source code via an in-memory String Input and to ask
			// this Editor for the IContentOutlinePage;
			// this way we can also show inner classes informations
			// TODO:
			// doesn't work yet, JavaOutlinePage.fInput == null in this case, so
			// ask Class File Editor for now, which has other problems and only
			// delivers an Outline if class is in class path
			adapter = this.classFileEditor.getAdapter(required);
			if (this.javaOutlinePage == null
					&& adapter instanceof JavaOutlinePage) {
				this.javaOutlinePage = (JavaOutlinePage) adapter;
				this.javaOutlinePage
						.addSelectionChangedListener(new ISelectionChangedListener() {

							@Override
							public void selectionChanged(
									final SelectionChangedEvent event) {
								initGraph();
							}

						});
			}
		} else {
			adapter = super.getAdapter(required);
		}
		return adapter;
	}

	private void initGraph() {
		// get selection from java outline page
		final TreeSelection treeSelection = (TreeSelection) this.javaOutlinePage
				.getSelection();
		final Object firstElement = treeSelection.getFirstElement();
		if (firstElement instanceof IType) {
			final IType type = (IType) firstElement;
			System.out.println("###TypeSelection: "
					+ type.getFullyQualifiedName());
		}
		if (!(firstElement instanceof IMethod)) {
			return;
		}
		// if selection found => get matching CFG from CU with choosen
		// decompiler step
		final IMethod method = (IMethod) firstElement;

		MD md = null;
		try {
			md = this.cu.getTd(
					method.getDeclaringType().getFullyQualifiedName()).getMD(
					method.getElementName(), method.getSignature());
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final CFG cfg = md.getCfg();
		if (cfg != null) {
			final int i = this.combo.getSelectionIndex();
			TrJvmCode2IvmCfg.transform(cfg);
			if (i > 0) {
				TrDataFlowAnalysis.transform(cfg);
				TrIvmCfg2JavaExprStmts.transform(cfg);
			}
			if (i > 1) {
				TrControlFlowAnalysis.transform(cfg);
				try {
					TrStructCfg2JavaControlFlowStmts.transform(cfg);
				} catch (final Throwable e) {
					e.printStackTrace();
				}
			}
			initGraph(cfg);
		}
	}

	private void initGraph(final CFG cfg) {
		final Graph g = this.graph;
		// dispose old graph content, first connections than nodes
		Object[] objects = g.getConnections().toArray();
		for (final Object object : objects) {
			((GraphConnection) object).dispose();
		}
		objects = g.getNodes().toArray();
		for (final Object object : objects) {
			((GraphNode) object).dispose();
		}
		// add graph content
		addToGraph(cfg.getStartBb(), new IdentityHashMap<BB, GraphNode>());
		g.applyLayout();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					final IWorkbenchPage[] pages = getSite()
							.getWorkbenchWindow().getPages();
					for (final IWorkbenchPage page : pages) {
						if (((FileEditorInput) ClassEditor.this.classFileEditor
								.getEditorInput()).getFile().getProject()
								.equals(event.getResource())) {
							final IEditorPart editorPart = page
									.findEditor(ClassEditor.this.classFileEditor
											.getEditorInput());
							page.closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

}