/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;

public interface IDefaultValueAdvisor {

	/**
	 * Create an Expression for
	 * @param rewrite
	 * @param importRewrite
	 * @param info
	 * @param parameterInfos
	 * @param nodes
	 * @return
	 */
	Expression createDefaultExpression(ASTRewrite rewrite, ImportRewrite importRewrite, ParameterInfo info, List parameterInfos, List nodes);

}