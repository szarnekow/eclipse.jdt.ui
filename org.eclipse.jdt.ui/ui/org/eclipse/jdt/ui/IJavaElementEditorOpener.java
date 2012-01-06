/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Sebastian Zarnekow - Initial API
 *******************************************************************************/
package org.eclipse.jdt.ui;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IJavaElement;

/**
 * An {@link IJavaElementEditorOpener} allows to customize the opening
 * of an editor for a given {@link IJavaElement Java element}. Thereby it
 * allows to use another editor than the default Java editor to navigate
 * to primary resources e.g. instead of generated code. 
 * @author Sebastian Zarnekow
 */
public interface IJavaElementEditorOpener {

	/**
	 * An openable editor is a lightweight handle that allows to
	 * obtain and open an {@link IEditorPart}.
	 * 
	 * Implementors should not lock resources on construction of the openable editor.
	 */
	interface IOpenableEditor {
		/**
		 * Open the editor.
		 * @return the opened editor part.
		 * @throws PartInitException if the editor could not be initialized or no workbench page is active.
		 */
		IEditorPart open() throws PartInitException;
	}
	
	/**
	 * A revealable is a lightweight handle that allows to {@link #reveal()} a certain
	 * region in an editor.
	 * 
	 * Implementors should not lock resources on construction of the revealable.
	 */
	interface IRevealable {
		/**
		 * Reveal the range in this editor as configured.
		 */
		void reveal();
	}
	
	/**
	 * Returns an openable editor iff the given element can be handled by this editor opener.
	 * Otherwise returns <code>null</code>.
	 * @param element the element that should be opened in an editor.
	 * @param activate whether to activate the editor.
	 * @param reveal whether to reveal the given element.
	 * @return an {@link IOpenableEditor} or <code>null</code> if this editor opener is not
	 *   responsible for the given element.
	 */
	IOpenableEditor openInEditor(IJavaElement element, boolean activate, boolean reveal);
	
	/**
	 * Returns an openable editor iff the given tuple of part and element can be handled by this editor opener.
	 * Otherwise returns <code>null</code>.
	 * @param part the editor part that should reveal the element.
	 * @param element the element that should be revealed.
	 * @return an {@link IOpenableEditor} or <code>null</code> if this editor opener is not
	 *   responsible for the given part and element.
	 */
	IRevealable revealInEditor(IEditorPart part, IJavaElement element);
	
	/**
	 * Returns an openable editor iff the given part can be handled by this editor opener.
	 * Otherwise returns <code>null</code>.
	 * The given region of offset and length describes the original region in the corresponding
	 * Java file.
	 * @param part the editor part that should reveal the element.
	 * @param offset the original offset that should be revealed.
	 * @param length the original length that should be revealed.
	 * @return an {@link IOpenableEditor} or <code>null</code> if this editor opener is not
	 *   responsible for the given part.
	 */
	IRevealable revealInEditor(IEditorPart part, int offset, int length);
	
}
