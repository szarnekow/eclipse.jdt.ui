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

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultPresentation;
import org.eclipse.search.ui.text.ITextSearchResult;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * @author Thomas Mäder
 *
 */
public class JavaSearchCategory implements ISearchResultPresentation {

	/**
	 * 
	 */
	public JavaSearchCategory() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchCategory#getText(org.eclipse.search.core.basic.ITextSearchResult)
	 */
	public String getText(ISearchResult search) {
		JavaSearchDescription desc= (JavaSearchDescription) search.getUserData();
		return getLabelPattern(search, desc);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchCategory#getTooltip(org.eclipse.search.core.basic.ITextSearchResult)
	 */
	public String getTooltip(ISearchResult search) {
		return getText(search);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchCategory#getImageDescriptor(org.eclipse.search.core.basic.ITextSearchResult)
	 */
	public ImageDescriptor getImageDescriptor(ISearchResult search) {
		JavaSearchDescription desc= (JavaSearchDescription) search.getUserData();
		if (desc.getLimitTo() == IJavaSearchConstants.IMPLEMENTORS || desc.getLimitTo() == IJavaSearchConstants.DECLARATIONS)
			return JavaPluginImages.DESC_OBJS_SEARCH_DECL;
		else
			return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchCategory#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub

	}
	

	String getLabelPattern(ISearchResult search, JavaSearchDescription description) {
		String desc= null;
		IJavaElement elementPattern= description.getElementPattern();
		int limitTo= description.getLimitTo();
		
		String prefix= "plural";
		if (((ITextSearchResult)search).getMatchCount() == 1)
			prefix= "singular";
		
		if (elementPattern != null) {
			if (limitTo == IJavaSearchConstants.REFERENCES
			&& elementPattern.getElementType() == IJavaElement.METHOD)
				desc= PrettySignature.getUnqualifiedMethodSignature((IMethod)elementPattern);
			else
				desc= elementPattern.getElementName();
			if ("".equals(desc) && elementPattern.getElementType() == IJavaElement.PACKAGE_FRAGMENT) //$NON-NLS-1$
				desc= SearchMessages.getString("JavaSearchOperation.default_package"); //$NON-NLS-1$
		}
		else
			desc= description.getStringPattern();

		String[] args= new String[] {desc, String.valueOf(((ITextSearchResult)search).getMatchCount()), description.getScopeDescription()}; //$NON-NLS-1$
		switch (limitTo) {
			case IJavaSearchConstants.IMPLEMENTORS:
				return SearchMessages.getFormattedString("JavaSearchOperation."+prefix+"ImplementorsPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.DECLARATIONS:
				return SearchMessages.getFormattedString("JavaSearchOperation."+prefix+"DeclarationsPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.REFERENCES:
				return SearchMessages.getFormattedString("JavaSearchOperation."+prefix+"ReferencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.ALL_OCCURRENCES:
				return SearchMessages.getFormattedString("JavaSearchOperation."+prefix+"OccurrencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.READ_ACCESSES:
				return SearchMessages.getFormattedString("JavaSearchOperation."+prefix+"ReadReferencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.WRITE_ACCESSES:
				return SearchMessages.getFormattedString("JavaSearchOperation."+prefix+"WriteReferencesPostfix", args); //$NON-NLS-1$
			default:
				return SearchMessages.getFormattedString("JavaSearchOperation."+prefix+"OccurrencesPostfix", args); //$NON-NLS-1$;
		}
	}	
}
