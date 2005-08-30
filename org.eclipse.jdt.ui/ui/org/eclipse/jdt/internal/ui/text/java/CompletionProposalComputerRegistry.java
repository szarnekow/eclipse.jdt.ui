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

import java.text.MessageFormat;
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

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
		}
		
		return fgSingleton;
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
	 * Returns the set of {@link CompletionProposalComputerDescriptor}s describing all extensions
	 * to the <code>javaCompletionProposalComputer</code> extension point for the given partition
	 * type.
	 * <p>
	 * A valid partition is either one of the constants defined in
	 * {@link org.eclipse.jdt.ui.text.IJavaPartitions} or
	 * {@link org.eclipse.jface.text.IDocument#DEFAULT_CONTENT_TYPE}. An empty set is returned if
	 * there are no extensions for the given partition.
	 * </p>
	 * <p>
	 * The returned set is read-only and is sorted in the order that the extensions were read in.
	 * The returned set may change if plug-ins are loaded or unloaded while the application is
	 * running or if an extension violates the API contract of
	 * {@link org.eclipse.jface.text.contentassist.ICompletionProposalComputer}. When computing
	 * proposals, it is therefore imperative to copy the the returned set before iterating over it.
	 * </p>
	 * 
	 * @param partition
	 *        the partition type for which to retrieve the computer descriptors
	 * @return the set of extensions to the <code>javaCompletionProposalComputer</code> extension
	 *         point (element type: {@link CompletionProposalComputerDescriptor})
	 */
	public SortedSet getProposalComputerDescriptors(String partition) {
		ensureExtensionPointRead();
		SortedSet result= (SortedSet) fPublicDescriptorsByPartition.get(partition);
		return result != null ? result : EMPTY_SORTED_SET;
	}

	/**
	 * Returns the set of {@link CompletionProposalComputerDescriptor}s describing all extensions
	 * to the <code>javaCompletionProposalComputer</code> extension point.
	 * <p>
	 * The returned set is read-only and is sorted in the order that the extensions were read in.
	 * The returned set may change if plug-ins are loaded or unloaded while the application is
	 * running or if an extension violates the API contract of
	 * {@link org.eclipse.jface.text.contentassist.ICompletionProposalComputer}. When computing
	 * proposals, it is therefore imperative to copy the the returned set before iterating over it.
	 * </p>
	 * 
	 * @return the set of extensions to the <code>javaCompletionProposalComputer</code> extension
	 *         point (element type: {@link CompletionProposalComputerDescriptor})
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
				 * Element is not valid any longer as the contributing plug-in was unloaded or for
				 * some other reason. Do not include the extension in the list and inform the user
				 * about it.
				 */
				Object[] args= {elements[i].toString()};
				String message= MessageFormat.format(JavaTextMessages.CompletionProposalComputerRegistry_invalid_message, args);
				IStatus status= new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, message, x);
				informUser(status);
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
	 * @param status a status object that will be logged
	 */
	void remove(CompletionProposalComputerDescriptor descriptor, IStatus status) {
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
		informUser(status);
	}

	private void informUser(IStatus status) {
		JavaPlugin.log(status);
		String title= JavaTextMessages.CompletionProposalComputerRegistry_error_dialog_title;
		String message= status.getMessage();
		MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message);
	}
}
