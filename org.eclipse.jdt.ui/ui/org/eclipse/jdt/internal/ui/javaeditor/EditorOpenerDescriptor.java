/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Sebastian Zarnekow - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.ui.IJavaElementEditorOpener;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class EditorOpenerDescriptor {

	private static final String ATT_EXTENSION = "javaElementOpeners"; //$NON-NLS-1$

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$

	private static EditorOpenerDescriptor[] contributedEditorOpeners;

	private final IConfigurationElement configurationElement;
	private IJavaElementEditorOpener editorOpener;
	private boolean active = true;
	

	/**
	 * Creates a new EditorOpenerDescriptor.
	 * @param configurationElement - the configuration element
	 */
	public EditorOpenerDescriptor(IConfigurationElement configurationElement) {
		this.configurationElement = configurationElement;
	}

	/**
	 * Alters the active state of this descriptor.
	 * A descriptors is usually set inactive if the provided {@link IJavaElementEditorOpener}
	 * throws exceptions or conflicts with another {@link IJavaElementEditorOpener}.
	 * 
	 * @param active - whether the descriptor is active or not
	 */
	public void setActive(boolean active) {
		this.active= active;
	}
	
	/**
	 * @return whether this descriptor is active or not
	 */
	public boolean isActive() {
		return active;
	}
	
	/**
	 * @return the extension id of this descriptor
	 */
	public String getID() {
		return configurationElement.getAttribute(ID);
	}

	/**
	 * @return the {@link IJavaElementEditorOpener} contributed by this descriptor
	 */
	public IJavaElementEditorOpener getEditorOpener() {
		if (active) {
			if (editorOpener == null) {
				try {
					Object extension= configurationElement.createExecutableExtension(CLASS);
					if (extension instanceof IJavaElementEditorOpener) {
						editorOpener= (IJavaElementEditorOpener) extension;
					} else {
						String message= "Invalid extension to " + ATT_EXTENSION //$NON-NLS-1$
							+ ". Must implement IJavaElementEditorOpener: " + getID(); //$NON-NLS-1$
						JavaPlugin.log(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, message));
						setActive(false);
						return null;
					}
				} catch (CoreException e) {
					JavaPlugin.log(e);
					setActive(false);
					return null;
				}
			}
			return editorOpener;
		}
		return null;
	}

	/**
	 * @return {@link EditorOpenerDescriptor} for all extensions of 'javaElementOpeners'
	 */
	static EditorOpenerDescriptor[] getEditorOpeners() {
		if (contributedEditorOpeners == null) {
			IConfigurationElement[] elements= Platform.getExtensionRegistry().getConfigurationElementsFor(JavaUI.ID_PLUGIN, ATT_EXTENSION);
			ArrayList<EditorOpenerDescriptor> res= new ArrayList<EditorOpenerDescriptor>(elements.length);

			for (int i= 0; i < elements.length; i++) {
				EditorOpenerDescriptor desc= new EditorOpenerDescriptor(elements[i]);
				res.add(desc);
			}
			contributedEditorOpeners= res.toArray(new EditorOpenerDescriptor[res.size()]);
		}
		return contributedEditorOpeners;
	}
}
