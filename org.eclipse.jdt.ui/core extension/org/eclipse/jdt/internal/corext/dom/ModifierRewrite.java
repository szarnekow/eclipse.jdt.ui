/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;

/**
 *
 */
public class ModifierRewrite {
	
	private static final int VISIBILITY_MODIFIERS= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;
	public static final Modifier.ModifierKeyword[] ALL_KEYWORDS= {
			Modifier.ModifierKeyword.PUBLIC_KEYWORD,
			Modifier.ModifierKeyword.PROTECTED_KEYWORD,
			Modifier.ModifierKeyword.PRIVATE_KEYWORD,
			Modifier.ModifierKeyword.STATIC_KEYWORD,
			Modifier.ModifierKeyword.ABSTRACT_KEYWORD,
			Modifier.ModifierKeyword.FINAL_KEYWORD,
			Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD,
			Modifier.ModifierKeyword.STRICTFP_KEYWORD,
			Modifier.ModifierKeyword.VOLATILE_KEYWORD,
			Modifier.ModifierKeyword.NATIVE_KEYWORD,
			Modifier.ModifierKeyword.TRANSIENT_KEYWORD
	};
	
	private ListRewrite fModifierRewrite;
	private AST fAst;


	public ModifierRewrite create(ASTRewrite rewrite, ASTNode declNode) {
		return new ModifierRewrite(rewrite, declNode);
	}
	
	private ModifierRewrite(ASTRewrite rewrite, ASTNode declNode) {
		ListRewrite modifierRewrite= null;
		if (declNode instanceof MethodDeclaration) {
			modifierRewrite= rewrite.getListRewrite(declNode, MethodDeclaration.MODIFIERS2_PROPERTY);
		} else if (declNode instanceof VariableDeclarationFragment) {
			ASTNode parent= declNode.getParent();
			if (parent instanceof FieldDeclaration) {
				modifierRewrite= rewrite.getListRewrite(parent, FieldDeclaration.MODIFIERS2_PROPERTY);
			} else if (parent instanceof VariableDeclarationStatement) {
				modifierRewrite= rewrite.getListRewrite(parent, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
			} else if (parent instanceof VariableDeclarationExpression) {
				modifierRewrite= rewrite.getListRewrite(parent, VariableDeclarationExpression.MODIFIERS2_PROPERTY);
			}
		} else if (declNode instanceof SingleVariableDeclaration) {
			modifierRewrite= rewrite.getListRewrite(declNode, SingleVariableDeclaration.MODIFIERS2_PROPERTY);	
		} else if (declNode instanceof TypeDeclaration) {
			modifierRewrite= rewrite.getListRewrite(declNode, TypeDeclaration.MODIFIERS2_PROPERTY);	
		}
		Assert.isNotNull(modifierRewrite);
		
		fModifierRewrite= modifierRewrite;
		fAst= declNode.getAST();

	}
	
	public void setModifiers(int modfiers, TextEditGroup editGroup) {
		setModifiers(modfiers, -1, editGroup);
	}
	
	public void setVisibility(int visibilityFlags, TextEditGroup editGroup) {
		setModifiers(visibilityFlags, VISIBILITY_MODIFIERS, editGroup);
	}
	
	
	private void setModifiers(int modfiers, int consideredFlags, TextEditGroup editGroup) {
		// remove modfiers
		int newModifiers= modfiers & consideredFlags;
		
		List originalList= fModifierRewrite.getOriginalList();
		for (int i= 0; i < originalList.size(); i++) {
			ASTNode curr= (ASTNode) originalList.get(i);
			if (curr instanceof Modifier) {
				int flag= ((Modifier)curr).getKeyword().toFlagValue();
				if ((consideredFlags & flag) != 0) {
					if ((newModifiers & flag) == 0) {
						fModifierRewrite.remove(curr, editGroup);
					}
					newModifiers &= ~flag;
				}
			}
		}
		// add modifiers
		for (int i= 0; i < ALL_KEYWORDS.length; i++) {
			if ((newModifiers & ALL_KEYWORDS[i].toFlagValue()) != 0) {
				Modifier curr= fAst.newModifier(ALL_KEYWORDS[i]);
				if ((curr.getKeyword().toFlagValue() & VISIBILITY_MODIFIERS) != 0) {
					fModifierRewrite.insertFirst(curr, editGroup);
				} else {
					fModifierRewrite.insertLast(curr, editGroup);
				}
			}
		}
	}
	

}
