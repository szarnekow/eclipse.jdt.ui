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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.search.ui.text.IStructureProvider;
import org.eclipse.search.ui.text.ITextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

/**
 * @author Thomas Mäder
 *
 */
public class JavaStructureProvider implements IStructureProvider {
	private StandardJavaElementContentProvider fContentProvider;

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultCategory#getPath(java.lang.Object)
	 */
	JavaStructureProvider() {
		fContentProvider= new StandardJavaElementContentProvider();
	}
	
	public Object getParent(Object child) {
		if (child instanceof IProject || child instanceof IJavaProject)
			return null;
		if (child instanceof IJavaElement) {
			return ((IJavaElement)child).getParent();
		}
		return fContentProvider.getParent(child);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultCategory#getFile(java.lang.Object)
	 */
	public IFile getFile(Object element) {
		if (element instanceof IJavaElement) {
			IJavaElement javaElement= (IJavaElement) element;
			try {
				element= javaElement.getUnderlyingResource();
			} catch (JavaModelException e) {
				// we can't get a resource for this.
			}
		}
		if (element instanceof IFile)
			return (IFile)element;
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultCategory#findContainedMatches(java.lang.Object)
	 */
	public Match[] findContainedMatches(ITextSearchResult result, IFile file) {
		IJavaElement javaElement= JavaCore.create(file);
		Set matches= new HashSet();
		collectMatches(result, matches, javaElement);
		return (Match[]) matches.toArray(new Match[matches.size()]);
	}

	private void collectMatches(ITextSearchResult result, Set matches, IJavaElement element) {
		Match[] m= result.getMatches(element);
		if (m.length != 0) {
			for (int i= 0; i < m.length; i++) {
				matches.add(m[i]);
			}
		}
		if (element instanceof IParent) {
			IParent parent= (IParent) element;
			try {
				IJavaElement[] children= parent.getChildren();
				for (int i= 0; i < children.length; i++) {
					collectMatches(result, matches, children[i]);
				}
			} catch (JavaModelException e) {
				// we will not be tracking these results
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search2.ui.text.IStructureProvider#findContainedMatches(org.eclipse.search2.ui.text.ITextSearchResult, org.eclipse.ui.IEditorInput)
	 */
	public Match[] findContainedMatches(ITextSearchResult result, IEditorInput editorInput) {
		if (editorInput instanceof IFileEditorInput)  {
			IFileEditorInput fileEditorInput= (IFileEditorInput) editorInput;
			return findContainedMatches(result, fileEditorInput.getFile());
		} else if (editorInput instanceof IClassFileEditorInput) {
			IClassFileEditorInput classFileEditorInput= (IClassFileEditorInput) editorInput;
			Set matches= new HashSet();
			collectMatches(result, matches, classFileEditorInput.getClassFile());
			return (Match[]) matches.toArray(new Match[matches.size()]);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.IStructureProvider#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search2.ui.text.IStructureProvider#isShownInEditor(org.eclipse.search2.ui.text.Match, org.eclipse.ui.IEditorPart)
	 */
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		IEditorInput editorInput= editor.getEditorInput();
		if (match.getElement() instanceof IJavaElement) {
			IJavaElement je= (IJavaElement) match.getElement();
			if (editorInput instanceof IFileEditorInput) {
				try {
					return ((IFileEditorInput)editorInput).getFile().equals(je.getUnderlyingResource());
				} catch (JavaModelException e) {
					return false;
				}
			} else if (editorInput instanceof IClassFileEditorInput) {
				return ((IClassFileEditorInput)editorInput).getClassFile().equals(je.getAncestor(IJavaElement.CLASS_FILE));
			}
		} else if (match.getElement() instanceof IFile) {
			if (editorInput instanceof IFileEditorInput) {
				return ((IFileEditorInput)editorInput).getFile().equals(match.getElement());
			}
		}
		return false;
	}


}
