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
	private Boolean active = Boolean.TRUE;

	public EditorOpenerDescriptor(IConfigurationElement configurationElement) {
		this.configurationElement = configurationElement;
	}

	public String getID() {
		return configurationElement.getAttribute(ID);
	}

	public IJavaElementEditorOpener getEditorOpener() {
		if (active.booleanValue()) {
			if (editorOpener == null) {
				try {
					Object extension= configurationElement.createExecutableExtension(CLASS);
					if (extension instanceof IJavaElementEditorOpener) {
						editorOpener= (IJavaElementEditorOpener) extension;
					} else {
						String message= "Invalid extension to " + ATT_EXTENSION //$NON-NLS-1$
							+ ". Must implement IJavaElementEditorOpener: " + getID(); //$NON-NLS-1$
						JavaPlugin.log(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, message));
						active = Boolean.FALSE;
						return null;
					}
				} catch (CoreException e) {
					JavaPlugin.log(e);
					active= Boolean.FALSE;
					return null;
				}
			}
			return editorOpener;
		}
		return null;
	}

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
