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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.search.ui.ISearchJob;
import org.eclipse.search.ui.text.ITextSearchResult;

import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;

/**
 * @author Thomas Mäder
 *
 */
public class JavaSearchJob implements ISearchJob {

	private IWorkspace fWorkspace;
	private ISearchPatternData fPatternData;
	private IJavaSearchScope fScope;
	private ITextSearchResult fSearch;
	private ISearchParticipant[] fParticipants;
	private String fName;
	/**
	 * @param name
	 */
	public JavaSearchJob(
		IWorkspace workspace,
		ISearchParticipant[] participants,
		ISearchPatternData patternData,
		IJavaSearchScope scope,
		String scopeDescription,
		ITextSearchResult search) {
			
		fName= "Java Search Job";
		fParticipants= participants;
		fWorkspace= workspace;
		fPatternData= patternData;
		fScope= scope;
		fSearch= search;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) {
		fSearch.removeAll();
		fSearch.jobStarted();
		// Also search working copies
		SearchEngine engine= new SearchEngine(JavaUI.getSharedWorkingCopiesOnClasspath());

		try {
			int totalTicks= 1000;
			for (int i= 0; i < fParticipants.length; i++) {
				totalTicks+= fParticipants[i].estimateTicks(fPatternData);
			}
			monitor.beginTask("Searching", totalTicks);
			IProgressMonitor mainSearchPM= new SubProgressMonitor(monitor, 1000);
			NewSearchResultCollector collector= new NewSearchResultCollector(fSearch, mainSearchPM);
			if (fPatternData.getJavaElement() != null)
				engine.search(fWorkspace, fPatternData.getJavaElement(), fPatternData.getLimitTo(), fScope, collector);
			else
				engine.search(
					fWorkspace,
					SearchEngine.createSearchPattern(fPatternData.getPattern(), fPatternData.getSearchFor(), fPatternData.getLimitTo(), fPatternData.isCaseSensitive()),
					fScope,
					collector);
			
			for (int i= 0; i < fParticipants.length; i++) {
				IProgressMonitor participantPM= new SubProgressMonitor(monitor, fParticipants[i].estimateTicks(fPatternData));
				fParticipants[i].search(fSearch, fPatternData, participantPM);
			}
		} catch (CoreException e) {
			return e.getStatus();
		}
		fSearch.jobFinished();
		return new Status(IStatus.OK, "some plugin", 0, "Dummy message", null);
	}


	public String getName() {
		return fName;
	}


}
