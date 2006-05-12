/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public final class ExperimentalResultCollector extends CompletionProposalCollector {

	private final boolean fIsGuessArguments;

	public ExperimentalResultCollector(JavaContentAssistInvocationContext context) {
		super(context.getCompilationUnit());
		setInvocationContext(context);
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		fIsGuessArguments= preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.ResultCollector#createJavaCompletionProposal(org.eclipse.jdt.core.CompletionProposal)
	 */
	protected IJavaCompletionProposal createJavaCompletionProposal(CompletionProposal proposal) {
		switch (proposal.getKind()) {
			case CompletionProposal.METHOD_REF:
				return createMethodReferenceProposal(proposal);
			case CompletionProposal.TYPE_REF:
				return createTypeProposal(proposal);
			default:
				return super.createJavaCompletionProposal(proposal);
		}
	}

	private IJavaCompletionProposal createMethodReferenceProposal(CompletionProposal methodProposal) {
		String completion= String.valueOf(methodProposal.getCompletion());
		// super class' behavior if this is not a normal completion or has no
		// parameters
		if ((completion.length() == 0) || ((completion.length() == 1) && completion.charAt(0) == ')') || Signature.getParameterCount(methodProposal.getSignature()) == 0 || getContext().isInJavadoc())
			return super.createJavaCompletionProposal(methodProposal);

		LazyJavaCompletionProposal proposal;
		ICompilationUnit compilationUnit= getCompilationUnit();
		if (compilationUnit != null && fIsGuessArguments)
			proposal= new ParameterGuessingProposal(methodProposal, getInvocationContext());
		else
			proposal= new ExperimentalProposal(methodProposal, getInvocationContext());
		return proposal;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.ResultCollector#createTypeCompletion(org.eclipse.jdt.core.CompletionProposal)
	 */
	private IJavaCompletionProposal createTypeProposal(CompletionProposal typeProposal) {
		final ICompilationUnit cu= getCompilationUnit();
		if (cu == null || getContext().isInJavadoc())
			return super.createJavaCompletionProposal(typeProposal);

		IJavaProject project= cu.getJavaProject();
		if (!shouldProposeGenerics(project))
			return super.createJavaCompletionProposal(typeProposal);

		char[] completion= typeProposal.getCompletion();
		// don't add parameters for import-completions nor for proposals with an empty completion (e.g. inside the type argument list)
		if (completion.length == 0 || completion[completion.length - 1] == ';' || completion[completion.length - 1] == '.')
			return super.createJavaCompletionProposal(typeProposal);

		LazyJavaCompletionProposal newProposal= new GenericJavaTypeProposal(typeProposal, getInvocationContext());
		return newProposal;
	}

	/**
	 * Returns <code>true</code> if generic proposals should be allowed,
	 * <code>false</code> if not. Note that even though code (in a library)
	 * may be referenced that uses generics, it is still possible that the
	 * current source does not allow generics.
	 *
	 * @return <code>true</code> if the generic proposals should be allowed,
	 *         <code>false</code> if not
	 */
	private final boolean shouldProposeGenerics(IJavaProject project) {
		String sourceVersion;
		if (project != null)
			sourceVersion= project.getOption(JavaCore.COMPILER_SOURCE, true);
		else
			sourceVersion= JavaCore.getOption(JavaCore.COMPILER_SOURCE);

		return sourceVersion != null && JavaCore.VERSION_1_5.compareTo(sourceVersion) <= 0;
	}
}
