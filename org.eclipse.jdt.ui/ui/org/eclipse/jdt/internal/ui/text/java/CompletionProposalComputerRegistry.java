/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.swt.SWT;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * A registry for all extensions to the
 * <code>org.eclipse.jdt.ui.javaCompletionProposalComputer</code>
 * extension point.
 * 
 * @since 3.2
 */
public class CompletionProposalComputerRegistry {

	private static final String EXTENSION_POINT= "javaCompletionProposalComputer"; //$NON-NLS-1$
	private static final Comparator COMPARATOR= new Comparator() {
		/*
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2) {
			CompletionProposalComputerDescriptor d1= (CompletionProposalComputerDescriptor) o1;
			CompletionProposalComputerDescriptor d2= (CompletionProposalComputerDescriptor) o2;
			return d1.ordinal() - d2.ordinal();
		}
	};
	private static final SortedSet EMPTY_SORTED_SET= Collections.unmodifiableSortedSet(new TreeSet(COMPARATOR));
	
	/** The singleton instance. */
	private static CompletionProposalComputerRegistry fgSingleton= null;
	
	/**
	 * Returns the default computer registry.
	 * <p>
	 * TODO keep this or add some other singleton, e.g. JavaPlugin?
	 * </p>
	 * 
	 * @return the singleton instance
	 */
	public static synchronized CompletionProposalComputerRegistry getDefault() {
		if (fgSingleton == null) {
			fgSingleton= new CompletionProposalComputerRegistry();
			
			// TODO move somewhere else
			createCommands();
		}
		
		return fgSingleton;
	}
	
	private static void createCommands() {
		final IWorkbench workbench= PlatformUI.getWorkbench();
		ICommandService commandSvc= (ICommandService) workbench.getAdapter(ICommandService.class);
		IBindingService bindingSvc= (IBindingService) workbench.getAdapter(IBindingService.class);
		Category category= commandSvc.getCategory("org.eclipse.ui.category.edit"); //$NON-NLS-1$
		
		final Set computers= getDefault().getProposalComputerDescriptors();
		
		Set characters= new HashSet();
		KeyStroke[] strokes= {KeyStroke.getInstance(SWT.SHIFT, ' '), null};
		
		for (Iterator it= computers.iterator(); it.hasNext();) {
			final CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) it.next();
			
			Command command= commandSvc.getCommand(desc.getId() + ".direct_command"); //$NON-NLS-1$
			Assert.isTrue(!command.isDefined());
			command.define(desc.getName(), "Shows " + desc.getName() + " content assist proposals", category, null);
			char key= getUniqueKey(characters, desc.getName());
			if (key != 0) {
				strokes[1]= KeyStroke.getInstance(key);
				KeySequence sequence= KeySequence.getInstance(strokes);
				ParameterizedCommand pCommand= new ParameterizedCommand(command, null);
				String scheme= bindingSvc.getDefaultSchemeId();
				String context= "org.eclipse.jdt.ui.javaEditorScope"; //$NON-NLS-1$
				String locale= null;
				String platform= null;
				String wm= null;
				int type= Binding.SYSTEM;
				KeyBinding binding= new KeyBinding(sequence, pCommand, scheme, context, locale, platform, wm, type);
				Binding[] oldBindings= bindingSvc.getBindings();
				Binding[] newBindings= new Binding[oldBindings.length + 1];
				System.arraycopy(oldBindings, 0, newBindings, 0, oldBindings.length);
				newBindings[oldBindings.length]= binding;
				try {
					bindingSvc.savePreferences(bindingSvc.getActiveScheme(), newBindings);
				} catch (IOException x) {
					x.printStackTrace();
				}
			}
			
			AbstractHandler handler= new AbstractHandler() {
				
				/*
				 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
				 */
				public Object execute(ExecutionEvent event) throws ExecutionException {
					final JavaEditor editor= getActiveEditor();
					if (editor == null)
						return null;
					
					IAction action= editor.getAction("ContentAssistProposal"); //$NON-NLS-1$
					if (action == null || !action.isEnabled())
						return null;
					
					boolean[] oldstates= new boolean[computers.size()];
					int i= 0;
					for (Iterator it1= computers.iterator(); it1.hasNext();) {
						CompletionProposalComputerDescriptor d= (CompletionProposalComputerDescriptor) it1.next();
						oldstates[i++]= d.isEnabled();
						d.setEnabled(d == desc);
					}
					
					try {
						action.run();
					} finally {
						i= 0;
						for (Iterator it1= computers.iterator(); it1.hasNext();) {
							CompletionProposalComputerDescriptor d= (CompletionProposalComputerDescriptor) it1.next();
							d.setEnabled(oldstates[i++]);
						}
					}
					
					return null;
				}
				
				private JavaEditor getActiveEditor() {
					IEditorPart editor= workbench.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					if (editor instanceof JavaEditor)
						return (JavaEditor) editor;
					return null;
				}
				
			};
			command.setHandler(handler);
			
		}

	}

	private static char getUniqueKey(Set characters, String name) {
		String alphabet= "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; //$NON-NLS-1$
		int len= name.length();
		for (int i= 0; i < len; i++) {
			char c= Character.toUpperCase(name.charAt(i));
			if (alphabet.indexOf(c) != -1 && !characters.contains(new Character(c))) {
				characters.add(new Character(c));
				return c;
			}
		}
		
		return 0;
	}

	/**
	 * The sets of descriptors, grouped by partition type (key type:
	 * {@link String}, value type:
	 * {@linkplain SortedSet SortedSet&lt;CompletionProposalComputerDescriptor&gt;}).
	 */
	private final Map fDescriptorsByPartition= new HashMap();
	/**
	 * Unmodifiable versions of the sets stored in
	 * <code>fDescriptorsByPartition</code> (key type: {@link String},
	 * value type:
	 * {@linkplain SortedSet SortedSet&lt;CompletionProposalComputerDescriptor&gt;}).
	 */
	private final Map fPublicDescriptorsByPartition= new HashMap();
	/**
	 * All descriptors (element type:
	 * {@link CompletionProposalComputerDescriptor}).
	 */
	private final SortedSet fDescriptors= new TreeSet(COMPARATOR);
	/**
	 * Unmodifiable view of <code>fDescriptors</code>
	 */
	private final SortedSet fPublicDescriptors= Collections.unmodifiableSortedSet(fDescriptors);
	/**
	 * <code>true</code> if this registry has been loaded.
	 */
	private boolean fLoaded= false;

	/**
	 * Creates a new instance.
	 */
	public CompletionProposalComputerRegistry() {
	}

	/**
	 * Returns the set of
	 * {@link CompletionProposalComputerDescriptor}s describing all
	 * extensions to the <code>javaCompletionProposalComputer</code>
	 * extension point for the given partition type.
	 * <p>
	 * A valid partition is either one of the constants defined in
	 * {@link org.eclipse.jdt.ui.text.IJavaPartitions} or
	 * {@link org.eclipse.jface.text.IDocument#DEFAULT_CONTENT_TYPE}.
	 * An empty set is returned if there are no extensions for the given
	 * partition.
	 * </p>
	 * <p>
	 * The returned set is read-only and is sorted in the order that the
	 * extensions were read in. The returned set may change if plug-ins
	 * are loaded or unloaded while the application is running.
	 * </p>
	 * 
	 * @param partition the partition type for which to retrieve the
	 *        computer descriptors
	 * @return the set of extensions to the
	 *         <code>javaCompletionProposalComputer</code> extension
	 *         point (element type:
	 *         {@link CompletionProposalComputerDescriptor})
	 */
	public SortedSet getProposalComputerDescriptors(String partition) {
		ensureExtensionPointRead();
		SortedSet result= (SortedSet) fPublicDescriptorsByPartition.get(partition);
		return result != null ? result : EMPTY_SORTED_SET;
	}

	/**
	 * Returns the set of {@link CompletionProposalComputerDescriptor}s
	 * describing all extensions to the
	 * <code>javaCompletionProposalComputer</code> extension point.
	 * <p>
	 * The returned set is read-only and is sorted in the order that the
	 * extensions were read in. The returned set may change if plug-ins
	 * are loaded or unloaded while the application is running.
	 * </p>
	 * 
	 * @return the set of extensions to the
	 *         <code>javaCompletionProposalComputer</code> extension
	 *         point (element type:
	 *         {@link CompletionProposalComputerDescriptor})
	 */
	public SortedSet getProposalComputerDescriptors() {
		ensureExtensionPointRead();
		return fPublicDescriptors;
	}

	/**
	 * Ensures that the extensions are read and stored in
	 * <code>fDescriptorsByPartition</code>.
	 */
	private void ensureExtensionPointRead() {
		boolean reload;
		synchronized (this) {
			reload= !fLoaded;
			fLoaded= true;
		}
		if (reload)
			reload();
	}

	/**
	 * Reloads the extensions to the extension point.
	 * <p>
	 * This method can be called more than once in order to reload from
	 * a changed extension registry.
	 * </p>
	 */
	public void reload() {
		IExtensionRegistry registry= Platform.getExtensionRegistry();
		Map map= new HashMap();
		Set all= new HashSet();
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		String preference= store.getString(PreferenceConstants.CODEASSIST_DISABLED_COMPUTERS);
		Set disabled= new HashSet();
		StringTokenizer tok= new StringTokenizer(preference, "\0");  //$NON-NLS-1$
		while (tok.hasMoreTokens())
			disabled.add(tok.nextToken());
		
		IConfigurationElement[] elements= registry.getConfigurationElementsFor(JavaPlugin.getPluginId(), EXTENSION_POINT);
		for (int i= 0; i < elements.length; i++) {
			try {
				CompletionProposalComputerDescriptor desc= new CompletionProposalComputerDescriptor(elements[i], i, this);
				Set partitions= desc.getPartitions();
				for (Iterator it= partitions.iterator(); it.hasNext();) {
					String partition= (String) it.next();
					SortedSet set= (SortedSet) map.get(partition);
					if (set == null) {
						set= new TreeSet(COMPARATOR);
						map.put(partition, set);
					}
					set.add(desc);
				}
				all.add(desc);
				desc.setEnabled(!disabled.contains(desc.getId()));
				
			} catch (InvalidRegistryObjectException x) {
				/*
				 * Element is not valid any longer as the contributing
				 * plug-in was unloaded or for some other reason. Ignore
				 * this fact but do not include the extension in the
				 * list.
				 */
			}
		}
		
		synchronized (this) {
			Set partitions= map.keySet();
			fDescriptorsByPartition.keySet().retainAll(partitions);
			fPublicDescriptorsByPartition.keySet().retainAll(partitions);
			for (Iterator it= partitions.iterator(); it.hasNext();) {
				String partition= (String) it.next();
				SortedSet old= (SortedSet) fDescriptorsByPartition.get(partition);
				SortedSet current= (SortedSet) map.get(partition);
				if (old != null) {
					old.clear();
					old.addAll(current);
				} else {
					fDescriptorsByPartition.put(partition, current);
					fPublicDescriptorsByPartition.put(partition, Collections.unmodifiableSortedSet(current));
				}
			}
			
			fDescriptors.clear();
			fDescriptors.addAll(all);
		}
	}

	/**
	 * Remove the descriptor from the set of managed descriptors.
	 * Nothing happens if <code>descriptor</code> is not managed by
	 * the receiver.
	 * 
	 * @param descriptor the descriptor to be removed
	 */
	void remove(CompletionProposalComputerDescriptor descriptor) {
		Set partitions= descriptor.getPartitions();
		for (Iterator it= partitions.iterator(); it.hasNext();) {
			String partition= (String) it.next();
			SortedSet descriptors= (SortedSet) fDescriptorsByPartition.get(partition);
			if (descriptors != null) {
				// use identity since TreeSet does not check equality
				for (Iterator it2= descriptors.iterator(); it2.hasNext();) {
					CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) it2.next();
					if (desc.equals(descriptor)) {
						it2.remove();
						break;
					}
				}
			}
		}
	}
}
