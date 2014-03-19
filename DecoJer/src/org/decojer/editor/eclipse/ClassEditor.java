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

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.decojer.DecoJer;
import org.decojer.DecoJerException;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.Container;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.utils.Cursor;
import org.decojer.editor.eclipse.cfg.CfgViewer;
import org.decojer.editor.eclipse.du.DecompilationUnitEditor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.google.common.collect.Lists;

/**
 * Class editor.
 */
@SuppressWarnings("restriction")
@Slf4j
public class ClassEditor extends MultiPageEditorPart {

	private static Pattern createEclipseMethodSignaturePattern(final String signature) {
		final Cursor c = new Cursor();
		final StringBuilder sb = new StringBuilder();
		// never contains generic type parameters
		parseMethodParamTs(signature, c, sb);
		parseT(signature, c, sb);
		return Pattern.compile(sb.toString());
	}

	private static String extractPath(final IClassFile eclipseClassFile) {
		// is from JAR...
		// example: sun/org/mozilla/javascript/internal/
		final String jarPath = eclipseClassFile.getResource() != null ? eclipseClassFile
				.getResource().getLocation().toOSString() : eclipseClassFile.getPath().toOSString();
		assert jarPath != null;

		final String packageName = eclipseClassFile.getParent().getElementName();
		final String typeName = eclipseClassFile.getElementName();
		return jarPath + "!/" + (packageName.isEmpty() ? "" : packageName.replace('.', '/') + '/')
				+ typeName;
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
			sb.append("\\[");
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

	@Nullable
	private Tree archiveTree;

	@Nullable
	private CfgViewer cfgViewer;

	@Nullable
	private ClassFileEditor classFileEditor;

	@Nullable
	private DecompilationUnitEditor decompilationUnitEditor;

	@Nullable
	private DU du;

	@Nullable
	private JavaOutlinePage javaOutlinePage;

	@Nullable
	private CU selectedCu;

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
		this.cfgViewer = new CfgViewer(getContainer(), SWT.NONE);
		addPage(0, this.cfgViewer);
		setPageText(0, "CFG Viewer");
	}

	private void createDecompilationUnitEditor() {
		this.decompilationUnitEditor = new DecompilationUnitEditor();

		try {
			addPage(0, this.decompilationUnitEditor,
					DecompilationUnitEditor.decompileToEditorInput(this.selectedCu));
		} catch (final PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null,
					e.getStatus());
		}
		setPageText(0, "Source");
	}

	@Override
	protected Composite createPageContainer(@Nullable final Composite parent) {
		// method is called before createPages() - change pageContainer for archives
		final Composite pageContainer = super.createPageContainer(parent);
		if (this.selectedCu != null) {
			return pageContainer;
		}
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
			public void widgetDefaultSelected(@Nullable final SelectionEvent e) {
				// OK
			}

			@Override
			public void widgetSelected(@Nullable final SelectionEvent e) {
				final TreeItem[] selections = ClassEditor.this.archiveTree.getSelection();
				if (selections.length != 1) {
					return;
				}
				final TreeItem selection = selections[0];
				if (ClassEditor.this.selectedCu != null) {
					ClassEditor.this.selectedCu.clear();
				}
				ClassEditor.this.selectedCu = ClassEditor.this.du.getCu(selection.getText());
				ClassEditor.this.decompilationUnitEditor.setInput(ClassEditor.this.selectedCu);

			}

		});
		this.archiveTree.pack(); // necessary for correct selection box for first item
		return sashForm;
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
	public void doSave(@Nullable final IProgressMonitor monitor) {
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
	@Nullable
	private Container findDeclarationForJavaElement(final IJavaElement javaElement) {
		// type.getFullyQualifiedName() potentially follows a different naming strategy for inner
		// classes than the internal model from the bytecode, hence we must iterate through the tree
		final List<IJavaElement> path = Lists.newArrayList();
		for (IJavaElement element = javaElement; element != null; element = element.getParent()) {
			path.add(0, element);
		}
		try {
			Container container = this.selectedCu;
			path: for (final IJavaElement element : path) {
				if (element instanceof IType) {
					final String typeName = element.getElementName();
					// count anonymous!
					int occurrenceCount = ((IType) element).getOccurrenceCount();
					for (final Element e : container.getDeclarations()) {
						if (e instanceof T && ((T) e).getSimpleName().equals(typeName)) {
							if (--occurrenceCount == 0) {
								container = e;
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
							for (final Element e : container.getDeclarations()) {
								if (e instanceof M && ((M) e).isInitializer()) {
									container = e;
									continue path;
								}
							}
							return null;
						}
						for (final Element e : container.getDeclarations()) {
							// descriptor not important, all constructors have same field
							// initializers
							if (e instanceof M && ((M) e).isConstructor()) {
								container = e;
								continue path;
							}
						}
					}
					// TODO relocation of other anonymous field initializer TDs...difficult
					final String fieldName = element.getElementName();
					for (final Element e : container.getDeclarations()) {
						if (e instanceof F && ((F) e).getName().equals(fieldName)) {
							container = e;
							continue path;
						}
					}
					return null;
				}
				if (element instanceof IInitializer) {
					for (final Element e : container.getDeclarations()) {
						if (e instanceof M && ((M) e).isInitializer()) {
							container = e;
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
					final List<M> ms = Lists.newArrayList();
					for (final Element e : container.getDeclarations()) {
						if (e instanceof M && ((M) e).getName().equals(methodName)) {
							ms.add((M) e);
						}
					}
					switch (ms.size()) {
					case 0:
						// shouldn't happen, after all we have decompiled this from the model
						log.warn("Unknown method declaration for '" + methodName + "'!");
						return null;
					case 1:
						// only 1 possible method, signature check not really necessary
						container = ms.get(0);
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
						for (final M checkMd : ms) {
							// exact match for descriptor
							if (signaturePattern.matcher(checkMd.getDescriptor()).matches()) {
								container = checkMd;
								continue path;
							}
							if (checkMd.getSignature() == null) {
								continue;
							}
							// ignore initial method parameters <T...;T...> and exceptions
							// ^T...^T...;
							// <T:Ljava/lang/Integer;E:Ljava/lang/RuntimeException;>(TT;TT;)V^TE;^Ljava/lang/RuntimeException;
							if (signaturePattern.matcher(checkMd.getSignature()).find()) {
								container = checkMd;
								continue path;
							}
						}
						log.warn("Unknown method declaration for '" + methodName
								+ "' and signature '" + signature + "'! Derived pattern:\n"
								+ signaturePattern.toString());
						return null;
					}
				}
			}
			return container;
		} catch (final JavaModelException e) {
			log.error("Couldn't get Eclipse Java element data for selection!", e);
			return null;
		}
	}

	@Override
	public Object getAdapter(@Nullable final Class required) {
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
					&& this.decompilationUnitEditor != null) {
				adapter = this.decompilationUnitEditor.getAdapter(required);
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
					public void selectionChanged(@Nullable final SelectionChangedEvent event) {
						final TreeSelection treeSelection = (TreeSelection) event.getSelection();
						final Container c = findDeclarationForJavaElement((IJavaElement) treeSelection
								.getFirstElement());
						if (c == null) {
							log.warn("Unknown declaration for path '"
									+ treeSelection.getFirstElement() + "'!");
							return;
						}
						ClassEditor.this.cfgViewer.setlectD(c);
					}

				});
				return this.javaOutlinePage;
			}
		}
		return super.getAdapter(required);
	}

	@Override
	public void init(@Nullable final IEditorSite site, @Nullable final IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		String fileName;
		if (input instanceof IClassFileEditorInput) {
			// is a simple Eclipse-pre-analyzed class file, not an archive
			final IClassFile classFile = ((IClassFileEditorInput) input).getClassFile();
			fileName = extractPath(classFile);
		} else if (input instanceof FileEditorInput) {
			// could be a class file (not Eclipse-pre-analyzed) or an archive
			final FileEditorInput fileEditorInput = (FileEditorInput) input;
			final IPath filePath = fileEditorInput.getPath();
			fileName = filePath.toString();
		} else {
			throw new PartInitException("Unknown editor input type '"
					+ input.getClass().getSimpleName() + "'!");
		}
		this.du = DecoJer.createDu();
		final List<T> selectedTds;
		try {
			final long currentTimeMillis = System.currentTimeMillis();
			selectedTds = this.du.read(fileName);
			log.info("Read '" + selectedTds.size() + "' TDs from file '" + fileName + "' in "
					+ (System.currentTimeMillis() - currentTimeMillis) + " ms");
		} catch (final Throwable e) {
			throw new PartInitException("Couldn't read file '" + fileName + "'!", e);
		}
		final List<CU> cus;
		try {
			cus = this.du.getCus();
		} catch (final Throwable e) {
			throw new PartInitException(
					"Couldn't create compilation units for '" + fileName + "'!", e);
		}
		if (cus.isEmpty()) {
			throw new PartInitException("Couldn't find a class in file '" + fileName + "'!");
		}
		if (selectedTds.size() == 1) {
			this.selectedCu = selectedTds.get(0).getCu();
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

}