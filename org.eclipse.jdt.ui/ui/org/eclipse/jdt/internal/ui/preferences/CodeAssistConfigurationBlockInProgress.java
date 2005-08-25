/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerDescriptor;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerRegistry;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * 	
 * @since 3.2
 */
public final class CodeAssistConfigurationBlockInProgress extends OptionsConfigurationBlock {
	private static final String SEPARATOR= "\0"; //$NON-NLS-1$
	private static final Key PREF_DISABLED_COMPUTERS= getJDTUIKey(PreferenceConstants.CODEASSIST_DISABLED_COMPUTERS);
	private CheckboxTableViewer fViewer;
	private final Map fImages= new HashMap();
	
	CodeAssistConfigurationBlockInProgress(IStatusChangeListener statusListener, IWorkbenchPreferenceContainer container) {
		super(statusListener, null, getAllKeys(), container);
	}

	private static Key[] getAllKeys() {
		return new Key[] {
				PREF_DISABLED_COMPUTERS,
		};
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new RowLayout(SWT.VERTICAL));
		
		new Label(composite, SWT.NONE | SWT.WRAP).setText("Check the &proposal types that should be included in the default content assist command:");
		
		createViewer(composite);
		
		return composite;
	}

	private void createViewer(Composite composite) {
		fViewer= CheckboxTableViewer.newCheckList(composite, SWT.FULL_SELECTION | SWT.BORDER);
		IContentProvider contentProvider= new IStructuredContentProvider() {
			private CompletionProposalComputerRegistry fInput;

			/*
			 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
			 */
			public Object[] getElements(Object inputElement) {
				if (fInput != null)
					return fInput.getProposalComputerDescriptors().toArray();
				return null;
			}

			/*
			 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
			 */
			public void dispose() {
			}

			/*
			 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
			 */
			public void inputChanged(Viewer v, Object oldInput, Object newInput) {
				if (newInput instanceof CompletionProposalComputerRegistry)
					fInput= (CompletionProposalComputerRegistry) newInput;
				else
					fInput= null;
			}
		};
		fViewer.setContentProvider(contentProvider);
		ILabelProvider labelProvider= new LabelProvider() {
			/*
			 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
			 */
			public String getText(Object element) {
				return ((CompletionProposalComputerDescriptor) element).getName();
			}
			
			/*
			 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
			 */
			public Image getImage(Object element) {
				return CodeAssistConfigurationBlockInProgress.this.getImage(((CompletionProposalComputerDescriptor) element).getImageDescriptor());
			}
		};
		fViewer.setLabelProvider(labelProvider);
		fViewer.setSorter(null);
		CompletionProposalComputerRegistry registry= CompletionProposalComputerRegistry.getDefault();
		fViewer.setInput(registry);
		
		SortedSet descriptors= registry.getProposalComputerDescriptors();
		// assume descriptors are up2date with the preferences
		for (Iterator iter= descriptors.iterator(); iter.hasNext();) {
			CompletionProposalComputerDescriptor descriptor= (CompletionProposalComputerDescriptor) iter.next();
			fViewer.setChecked(descriptor, descriptor.isEnabled());
		}
	}
	
	private Image getImage(ImageDescriptor imgDesc) {
		if (imgDesc == null)
			return null;
		
		Image img= (Image) fImages.get(imgDesc);
		if (img == null) {
			img= imgDesc.createImage(false);
			fImages.put(imgDesc, img);
		}
		return img;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#updateControls()
	 */
	protected void updateControls() {
		super.updateControls();
		
		// check table
		String[] disabled= getTokens(getValue(PREF_DISABLED_COMPUTERS), SEPARATOR);
		
		CompletionProposalComputerRegistry registry= CompletionProposalComputerRegistry.getDefault();
		SortedSet descriptors= registry.getProposalComputerDescriptors();

		for (Iterator it= descriptors.iterator(); it.hasNext();) {
			CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) it.next();
			boolean enabled= true;
			for (int i= 0; i < disabled.length; i++) {
				if (desc.getId().equals(disabled[i]))
					enabled= false;
			}
			fViewer.setChecked(desc, enabled);
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#controlChanged(org.eclipse.swt.widgets.Widget)
	 */
	protected void controlChanged(Widget widget) {
		if (widget == fViewer.getControl()) {
			StringBuffer buf= new StringBuffer();
			CompletionProposalComputerRegistry registry= CompletionProposalComputerRegistry.getDefault();
			SortedSet descriptors= registry.getProposalComputerDescriptors();
			for (Iterator it= descriptors.iterator(); it.hasNext();) {
				CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) it.next();
				if (!fViewer.getChecked(desc))
					buf.append(desc.getId() + SEPARATOR);
			}
			
			String newValue= buf.toString();
			String oldValue= setValue(PREF_DISABLED_COMPUTERS, newValue);
			validateSettings(PREF_DISABLED_COMPUTERS, oldValue, newValue);
		}
		super.controlChanged(widget);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#processChanges(org.eclipse.ui.preferences.IWorkbenchPreferenceContainer)
	 */
	protected boolean processChanges(IWorkbenchPreferenceContainer container) {
		CompletionProposalComputerRegistry registry= CompletionProposalComputerRegistry.getDefault();
		SortedSet descriptors= registry.getProposalComputerDescriptors();
		for (Iterator it= descriptors.iterator(); it.hasNext();) {
			CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) it.next();
			desc.setEnabled(fViewer.getChecked(desc));
		}
		
		return super.processChanges(container);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#validateSettings(org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key, java.lang.String, java.lang.String)
	 */
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getFullBuildDialogStrings(boolean)
	 */
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		// no builds triggered by our settings
		return null;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#dispose()
	 */
	public void dispose() {
		for (Iterator it= fImages.values().iterator(); it.hasNext();) {
			Image image= (Image) it.next();
			image.dispose();
		}
		
		super.dispose();
	}

}
