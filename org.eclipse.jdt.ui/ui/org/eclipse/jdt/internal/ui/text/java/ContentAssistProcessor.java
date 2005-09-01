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
package org.eclipse.jdt.internal.ui.text.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext;

/**
 * A content assist processor that aggregates the proposals of the
 * {@link org.eclipse.jface.text.contentassist.ICompletionProposalComputer}s
 * contributed via the
 * <code>org.eclipse.jdt.ui.javaCompletionProposalComputer</code>
 * extension point.
 * <p>
 * Subclasses may extend:
 * <ul>
 * <li><code>createContext</code> to modify the context object passed
 * to the computers</li>
 * <li><code>createProgressMonitor</code> to change the way progress
 * is reported</li>
 * <li><code>filterAndSort</code> to add sorting and filtering</li>
 * <li><code>getContextInformationValidator</code> to add context
 * validation (needed if any contexts are provided)</li>
 * <li><code>getErrorMessage</code> to change error reporting</li>
 * </ul>
 * </p>
 * 
 * @since 3.2
 */
public class ContentAssistProcessor implements IContentAssistProcessor {

	private char[] fCompletionAutoActivationCharacters;
	private final List fProviders;
	private int fRepetition;
	
	public ContentAssistProcessor(String partition) {
		fProviders= CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors(partition);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public final ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IProgressMonitor monitor= createProgressMonitor();
		monitor.beginTask(JavaTextMessages.ContentAssistProcessor_computing_proposals, fProviders.size() + 1);

		TextContentAssistInvocationContext context= createContext(viewer, offset);
		
		monitor.subTask(JavaTextMessages.ContentAssistProcessor_collecting_proposals);
		List proposals= collectProposals(viewer, offset, monitor, context);

		monitor.subTask(JavaTextMessages.ContentAssistProcessor_sorting_proposals);
		List filtered= filterAndSortProposals(proposals, monitor, context);
		
		ICompletionProposal[] result= (ICompletionProposal[]) filtered.toArray(new ICompletionProposal[filtered.size()]);
		monitor.done();
		return result;
	}

	private List collectProposals(ITextViewer viewer, int offset, IProgressMonitor monitor, TextContentAssistInvocationContext context) {
		List proposals= new ArrayList();
		List providers= getSelectedProviders();
		for (Iterator it= providers.iterator(); it.hasNext();) {
			CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) it.next();
			List computed= desc.computeCompletionProposals(context, new SubProgressMonitor(monitor, 1));
			proposals.addAll(computed);
		}
		
		return proposals;
	}

	private List getSelectedProviders() {
		int selection= fRepetition % (fProviders.size() + 1);
		if (selection == 0)
			return new ArrayList(fProviders);
		else
			return Collections.singletonList(fProviders.get(selection - 1));
	}

	/**
	 * Filters and sorts the proposals. The passed list may be modified
	 * and returned, or a new list may be created and returned.
	 * 
	 * @param proposals the list of collected proposals (element type:
	 *        {@link ICompletionProposal})
	 * @param monitor a progress monitor
	 * @param context TODO
	 * @return the list of filtered and sorted proposals, ready for
	 *         display (element type: {@link ICompletionProposal})
	 */
	protected List filterAndSortProposals(List proposals, IProgressMonitor monitor, TextContentAssistInvocationContext context) {
		return proposals;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		IProgressMonitor monitor= createProgressMonitor();
		monitor.beginTask(JavaTextMessages.ContentAssistProcessor_computing_contexts, fProviders.size() + 1);
		
		monitor.subTask(JavaTextMessages.ContentAssistProcessor_collecting_contexts);
		List proposals= collectContextInformation(viewer, offset, monitor);

		monitor.subTask(JavaTextMessages.ContentAssistProcessor_sorting_contexts);
		List filtered= filterAndSortContextInformation(proposals, monitor);
		
		IContextInformation[] result= (IContextInformation[]) filtered.toArray(new IContextInformation[filtered.size()]);
		monitor.done();
		return result;
	}

	private List collectContextInformation(ITextViewer viewer, int offset, IProgressMonitor monitor) {
		List proposals= new ArrayList();
		TextContentAssistInvocationContext context= createContext(viewer, offset);
		
		List providers= getSelectedProviders();
		for (Iterator it= providers.iterator(); it.hasNext();) {
			CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) it.next();
			List computed= desc.computeContextInformation(context, new SubProgressMonitor(monitor, 1));
			proposals.addAll(computed);
		}
		
		return proposals;
	}

	/**
	 * Filters and sorts the context information objects. The passed
	 * list may be modified and returned, or a new list may be created
	 * and returned.
	 * 
	 * @param contexts the list of collected proposals (element type:
	 *        {@link IContextInformation})
	 * @param monitor a progress monitor
	 * @return the list of filtered and sorted proposals, ready for
	 *         display (element type: {@link IContextInformation})
	 */
	protected List filterAndSortContextInformation(List contexts, IProgressMonitor monitor) {
		return contexts;
	}

	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 *
	 * @param activationSet the activation set
	 */
	public final void setCompletionProposalAutoActivationCharacters(char[] activationSet) {
		fCompletionAutoActivationCharacters= activationSet;
	}


	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public final char[] getCompletionProposalAutoActivationCharacters() {
		return fCompletionAutoActivationCharacters;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/**
	 * Creates a progress monitor.
	 * <p>
	 * The default implementation creates a
	 * <code>NullProgressMonitor</code>.
	 * </p>
	 * 
	 * @return a progress monitor
	 */
	protected IProgressMonitor createProgressMonitor() {
		return new NullProgressMonitor();
	}

	/**
	 * Creates the context that is passed to the completion proposal
	 * computers.
	 * 
	 * @param viewer the viewer that content assist is invoked on
	 * @param offset the content assist offset
	 * @return the context to be passed to the computers
	 */
	protected TextContentAssistInvocationContext createContext(ITextViewer viewer, int offset) {
		return new TextContentAssistInvocationContext(viewer, offset);
	}

	public void setRepeatedInvocation(int repetition) {
		fRepetition= repetition;
	}
	
	public String getRepetitionMessage() {
		int selection= fRepetition % (fProviders.size() + 1);
		if (selection == 0)
			return null;
		else
			return ((CompletionProposalComputerDescriptor) fProviders.get(selection - 1)).getName().replaceAll("&", "");  //$NON-NLS-2$//$NON-NLS-3$
	}
}
