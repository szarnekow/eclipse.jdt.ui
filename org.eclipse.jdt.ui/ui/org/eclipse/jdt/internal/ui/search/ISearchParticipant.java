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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.search.ui.text.ITextSearchResult;

/**
 * @author Thomas Mäder
 *
 */
public interface ISearchParticipant {
	void search(ITextSearchResult result, ISearchPatternData data, IProgressMonitor monitor) throws CoreException;
	/**
	 * Returns the number of units of work estimated. The returned number should be normalized such
	 * that the number of ticks for the original java search job is 1000. For example if the participant
	 * uses the same amount of time as the java search, it should return 1000, if it uses half the time,
	 * it should return 500, etc.
	 * @return The number of ticks estimated
	 */
	int estimateTicks(ISearchPatternData data);
}
