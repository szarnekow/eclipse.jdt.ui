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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.graphics.Image;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.search.ui.text.ISearchElementPresentation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * @author Thomas Mäder
 *
 */
public class UIAdapter implements ISearchElementPresentation {
	
	public static final int SHOW_ELEMENT_CONTAINER= 1;
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;
	public static final int SHOW_ELEMENT= 4;
	
	private static final String NAME_ATTRIBUTE= "Name";
	private static final String PATH_ATTRIBUTE= "Path";
	private static final String PARENT_NAME_ATTRIBUTE= "Parent Name";
	
	private static final String[] FLAT_ATTRIBUTES= { NAME_ATTRIBUTE, PARENT_NAME_ATTRIBUTE, PATH_ATTRIBUTE };
	private static final String[] STRUCTURED_ATTRIBUTES= { NAME_ATTRIBUTE };
	
	AppearanceAwareLabelProvider fLabelProvider;
	private ActionGroup fActionGroup;
	
	public UIAdapter(IViewPart view) {
		super();
		fLabelProvider= new AppearanceAwareLabelProvider();
		fActionGroup= new NewSearchViewActionGroup(view);
	}

	public String[] getSortingAtributes(boolean flatMode) {
		if (flatMode)
			return FLAT_ATTRIBUTES;
		else
			return STRUCTURED_ATTRIBUTES;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultCategory#setSortOrder(java.lang.String[])
	 */
	public void setSortOrder(String[] attributeNames, boolean flatMode) {
		for (int i= 0; i < attributeNames.length; i++) {
			if (attributeNames[i].equals(NAME_ATTRIBUTE)) {
				setOrder(JavaSearchResultLabelProvider.SHOW_ELEMENT_CONTAINER);
				return;
			} else if (attributeNames[i].equals(PATH_ATTRIBUTE)) {
				setOrder(JavaSearchResultLabelProvider.SHOW_PATH);
				return;
			} else if (attributeNames[i].equals(PARENT_NAME_ATTRIBUTE)) {
				setOrder(JavaSearchResultLabelProvider.SHOW_CONTAINER_ELEMENT);
				return;
			}
		}
	}
	
	private void setOrder(int orderFlag) {
		int flags= AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED;
		if (orderFlag == SHOW_ELEMENT_CONTAINER)
			flags |= JavaElementLabels.F_POST_QUALIFIED | JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.I_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
			| JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.D_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED;
		
		else if (orderFlag == SHOW_CONTAINER_ELEMENT)
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
			| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
		else if (orderFlag == SHOW_PATH) {
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
			| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
			flags |= JavaElementLabels.PREPEND_ROOT_PATH;
		} else if (orderFlag == SHOW_ELEMENT) {
			flags |= JavaElementLabels.M_PARAMETER_TYPES;
		}
		fLabelProvider.setTextFlags(flags);
	}
	

	public String getAttribute(Object underlyingElement, String attributeName) {
		IJavaElement javaElement= (IJavaElement) underlyingElement;
		if (PATH_ATTRIBUTE.equals(attributeName))
			return javaElement.getPath().toOSString();
		else if (NAME_ATTRIBUTE.equals(attributeName))
			return javaElement.getElementName();
		else if (PARENT_NAME_ATTRIBUTE.equals(attributeName))
			return javaElement.getParent().getElementName();
		return ""; //$NON-NLS-1$
	}

	public void showMatch(Object element, int offset, int length) throws PartInitException {
		IEditorPart editor= null;
		if (element instanceof IJavaElement) {
			IJavaElement javaElement= (IJavaElement) element;
			try {
				editor= EditorUtility.openInEditor(javaElement, false);
			} catch (PartInitException e1) {
				return;
			} catch (JavaModelException e1) {
				return;
			}
		} else if (element instanceof IFile) {
			editor= IDE.openEditor(JavaPlugin.getActivePage(), (IFile) element, false);
		}
		if (!(editor instanceof ITextEditor))
			return;
		ITextEditor textEditor= (ITextEditor) editor;
		textEditor.selectAndReveal(offset, length);
	}

	public void dispose() {
		fLabelProvider.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search2.ui.text.ISearchElementPresentation#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		return fLabelProvider.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search2.ui.text.ISearchElementPresentation#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		return fLabelProvider.getText(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search2.ui.text.ISearchElementPresentation#getActionGroup()
	 */
	public ActionGroup getActionGroup() {
		return fActionGroup;
	}

}
