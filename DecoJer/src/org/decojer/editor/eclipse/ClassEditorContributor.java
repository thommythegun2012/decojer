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

import javax.annotation.Nullable;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Manages the installation/deinstallation of global actions for multi-page editors. Responsible for
 * the redirection of global actions to the active editor. Multi-page contributor replaces the
 * contributors for the individual editors in the multi-page editor.
 */
public class ClassEditorContributor extends MultiPageEditorActionBarContributor {
	private IEditorPart activeEditorPart;
	private Action sampleAction;

	/**
	 * Creates a multi-page contributor.
	 */
	public ClassEditorContributor() {
		super();
		createActions();
	}

	@Override
	public void contributeToMenu(@Nullable final IMenuManager manager) {
		final IMenuManager menu = new MenuManager("Editor &Menu");
		manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
		menu.add(this.sampleAction);
	}

	@Override
	public void contributeToToolBar(@Nullable final IToolBarManager manager) {
		manager.add(new Separator());
		manager.add(this.sampleAction);
	}

	private void createActions() {
		this.sampleAction = new Action() {
			@Override
			public void run() {
				MessageDialog.openInformation(null, "DecoJer", "Sample Action Executed");
			}
		};
		this.sampleAction.setText("Sample Action");
		this.sampleAction.setToolTipText("Sample Action tool tip");
		this.sampleAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(IDE.SharedImages.IMG_OBJS_TASK_TSK));
	}

	/**
	 * Returns the action registed with the given text editor.
	 *
	 * @param editor
	 *            editor
	 * @param actionID
	 *            action ID
	 * @return IAction or null if editor is null.
	 */
	protected IAction getAction(final ITextEditor editor, final String actionID) {
		return editor == null ? null : editor.getAction(actionID);
	}

	/*
	 * (non-JavaDoc) Method declared in AbstractMultiPageEditorActionBarContributor.
	 */
	@Override
	public void setActivePage(@Nullable final IEditorPart part) {
		if (this.activeEditorPart == part) {
			return;
		}

		this.activeEditorPart = part;

		final IActionBars actionBars = getActionBars();
		if (actionBars != null) {

			final ITextEditor editor = part instanceof ITextEditor ? (ITextEditor) part : null;

			actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
					getAction(editor, ITextEditorActionConstants.DELETE));
			actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
					getAction(editor, ITextEditorActionConstants.UNDO));
			actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
					getAction(editor, ITextEditorActionConstants.REDO));
			actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(),
					getAction(editor, ITextEditorActionConstants.CUT));
			actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
					getAction(editor, ITextEditorActionConstants.COPY));
			actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
					getAction(editor, ITextEditorActionConstants.PASTE));
			actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
					getAction(editor, ITextEditorActionConstants.SELECT_ALL));
			actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(),
					getAction(editor, ITextEditorActionConstants.FIND));
			actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(),
					getAction(editor, IDEActionFactory.BOOKMARK.getId()));
			actionBars.updateActionBars();
		}
	}
}
