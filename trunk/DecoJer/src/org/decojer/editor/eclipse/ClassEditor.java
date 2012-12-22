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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.decojer.DecoJer;
import org.decojer.DecoJerException;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.D;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.transformers.TrCalculatePostorder;
import org.decojer.cavaj.transformers.TrCfg2JavaControlFlowStmts;
import org.decojer.cavaj.transformers.TrCfg2JavaExpressionStmts;
import org.decojer.cavaj.transformers.TrControlFlowAnalysis;
import org.decojer.cavaj.transformers.TrDalvikRemoveTempRegs;
import org.decojer.cavaj.transformers.TrDataFlowAnalysis;
import org.decojer.cavaj.utils.Cursor;
import org.decojer.editor.eclipse.utils.FramesFigure;
import org.decojer.editor.eclipse.utils.HierarchicalLayoutAlgorithm;
import org.decojer.editor.eclipse.utils.MemoryStorageEditorInput;
import org.decojer.editor.eclipse.utils.StringMemoryStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polyline;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
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

	private static Pattern createEclipseMethodSignaturePattern(final String signature) {
		final Cursor c = new Cursor();
		final StringBuilder sb = new StringBuilder();
		// never contains generic type parameters
		parseMethodParamTs(signature, c, sb);
		parseT(signature, c, sb);
		return Pattern.compile(sb.toString());
	}

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

	private static void parseClassT(final String s, final Cursor c, final StringBuilder sb) {
		// ClassTypeSignature: L PackageSpecifier_opt SimpleClassTypeSignature
		// ClassTypeSignatureSuffix_* ;
		// PackageSpecifier: Identifier / PackageSpecifier_*
		// SimpleClassTypeSignature: Identifier TypeArguments_opt
		// ClassTypeSignatureSuffix: . SimpleClassTypeSignature
		final int start = c.pos;
		char ch;
		// PackageSpecifier_opt Identifier
		while (s.length() > c.pos && (ch = s.charAt(c.pos)) != '<' && ch != ';') {
			// $ could be a regular identifier char, we cannot do anything about this here
			++c.pos;
		}
		sb.append(s.substring(start, c.pos));
		// TypeArguments_opt
		parseTypeArgs(s, c, sb);
		// ClassTypeSignatureSuffix_*
		if (s.length() > c.pos && s.charAt(c.pos) == '.') {
			++c.pos;
			sb.append("\\.");
			parseClassT(s, c, sb);
			return;
		}
		return;
	}

	private static void parseMethodParamTs(final String s, final Cursor c, final StringBuilder sb) {
		assert s.charAt(c.pos) == '(' : s.charAt(c.pos);
		++c.pos;
		sb.append("\\(");
		while (s.charAt(c.pos) != ')') {
			parseT(s, c, sb);
		}
		++c.pos;
		sb.append("\\)");
		return;
	}

	private static void parseT(final String s, final Cursor c, final StringBuilder sb) {
		if (s.length() <= c.pos) {
			return;
		}
		final char ch = s.charAt(c.pos++);
		switch (ch) {
		case 'I':
		case 'S':
		case 'B':
		case 'C':
		case 'Z':
		case 'F':
		case 'J':
		case 'D':
		case 'V':
			sb.append(ch);
			return;
		case 'L':
			// ClassTypeSignature
			sb.append('L');
			parseClassT(s, c, sb);
			assert s.charAt(c.pos) == ';' : s.charAt(c.pos);
			++c.pos;
			sb.append(';');
			return;
		case '[':
			// ArrayTypeSignature
			sb.append('[');
			parseT(s, c, sb);
			return;
		case 'T': {
			final int pos = s.indexOf(';', c.pos);
			sb.append('T').append(s.substring(c.pos, pos + 1));
			c.pos = pos + 1;
			return;
		}
		case 'Q':
			// ClassTypeSignature
			sb.append("[LT][^<;]*");
			parseClassT(s, c, sb);
			assert s.charAt(c.pos) == ';' : s.charAt(c.pos);
			++c.pos;
			sb.append(';');
			return;
		default:
			throw new DecoJerException("Unknown type in '" + s + "' (" + c.pos + ")!");
		}
	}

	private static void parseTypeArgs(final String s, final Cursor c, final StringBuilder sb) {
		// TypeArguments_opt
		if (s.length() <= c.pos || s.charAt(c.pos) != '<') {
			return;
		}
		++c.pos;
		sb.append('<');
		char ch;
		while ((ch = s.charAt(c.pos)) != '>') {
			switch (ch) {
			case '+':
				++c.pos;
				sb.append("\\+");
				parseT(s, c, sb);
				break;
			case '-':
				++c.pos;
				sb.append('-');
				parseT(s, c, sb);
				break;
			case '*':
				++c.pos;
				sb.append("\\*");
				break;
			default:
				parseT(s, c, sb);
			}
		}
		++c.pos;
		sb.append('>');
		return;
	}

	private Tree archiveTree;

	private Button cfgAntialiasingCheckbox;

	private Graph cfgViewer;

	private Combo cfgViewModeCombo;

	private ClassFileEditor classFileEditor;

	private CompilationUnitEditor compilationUnitEditor;

	private DU du;

	private JavaOutlinePage javaOutlinePage;

	private CU selectedCu;

	private D selectedD;

	private GraphNode addToGraph(final BB bb, final IdentityHashMap<BB, GraphNode> map) {
		final GraphNode node = new GraphNode(this.cfgViewer, SWT.NONE, bb.toString(), bb);
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
			final GraphConnection connection = new GraphConnection(this.cfgViewer,
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
		String sourceCode = null;
		try {
			sourceCode = this.selectedCu.decompile();
		} catch (final Throwable t) {
			t.printStackTrace();
			sourceCode = "// Decompilation error!";
		}
		try {
			addPage(0, this.compilationUnitEditor,
					new MemoryStorageEditorInput(
							new StringMemoryStorage(sourceCode, this.selectedCu == null
									|| this.selectedCu.getSourceFileName() == null ? new Path(
									"<Unknown>") : new Path(this.selectedCu.getSourceFileName()))));
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
		this.du = DecoJer.createDu();
		try {
			final long currentTimeMillis = System.currentTimeMillis();
			this.du.read(fileName);
			LOGGER.info("Read time for '" + fileName + "' is "
					+ (System.currentTimeMillis() - currentTimeMillis) + " ms");
			if (this.du.getCus().size() == 1) {
				this.selectedCu = this.du.getCus().get(0);
			}
		} catch (final Throwable e) {
			LOGGER.log(Level.SEVERE, "Couldn't open file!", e);
			return pageContainer;
		}
		if (this.selectedCu == null) {
			final SashForm sashForm = new SashForm(pageContainer, SWT.HORIZONTAL | SWT.BORDER
					| SWT.SMOOTH);
			this.archiveTree = new Tree(sashForm, SWT.NONE);
			for (final CU cu : this.du.getCus()) {
				final TreeItem treeItem = new TreeItem(this.archiveTree, SWT.NONE);
				treeItem.setText(cu.getName());
				if (this.selectedCu == null) {
					this.archiveTree.select(treeItem);
					this.selectedCu = cu;
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
					if (ClassEditor.this.selectedCu != null) {
						ClassEditor.this.selectedCu.clear();
					}
					ClassEditor.this.selectedCu = ClassEditor.this.du.getCu(selection.getText());

					String sourceCode = null;
					try {
						sourceCode = ClassEditor.this.selectedCu.decompile();
					} catch (final Throwable t) {
						LOGGER.log(Level.SEVERE, "Decompilation error!", t);
						sourceCode = "// Decompilation error!";
					}
					ClassEditor.this.compilationUnitEditor
							.setInput(new MemoryStorageEditorInput(
									new StringMemoryStorage(sourceCode,
											ClassEditor.this.selectedCu == null
													|| ClassEditor.this.selectedCu
															.getSourceFileName() == null ? null
													: new Path(ClassEditor.this.selectedCu
															.getSourceFileName()))));

				}

			});
			this.archiveTree.pack(); // necessary for correct selection box for first item
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

	/**
	 * Find type declaration for Eclipse type.
	 * 
	 * @param javaElement
	 *            Eclipse Java element
	 * @return declaration
	 */
	private D findDeclarationForJavaElement(final IJavaElement javaElement) {
		// type.getFullyQualifiedName() potentially follows a different naming strategy for inner
		// classes than the internal model from the bytecode, hence we must iterate through the tree
		final ArrayList<IJavaElement> path = new ArrayList<IJavaElement>();
		for (IJavaElement element = javaElement; element != null; element = element.getParent()) {
			path.add(0, element);
		}
		try {
			D d = this.selectedCu;
			path: for (final IJavaElement element : path) {
				if (element instanceof IType) {
					final String typeName = element.getElementName();
					// count anonymous!
					int occurrenceCount = ((IType) element).getOccurrenceCount();
					for (final BD bd : d.getBds()) {
						if (bd instanceof TD && ((TD) bd).getSimpleName().equals(typeName)) {
							if (--occurrenceCount == 0) {
								d = bd;
								continue path;
							}
						}
					}
					return null;
				}
				if (element instanceof IField) {
					// anonymous enum initializers are relocated, see FD#relocateTd();
					// isEnum() doesn't imply isStatic() for source code
					if (!Flags.isEnum(((IField) element).getFlags())) {
						if (Flags.isStatic(((IField) element).getFlags())) {
							for (final BD bd : d.getBds()) {
								if (bd instanceof MD && ((MD) bd).isInitializer()) {
									d = bd;
									continue path;
								}
							}
							return null;
						}
						for (final BD bd : d.getBds()) {
							// descriptor not important, all constructors have same field
							// initializers
							if (bd instanceof MD && ((MD) bd).isConstructor()) {
								d = bd;
								continue path;
							}
						}
					}
					// TODO relocation of other anonymous field initializer TDs...difficult
					final String fieldName = element.getElementName();
					for (final BD bd : d.getBds()) {
						if (bd instanceof FD && ((FD) bd).getName().equals(fieldName)) {
							d = bd;
							continue path;
						}
					}
					return null;
				}
				if (element instanceof IInitializer) {
					for (final BD bd : d.getBds()) {
						if (bd instanceof MD && ((MD) bd).isInitializer()) {
							d = bd;
							continue path;
						}
					}
					return null;
				}
				if (element instanceof IMethod) {
					final String methodName = ((IMethod) element).isConstructor() ? M.CONSTRUCTOR_NAME
							: element.getElementName();
					final String signature = ((IMethod) element).getSignature();
					// get all method declarations with this name
					final ArrayList<MD> mds = new ArrayList<MD>();
					for (final BD bd : d.getBds()) {
						if (bd instanceof MD && ((MD) bd).getName().equals(methodName)) {
							mds.add((MD) bd);
						}
					}
					switch (mds.size()) {
					case 0:
						// shouldn't happen, after all we have decompiled this from the model
						LOGGER.warning("Unknown method declaration for '" + methodName + "'!");
						return null;
					case 1:
						// only 1 possible method, signature check not really necessary
						d = mds.get(0);
						continue path;
					default:
						// multiple methods with different signatures, we now have to match against
						// Eclipse method selection signatures with Q instead of L or T:
						// Q stands for unresolved type packages and is replaced by regexp [LT][^;]*

						// for this we must decompile the signature, Q-signatures can follow to any
						// stuff like this characters: ();[
						// but also to primitives like this: (IIQString;)V

						// Such signatures doesn't contain method parameter types but they contain
						// generic type parameters.
						final Pattern signaturePattern = createEclipseMethodSignaturePattern(signature);
						for (final MD checkMd : mds) {
							// exact match for descriptor
							if (signaturePattern.matcher(checkMd.getDescriptor()).matches()) {
								d = checkMd;
								continue path;
							}
							if (checkMd.getSignature() == null) {
								continue;
							}
							// ignore initial method parameters <T...;T...> and exceptions
							// ^T...^T...;
							// <T:Ljava/lang/Integer;E:Ljava/lang/RuntimeException;>(TT;TT;)V^TE;^Ljava/lang/RuntimeException;
							if (signaturePattern.matcher(checkMd.getSignature()).find()) {
								d = checkMd;
								continue path;
							}
						}
						LOGGER.warning("Unknown method declaration for '" + methodName
								+ "' and signature '" + signature + "'! Derived pattern:\n"
								+ signaturePattern.toString());
						return null;
					}
				}
			}
			return d;
		} catch (final JavaModelException e) {
			LOGGER.log(Level.SEVERE, "Couldn't get Eclipse Java element data for selection!", e);
			return null;
		}
	}

	@Override
	public Object getAdapter(final Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			// initialize the CompilationUnitEditor with the decompiled source via a in-memory
			// StorageEditorInput and ask this Editor for the IContentOutlinePage, this way we can
			// also show inner classes

			// for this the in-memory StorageEditorInput needs a fullPath!

			// didn't work in older Eclipse? JavaOutlinePage.fInput == null in this case, also ask
			// the ClassFileEditor, which has other problems and only delivers an Outline if the
			// class is in the class path
			Object adapter = null;
			if ((this.selectedCu != null && this.selectedCu.getSourceFileName() != null || this.classFileEditor == null)
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
						final TreeSelection treeSelection = (TreeSelection) event.getSelection();
						final D d = findDeclarationForJavaElement((IJavaElement) treeSelection
								.getFirstElement());
						if (d == null) {
							LOGGER.warning("Unknown declaration for path '"
									+ treeSelection.getFirstElement() + "'!");
							return;
						}
						ClassEditor.this.selectedD = d;
						initGraph();
					}

				});
				return this.javaOutlinePage;
			}
		}
		return super.getAdapter(required);
	}

	private void initGraph() {
		if (this.selectedD instanceof MD) {
			final int stage = this.cfgViewModeCombo.getSelectionIndex();
			final CFG cfg = ((MD) this.selectedD).getCfg();
			if (cfg == null || cfg.isIgnore()) {
				return;
			}
			try {
				// retransform CFG until given transformation stage
				TrDataFlowAnalysis.transform(cfg);
				TrCalculatePostorder.transform(cfg);

				if (stage > 0) {
					TrDalvikRemoveTempRegs.transform(cfg);
					TrCfg2JavaExpressionStmts.transform(cfg);
					TrCalculatePostorder.transform(cfg);
				}
				if (stage > 1) {
					TrControlFlowAnalysis.transform(cfg);
					TrCfg2JavaControlFlowStmts.transform(cfg);
				}
			} catch (final Throwable e) {
				TrCalculatePostorder.transform(cfg);
				LOGGER.log(Level.WARNING, "Cannot transform '" + cfg + "'!", e);
			}
			initGraph(cfg);
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