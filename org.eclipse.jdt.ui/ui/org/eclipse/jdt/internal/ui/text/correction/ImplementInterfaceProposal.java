/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 *
 */
public class ImplementInterfaceProposal extends LinkedCorrectionProposal {

	private IBinding fBinding;
	private CompilationUnit fAstRoot;
	private ITypeBinding fNewInterface;

	public ImplementInterfaceProposal(ICompilationUnit targetCU, ITypeBinding binding, CompilationUnit astRoot, ITypeBinding newInterface, int relevance) {
		super("", targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$

		Assert.isTrue(binding != null && Bindings.isDeclarationBinding(binding));

		fBinding= binding;
		fAstRoot= astRoot;
		fNewInterface= newInterface;

		String[] args= { binding.getName(), Bindings.getRawName(newInterface) };
		setDisplayName(Messages.format(CorrectionMessages.ImplementInterfaceProposal_name, args));
	}

	protected ASTRewrite getRewrite() throws CoreException {
		ASTNode boundNode= fAstRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;
		CompilationUnit newRoot= fAstRoot;
		if (boundNode != null) {
			declNode= boundNode; // is same CU
		} else {
			newRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		ImportRewrite imports= createImportRewrite(newRoot);
		
		if (declNode instanceof TypeDeclaration) {
			AST ast= declNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);

			Type newInterface= imports.addImport(fNewInterface, ast);
			ListRewrite listRewrite= rewrite.getListRewrite(declNode, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
			listRewrite.insertLast(newInterface, null);

			// set up linked mode
			final String KEY_TYPE= "type"; //$NON-NLS-1$
			addLinkedPosition(rewrite.track(newInterface), true, KEY_TYPE);
				return rewrite;
		}
		return null;
	}


}
