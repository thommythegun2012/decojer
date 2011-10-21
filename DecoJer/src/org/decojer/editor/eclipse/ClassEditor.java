/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.decojer.DecoJer;
import org.decojer.DecoJerException;
import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.transformer.TrControlFlowAnalysis;
import org.decojer.cavaj.transformer.TrDataFlowAnalysis;
import org.decojer.cavaj.transformer.TrInitControlFlowGraph;
import org.decojer.cavaj.transformer.TrIvmCfg2JavaExprStmts;
import org.decojer.editor.eclipse.util.FramesFigure;
import org.decojer.editor.eclipse.util.HierarchicalLayoutAlgorithm;
import org.decojer.editor.eclipse.util.MemoryStorageEditorInput;
import org.decojer.editor.eclipse.util.StringMemoryStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polyline;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaOutlinePage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
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
 * An example showing how to create a multi-page editor. This example has 3 pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
@SuppressWarnings("restriction")
public class ClassEditor extends MultiPageEditorPart {

	private final static Logger LOGGER = Logger.getLogger(ClassEditor.class.getName());

	private static String extractPath(final IClassFile eclipseClassFile) {
		assert eclipseClassFile != null;

		if (eclipseClassFile.getResource() == null) {
			// is from JAR...
			// example: sun/org/mozilla/javascript/internal/
			final String jarPath = eclipseClassFile.getPath().toOSString();
			final String packageName = eclipseClassFile.getParent().getElementName();
			final String typeName = eclipseClassFile.getElementName();
			return jarPath + "!/" + packageName.replace('.', '/') + '/' + typeName;
		}
		return eclipseClassFile.getResource().getLocation().toOSString();
	}

	private Tree archiveTree;

	private Button cfgAntialiasingCheckbox;

	private Graph cfgViewer;

	private Combo cfgViewModeCombo;

	private ClassFileEditor classFileEditor;

	private CompilationUnitEditor compilationUnitEditor;

	private CU cu;

	private DU du;

	private TD td;

	private JavaOutlinePage javaOutlinePage;

	private GraphNode addToGraph(final BB bb, final IdentityHashMap<BB, GraphNode> map) {
		final GraphNode node = new GraphNode(this.cfgViewer, SWT.NONE, bb.toString(), bb);
		if (bb.getCfg() != null && bb.getCfg().getFrames() != null) {
			node.setTooltip(new FramesFigure(bb));
		} else if (bb.getStruct() != null) {
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
			final GraphConnection connection = new GraphConnection(this.cfgViewer,
					ZestStyles.CONNECTIONS_DIRECTED, node, succNode);
			if (this.cfgAntialiasingCheckbox.getSelection()) {
				((Polyline) connection.getConnectionFigure()).setAntialias(SWT.ON);
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
			setPageText(0, "Class File Editor");
		} catch (final PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null,
					e.getStatus());
		}
	}

	private void createControlFlowGraphViewer() {
		final Composite composite = new Composite(getContainer(), SWT.NONE);
		final GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		this.cfgAntialiasingCheckbox = new Button(composite, SWT.CHECK);
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
		this.cfgViewModeCombo = new Combo(composite, SWT.READ_ONLY);
		this.cfgViewModeCombo.setItems(new String[] { "IVM CFG", "Java Expr", "Control Flow" });
		this.cfgViewModeCombo.setText("Control Flow");
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
		this.cfgViewer = new Graph(composite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		this.cfgViewer.setLayoutData(gridData);
		this.cfgViewer.setLayoutAlgorithm(new HierarchicalLayoutAlgorithm(
				LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);

		addPage(0, composite);
		setPageText(0, "CFG Viewer");
	}

	private void createDecompilationUnitEditor() {
		this.compilationUnitEditor = new CompilationUnitEditor();

		// create editor input, in-memory string with decompiled source

		this.cu = null;
		String sourceCode = null;
		try {
			this.cu = DecoJer.createCu(this.td);
			sourceCode = DecoJer.decompile(this.cu);
		} catch (final Throwable t) {
			t.printStackTrace();
			sourceCode = "// Decompilation error!";
		}
		try {
			addPage(0, this.compilationUnitEditor,
					new MemoryStorageEditorInput(new StringMemoryStorage(sourceCode,
							this.cu == null || this.cu.getSourceFileName() == null ? new Path(
									"<Unknown>") : new Path(this.cu.getSourceFileName()))));
		} catch (final PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null,
					e.getStatus());
		}
		setPageText(0, "Source");
	}

	@Override
	protected Composite createPageContainer(final Composite parent) {
		// method is called before createPages(): pre-analyze editor input and
		// add archive if necessary
		Composite pageContainer = super.createPageContainer(parent);

		final IEditorInput editorInput = getEditorInput();

		String fileName;
		if (editorInput instanceof IClassFileEditorInput) {
			// is a simple Eclipse-pre-analyzed class file, not an archive
			final IClassFile classFile = ((IClassFileEditorInput) editorInput).getClassFile();
			fileName = extractPath(classFile);
		} else if (editorInput instanceof FileEditorInput) {
			// could be a class file (not Eclipse-pre-analyzed) or an archive
			final FileEditorInput fileEditorInput = (FileEditorInput) editorInput;
			final IPath filePath = fileEditorInput.getPath();
			fileName = filePath.toString();
		} else {
			throw new DecoJerException("Unknown editor input type '" + editorInput + "'!");
		}

		LOGGER.info("Editor Input: " + editorInput + " fileName: " + fileName);

		this.du = DecoJer.createDu();

		try {
			this.td = this.du.read(fileName);
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Couldn't open file!", e);
			return pageContainer;
		}

		if (this.td == null) {
			final SashForm sashForm = new SashForm(pageContainer, SWT.HORIZONTAL | SWT.BORDER
					| SWT.SMOOTH);

			this.archiveTree = new Tree(sashForm, SWT.NONE);
			for (final Entry<String, TD> type : this.du.getTds()) {
				final TreeItem treeItem = new TreeItem(this.archiveTree, SWT.NONE);
				treeItem.setText(type.getKey());
				if (this.td == null) {
					this.archiveTree.setSelection(treeItem);
					this.td = type.getValue();
				}
			}
			this.archiveTree.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					// OK
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					final TreeItem[] selections = ClassEditor.this.archiveTree.getSelection();
					if (selections.length != 1) {
						return;
					}
					final TreeItem selection = selections[0];
					ClassEditor.this.td = ClassEditor.this.du.getTd(selection.getText());

					if (ClassEditor.this.cu != null) {
						ClassEditor.this.cu.clear();
						ClassEditor.this.cu = null;
					}
					String sourceCode = null;
					try {
						ClassEditor.this.cu = DecoJer.createCu(ClassEditor.this.td);
						sourceCode = DecoJer.decompile(ClassEditor.this.cu);
						ClassEditor.this.compilationUnitEditor.setInput(new MemoryStorageEditorInput(
								new StringMemoryStorage(sourceCode, ClassEditor.this.cu == null
										|| ClassEditor.this.cu.getSourceFileName() == null ? null
										: new Path(ClassEditor.this.cu.getSourceFileName()))));
					} catch (final Throwable t) {
						t.printStackTrace();
						sourceCode = "// Decompilation error!";
					}

				}

			});
			pageContainer = sashForm;
		}
		return pageContainer;
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	@Override
	protected void createPages() {
		setPartName(getEditorInput().getName());
		if (this.archiveTree != null) {
			// must happen delayed after added tab pane
			((SashForm) this.archiveTree.getParent()).setWeights(new int[] { 1, 4 });
		}

		// for debugging purposes:
		createControlFlowGraphViewer();
		// initialization comes first, delivers IClassFileEditorInput
		if (this.archiveTree == null) {
			createClassFileEditor();
		}
		createDecompilationUnitEditor();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	@Override
	public void doSave(final IProgressMonitor monitor) {
		getEditor(0).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the text for page 0's
	 * tab, and updates this multi-page editor's input to correspond to the nested editor's.
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
		if (IContentOutlinePage.class.equals(required)) {
			// initialize the CompilationUnitEditor with the decompiled source
			// via a in-memory StorageEditorInput and ask this Editor for the
			// IContentOutlinePage, this way we can also show inner classes

			// for this the in-memory StorageEditorInput needs a fullPath!

			// didn't work in older Eclipse? JavaOutlinePage.fInput == null in
			// this case, also ask the ClassFileEditor, which has other problems
			// and only delivers an Outline if the class is in the class path
			Object adapter = null;
			if ((this.cu != null && this.cu.getSourceFileName() != null || this.classFileEditor == null)
					&& this.compilationUnitEditor != null) {
				adapter = this.compilationUnitEditor.getAdapter(required);
			}
			if (adapter == null && this.classFileEditor != null) {
				adapter = this.classFileEditor.getAdapter(required);
			}
			if (adapter instanceof JavaOutlinePage) {
				if (this.javaOutlinePage != null && this.javaOutlinePage == adapter) {
					return this.javaOutlinePage;
				}
				this.javaOutlinePage = (JavaOutlinePage) adapter;
				this.javaOutlinePage.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(final SelectionChangedEvent event) {
						initGraph();
					}

				});
				return this.javaOutlinePage;
			}
		}
		return super.getAdapter(required);
	}

	private void initGraph() {
		final TreeSelection treeSelection = (TreeSelection) this.javaOutlinePage.getSelection();
		final Object firstElement = treeSelection.getFirstElement();

		// get available selection information
		String fullyQualifiedName;
		String elementName;
		String signature;
		if (firstElement instanceof IInitializer) {
			final IInitializer method = (IInitializer) firstElement;
			fullyQualifiedName = method.getDeclaringType().getFullyQualifiedName();
			elementName = "<clinit>";
			signature = "()V";
		} else if (firstElement instanceof IMethod) {
			final IMethod method = (IMethod) firstElement;
			fullyQualifiedName = method.getDeclaringType().getFullyQualifiedName();
			elementName = method.getElementName();
			try {
				signature = method.getSignature();
			} catch (final JavaModelException e) {
				e.printStackTrace();
				return;
			}
		} else {
			return;
		}
		// find CFG
		LOGGER.info("Find method declaration for declaring type '" + fullyQualifiedName
				+ "' and element name '" + elementName + "' and signature '" + signature + "'!");
		try {
			// get declaring type,
			// first undo T.getIName() trick for valid java source with incomplete decompilation
			fullyQualifiedName = fullyQualifiedName.replace("$I_", "$");
			if (elementName.startsWith("I_")) {
				elementName = elementName.substring(2);
			}
			final TD td = this.cu.getTd(fullyQualifiedName);

			// get method in declaring type,
			// first check if selected method is a constructor, then method name is <init>
			final String methodName = fullyQualifiedName.equals(elementName)
					|| fullyQualifiedName.endsWith("$" + elementName) ? "<init>" : elementName;
			// get all method declarations with this name
			final ArrayList<MD> mds = new ArrayList<MD>();
			for (final BD bd : td.getBds()) {
				if (!(bd instanceof MD)) {
					continue;
				}
				if (methodName.equals(((MD) bd).getM().getName())) {
					mds.add((MD) bd);
				}
			}
			MD md = null;
			if (mds.size() == 0) {
				// shouldn't happen, after all we have decompiled this from the model
				LOGGER.warning("Unknown method declaration for '" + methodName + "'!");
				return;
			}
			if (mds.size() == 1) {
				// only 1 possible method, signature check not really necessary
				md = mds.get(0);
			} else if (mds.size() > 1) {
				// multiple methods with different signatures,
				// we now have to match against Eclipse-select Q-signatures (e.g. "QString;"):
				// Q stands for unresolved type packages and is replaced by regexp L[^/;]*

				// for this we must decompile the signature, Q-signatures can follow to any stuff
				// like this characters: ();[
				// but also to primitives like this: (IIQString;)V

				// Eclipse-signature doesn't contain method parameter types but contains generics

				// TODO

				// not enough here...Q can come after all kind off stuff, parser!
				final Pattern signaturePattern = Pattern.compile(signature.replace("[", "\\[")
						.replace("]", "\\]").replace("(Q", "(L[^;]*").replace(")Q", ")L[^;]*")
						.replace(";Q", ";L[^;]*").replace("$", "\\$").replace("(", "\\(")
						.replace(")", "\\)"));

				for (final MD checkMd : mds) {
					final M m = checkMd.getM();
					final String descriptor = m.getDescriptor();
					if (signaturePattern.matcher(descriptor).matches()) {
						md = checkMd;
						break;
					}
				}
			}
			if (md == null) {
				LOGGER.warning("Unknown method declaration for '" + methodName
						+ "' and signature '" + signature + "'!");
				return;
			}

			final CFG cfg = md.getCfg();
			if (cfg == null) {
				return;
			}

			// retransform CFG until given transformation stage
			TrInitControlFlowGraph.transform(cfg);
			try {
				TrDataFlowAnalysis.transform(cfg);
			} catch (final Exception e) {
				LOGGER.log(Level.WARNING, "Cannot transform '" + cfg.getMd() + "'!", e);
			}
			final int i = this.cfgViewModeCombo.getSelectionIndex();
			if (i > 0) {
				TrIvmCfg2JavaExprStmts.transform(cfg);
			}
			if (i > 1) {
				TrControlFlowAnalysis.transform(cfg);
			}

			// build graph
			initGraph(cfg);
		} catch (final Throwable e) {
			LOGGER.log(Level.WARNING, "Couldn't create graph!", e);
		}
	}

	private void initGraph(final CFG cfg) {
		final Graph g = this.cfgViewer;
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

}