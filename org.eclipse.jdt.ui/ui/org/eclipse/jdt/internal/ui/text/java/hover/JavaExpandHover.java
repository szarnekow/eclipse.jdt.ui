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
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.IDoubleClickListener;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.ui.texteditor.IAnnotationListener;

import org.eclipse.ui.internal.texteditor.AnnotationExpandHover;
import org.eclipse.ui.internal.texteditor.AnnotationExpansionControl.AnnotationHoverInput;

import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;

/**
 * 
 * 
 * @since 3.0
 */
public class JavaExpandHover extends AnnotationExpandHover {

	public static class EmptyAnnotation extends Annotation {
		/*
		 * @see org.eclipse.jface.text.source.Annotation#paint(org.eclipse.swt.graphics.GC, org.eclipse.swt.widgets.Canvas, org.eclipse.swt.graphics.Rectangle)
		 */
		public void paint(GC gc, Canvas canvas, Rectangle bounds) {
			// nothing to paint
		}
	}
	
	private IDoubleClickListener fDblClickListener;
	
	/**
	 * @param ruler
	 * @param listener
	 */
	public JavaExpandHover(IVerticalRulerInfo ruler, IAnnotationListener listener, IDoubleClickListener doubleClickListener) {
		super(ruler, listener);
		fDblClickListener= doubleClickListener;
	}

	/**
	 * @param ruler
	 */
	public JavaExpandHover(IVerticalRulerInfo ruler) {
		super(ruler);
	}
	
	/*
	 * @see org.eclipse.ui.internal.texteditor.AnnotationExpandHover#getHoverInfo2(org.eclipse.jface.text.source.ISourceViewer, int)
	 */
	public Object getHoverInfo2(ISourceViewer viewer, int line) {
		IAnnotationModel model= viewer.getAnnotationModel();
		IDocument document= viewer.getDocument();
		
		if (model == null)
			return null;
		
		List exact= new ArrayList();
		HashMap messagesAtPosition= new HashMap();
		
		StyledText text= viewer.getTextWidget();
		Display display;
		if (text != null && !text.isDisposed())
			display= text.getDisplay();
		else
			display= null;
			
		
		Iterator e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Annotation annotation= (Annotation) e.next();
			Position position= model.getPosition(annotation);
			if (position == null)
				continue;
			
			if (compareRulerLine(position, document, line) == 1) {
				if (!(annotation instanceof IJavaAnnotation))
					continue;
				IJavaAnnotation ja= (IJavaAnnotation) annotation;
				
				// filter duplicate task annotations
//				if (ja.isTemporary() && ja.getAnnotationType().equals("org.eclipse.ui.workbench.texteditor.task")) //$NON-NLS-1$
//				if ((ja.hasOverlay() || !ja.isRelevant()) && ja.isTemporary() )
//				if (!ja.isRelevant())
				if (display != null && ja.getImage(display) == null)
					continue;
				if (isDuplicateMessage(messagesAtPosition, position, ja.getMessage()))
					continue;
				
				exact.add(annotation);
			}
		}
		
		sort(exact, model);
		
		if (exact.size() > 0)
			setLastRulerMouseLocation(viewer, line);
		
		if (exact.size() > 0) {
			Annotation first= (Annotation) exact.get(0);
			if (!isBreakpointAnnotation(first))
				exact.add(0, new EmptyAnnotation());
		}
		
		AnnotationHoverInput input= new AnnotationHoverInput();
		input.fAnnotations= (Annotation[]) exact.toArray(new Annotation[0]);
		input.fViewer= viewer;
		input.fRulerInfo= fVerticalRulerInfo;
		input.fAnnotationListener= fAnnotationListener;
		input.fDoubleClickListener= fDblClickListener;
		
		return input;
	}
	
	/*
	 * @see org.eclipse.ui.internal.texteditor.AnnotationExpandHover#getOrder(org.eclipse.jface.text.source.Annotation)
	 */
	protected int getOrder(Annotation annotation) {
		if (isBreakpointAnnotation(annotation)) //$NON-NLS-1$
			return 1000;
		else
			return super.getOrder(annotation);
	}

	private boolean isBreakpointAnnotation(Annotation a) {
		if (a instanceof JavaMarkerAnnotation) {
			JavaMarkerAnnotation jma= (JavaMarkerAnnotation) a;
			// HACK to get breakpoints to show up first
			return jma.getAnnotationType().equals("org.eclipse.debug.core.breakpoint"); //$NON-NLS-1$
		}
		return false;
	}

}
