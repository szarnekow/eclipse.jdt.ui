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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jface.text.Assert;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerDescriptor;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerRegistry;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * 	
 * @since 3.2
 */
final class CodeAssistConfigurationBlockInProgress extends OptionsConfigurationBlock {
	private final class ComputerLabelProvider extends LabelProvider implements ITableLabelProvider {
		private final Command fCommand;
		private final IParameter fParam;

		private ComputerLabelProvider() {
			ICommandService commandSvc= (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
			fCommand= commandSvc.getCommand("org.eclipse.jdt.ui.specific_content_assist.command"); //$NON-NLS-1$
			IParameter type;
			try {
				type= fCommand.getParameters()[0];
			} catch (NotDefinedException x) {
				Assert.isTrue(false);
				type= null;
			}
			fParam= type;
		}

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0)
				return CodeAssistConfigurationBlockInProgress.this.getImage(((CompletionProposalComputerDescriptor) element).getImageDescriptor());
			return null;
		}

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) element;
			switch (columnIndex) {
				case 0:
					return desc.getName().replaceAll("&", ""); //$NON-NLS-1$ //$NON-NLS-2$
				case 1:
					final Parameterization[] params= { new Parameterization(fParam, desc.getId()) };
					final ParameterizedCommand pCmd= new ParameterizedCommand(fCommand, params);
					String key= getKeyboardShortcut(pCmd);
					return key;
			}
			return null;
		}
	}

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
		RowLayout layout= new RowLayout(SWT.VERTICAL);
		layout.spacing= 10;
		composite.setLayout(layout);
		
		final ICommandService commandSvc= (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		final Command command= commandSvc.getCommand(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		ParameterizedCommand pCmd= new ParameterizedCommand(command, null);
		String key= getKeyboardShortcut(pCmd);
		if (key == null)
			key= PreferencesMessages.CodeAssistConfigurationBlock_no_shortcut;

		new Label(composite, SWT.NONE | SWT.WRAP).setText(MessageFormat.format(PreferencesMessages.CodeAssistConfigurationBlock_computer_description, new Object[] { key }));
		
		createViewer(composite);
		
		Link link= new Link(composite, SWT.NONE | SWT.WRAP);
		link.setText(PreferencesMessages.CodeAssistConfigurationBlock_computer_link);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(getShell(), e.text, null, null);
			}
		});
		link.setLayoutData(new RowData(300, 100));
		
		
		return composite;
	}

	private void createViewer(Composite composite) {
		fViewer= CheckboxTableViewer.newCheckList(composite, SWT.FULL_SELECTION | SWT.BORDER);
		Table table= fViewer.getTable();
		table.setHeaderVisible(false);
		table.setLinesVisible(false);
		
		TableColumn nameColumn= new TableColumn(table, SWT.NONE);
		TableColumn keyColumn= new TableColumn(table, SWT.NONE);
		
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
		
		ComputerLabelProvider labelProvider= new ComputerLabelProvider();
		fViewer.setLabelProvider(labelProvider);
		fViewer.setSorter(null);
		CompletionProposalComputerRegistry registry= CompletionProposalComputerRegistry.getDefault();
		fViewer.setInput(registry);
		
		Collection descriptors= registry.getProposalComputerDescriptors();
		// assume descriptors are up2date with the preferences
		// compute widths along the way
		final int ICON_AND_CHECKBOX_WITH= 40;
		int minNameWidth= 100;
		int minKeyWidth= 50;
		for (Iterator iter= descriptors.iterator(); iter.hasNext();) {
			CompletionProposalComputerDescriptor descriptor= (CompletionProposalComputerDescriptor) iter.next();
			fViewer.setChecked(descriptor, descriptor.isEnabled());
			
			minNameWidth= Math.max(minNameWidth, computeWidth(table, labelProvider.getColumnText(descriptor, 0)) + ICON_AND_CHECKBOX_WITH);
			minKeyWidth= Math.max(minKeyWidth, computeWidth(table, labelProvider.getColumnText(descriptor, 1)));
		}
		
		nameColumn.setWidth(minNameWidth);
		keyColumn.setWidth(minKeyWidth);
	}
	
	private int computeWidth(Control control, String name) {
		GC gc= new GC(control.getDisplay());
		try {
			gc.setFont(JFaceResources.getDialogFont());
			return gc.stringExtent(name).x + 10;
		} finally {
			gc.dispose();
		}
	}

	private String getKeyboardShortcut(ParameterizedCommand command) {
		final IBindingService bindingSvc= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		final Binding[] bindings= bindingSvc.getBindings();
		for (int i= 0; i < bindings.length; i++) {
			Binding binding= bindings[i];
			if (command.equals(binding.getParameterizedCommand())) {
				TriggerSequence triggers= binding.getTriggerSequence();
				return triggers.format();
			}
		}
		return null;
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
		Collection descriptors= registry.getProposalComputerDescriptors();

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
			Collection descriptors= registry.getProposalComputerDescriptors();
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
		Collection descriptors= registry.getProposalComputerDescriptors();
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
