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
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ResourceBundle;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.AnnotationEvent;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SelectMarkerRulerAction2;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

/**
 * A special select marker ruler action which activates quick fix if clicked on a quick fixable problem.
 */
public class JavaSelectMarkerRulerAction2 extends SelectMarkerRulerAction2 {

	public JavaSelectMarkerRulerAction2(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.JAVA_SELECT_MARKER_RULER_ACTION);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.IAnnotationListener#annotationDefaultSelected(org.eclipse.ui.texteditor.AnnotationEvent)
	 */
	public void annotationDefaultSelected(AnnotationEvent event) {
		Annotation a= event.getAnnotation();
		AbstractMarkerAnnotationModel model= getAnnotationModel();
		Position position= model.getPosition(a);
		if (position == null)
			return;
		
		if (a instanceof IJavaAnnotation) {
			IJavaAnnotation ma= (IJavaAnnotation) a;
			if (isQuickFixTarget(ma)) {
				ITextOperationTarget operation= (ITextOperationTarget) getTextEditor().getAdapter(ITextOperationTarget.class);
				final int opCode= CompilationUnitEditor.CORRECTIONASSIST_PROPOSALS;
				if (operation != null && operation.canDoOperation(opCode)) {
					getTextEditor().selectAndReveal(position.getOffset(), position.getLength());
					operation.doOperation(opCode);
					return;
				}
			}
		}
		
		// default:
		super.annotationDefaultSelected(event);
	}

	private boolean isQuickFixTarget(IJavaAnnotation ma) {
		if ("org.eclipse.ui.workbench.texteditor.error".equals(ma.getAnnotationType())) //$NON-NLS-1$
			return true;
		else if ("org.eclipse.ui.workbench.texteditor.warning".equals(ma.getAnnotationType())) //$NON-NLS-1$
			return true;
		else if ("org.eclipse.ui.workbench.texteditor.task".equals(ma.getAnnotationType())) //$NON-NLS-1$
			return true;
		else
			return false;
	}
}

