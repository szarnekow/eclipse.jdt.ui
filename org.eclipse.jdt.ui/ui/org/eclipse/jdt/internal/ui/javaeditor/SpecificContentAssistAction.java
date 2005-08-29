/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerDescriptor;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerRegistry;

final class SpecificContentAssistAction extends Action {
	private final CompletionProposalComputerDescriptor fDescriptor;

	public SpecificContentAssistAction(CompletionProposalComputerDescriptor desc) {
		fDescriptor= desc;
		setText(desc.getName());
		setImageDescriptor(desc.getImageDescriptor());
		setActionDefinitionId("org.eclipse.jdt.ui.specific_content_assist.command"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		final JavaEditor editor= getActiveJavaEditor();
		if (editor == null)
			return;
		
		String computerId= fDescriptor.getId();
		
		IAction action= editor.getAction("ContentAssistProposal"); //$NON-NLS-1$
		if (action == null || !action.isEnabled())
			return;
		
		Set computers= CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors();
		boolean[] oldstates= new boolean[computers.size()];
		int i= 0;
		for (Iterator it1= computers.iterator(); it1.hasNext();) {
			CompletionProposalComputerDescriptor d= (CompletionProposalComputerDescriptor) it1.next();
			oldstates[i++]= d.isEnabled();
			d.setEnabled(d.getId().equals(computerId));
		}
		
		try {
			ITextOperationTarget target= editor.getViewer().getTextOperationTarget();
			if (target.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS))
				target.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
		} finally {
			i= 0;
			for (Iterator it1= computers.iterator(); it1.hasNext();) {
				CompletionProposalComputerDescriptor d= (CompletionProposalComputerDescriptor) it1.next();
				d.setEnabled(oldstates[i++]);
			}
		}
		
		return;
	}

	private JavaEditor getActiveJavaEditor() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page= window.getActivePage();
			if (page != null) {
				IEditorPart editor= page.getActiveEditor();
				if (editor instanceof JavaEditor)
					return (JavaEditor) editor;
			}
		}
		return null;
	}
}