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

import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.AnnotationEvent;
import org.eclipse.ui.texteditor.IAnnotationListener;
import org.eclipse.ui.texteditor.IAnnotationService;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

/**
 * A special select marker ruler action which activates quick fix if clicked on a quick fixable problem.
 */
public class JavaSelectMarkerRulerAction2 extends TextEditorAction implements IAnnotationListener {

	public JavaSelectMarkerRulerAction2(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.JAVA_SELECT_MARKER_RULER_ACTION);
		IAnnotationService service= (IAnnotationService) editor.getAdapter(IAnnotationService.class);
		if (service != null)
			service.addAnnotationListener(this);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.TextEditorAction#setEditor(org.eclipse.ui.texteditor.ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		if (getTextEditor() != null) {
			IAnnotationService service= (IAnnotationService) getTextEditor().getAdapter(IAnnotationService.class);
			if (service != null)
				service.removeAnnotationListener(this);
		}
		super.setEditor(editor);
		if (getTextEditor() != null) {
			IAnnotationService service= (IAnnotationService) getTextEditor().getAdapter(IAnnotationService.class);
			if (service != null)
				service.addAnnotationListener(this);
		}
	}
	
	/**
	 * Returns the <code>AbstractMarkerAnnotationModel</code> of the editor's input.
	 *
	 * @return the marker annotation model or <code>null</code> if there's none
	 */
	protected AbstractMarkerAnnotationModel getAnnotationModel() {
		IDocumentProvider provider= getTextEditor().getDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(getTextEditor().getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel)
			return (AbstractMarkerAnnotationModel) model;
		return null;
	}

	/*
	 * @see org.eclipse.ui.texteditor.IAnnotationListener#annotationSelected(org.eclipse.ui.texteditor.AnnotationEvent)
	 */
	public void annotationSelected(AnnotationEvent event) {
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
		
		// default: select the annotation's position
		getTextEditor().selectAndReveal(position.offset, position.length);
	}

	private boolean isQuickFixTarget(IJavaAnnotation ma) {
		if ("org.eclipse.ui.workbench.texteditor.error".equals(ma.getAnnotationType())) //$NON-NLS-1$
			return true;
		else if ("org.eclipse.ui.workbench.texteditor.warning".equals(ma.getAnnotationType())) //$NON-NLS-1$
			return true;
		else
			return false;
	}

	/*
	 * @see org.eclipse.ui.texteditor.IAnnotationListener#annotationContextMenuAboutToShow(org.eclipse.ui.texteditor.AnnotationEvent, org.eclipse.swt.widgets.Menu)
	 */
	public void annotationContextMenuAboutToShow(AnnotationEvent event, Menu menu) {
	}
}

