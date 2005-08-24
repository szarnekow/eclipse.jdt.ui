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
package org.eclipse.jdt.internal.ui.text.java;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposalComputer;
import org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.osgi.framework.Bundle;

/**
 * The description of an extension to the
 * <code>org.eclipse.jdt.ui.javaCompletionProposalComputer</code>
 * extension point. Instances are immutable and have a unique identifier
 * (see {@link #getId()}).
 * 
 * @since 3.2
 */
public final class CompletionProposalComputerDescriptor {
	/** The extension point name of the partition type attribute. */
	private static final String TYPE= "type"; //$NON-NLS-1$
	/** The extension point name of the class attribute. */
	private static final String CLASS= "class"; //$NON-NLS-1$
	/** The extension point name of the activate attribute. */
	private static final String ACTIVATE= "activate"; //$NON-NLS-1$
	/** The extension point name of the partition child elements. */
	private static final String PARTITION= "partition"; //$NON-NLS-1$
	/** Maps xml partition labels to JDT partition types. */
	private static final Set PARTITION_SET;
	
	static {
		Set partitions= new HashSet();
		partitions.add(IDocument.DEFAULT_CONTENT_TYPE);
		partitions.add(IJavaPartitions.JAVA_DOC);
		partitions.add(IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		partitions.add(IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		partitions.add(IJavaPartitions.JAVA_STRING);
		partitions.add(IJavaPartitions.JAVA_CHARACTER);
		
		PARTITION_SET= Collections.unmodifiableSet(partitions);
	}

	/** The identifier of the extension. */
	private final String fId;
	/** The name of the extension. */
	private final String fName;
	/** The class name of the provided <code>ICompletionProposalComputer</code>. */
	private final String fClass;
	/** The activate attribute value. */
	private final boolean fActivate;
	/** The partition of the extension (element type: {@link String}). */
	private final Set fPartitions;
	/** The configuration element of this extension. */
	private final IConfigurationElement fElement;
	/** The ordinal of this descriptor, which is used to define the natural order of descriptor objects constant. */
	private final int fOrdinal;
	/** The registry we are registered with. */
	private final CompletionProposalComputerRegistry fRegistry;
	/** The computer, if instantiated, <code>null</code> otherwise. */
	ICompletionProposalComputer fComputer;

	/**
	 * Creates a new descriptor.
	 * 
	 * @param element the configuration element to read
	 * @param ordinal the ordinal of this descriptor
	 * @param registry the computer registry creating this descriptor
	 */
	CompletionProposalComputerDescriptor(IConfigurationElement element, int ordinal, CompletionProposalComputerRegistry registry) throws InvalidRegistryObjectException {
		Assert.isLegal(registry != null);
		Assert.isLegal(element != null);
		
		fRegistry= registry;
		fElement= element;
		IExtension extension= element.getDeclaringExtension();
		fId= extension.getUniqueIdentifier();
		checkNotNull(fId, "id"); //$NON-NLS-1$

		String name= extension.getLabel();
		if (name.length() == 0)
			fName= fId;
		else
			fName= name;
		
		Set partitions= new HashSet();
		IConfigurationElement[] children= element.getChildren(PARTITION);
		if (children.length == 0) {
			fPartitions= PARTITION_SET; // add to all partition types if no partition is configured
		} else {
			for (int i= 0; i < children.length; i++) {
				String type= children[i].getAttributeAsIs(TYPE);
				checkNotNull(type, TYPE);
				partitions.add(type);
			}
			fPartitions= Collections.unmodifiableSet(partitions);
		}
		
		String activateAttribute= element.getAttributeAsIs(ACTIVATE);
		fActivate= Boolean.valueOf(activateAttribute).booleanValue();

		fClass= element.getAttributeAsIs(CLASS);
		checkNotNull(fClass, CLASS);
		
		fOrdinal= ordinal;
	}

	/**
	 * Checks an element that must be defined according to the extension
	 * point schema. Throws an
	 * <code>InvalidRegistryObjectException</code> if <code>obj</code>
	 * is <code>null</code>.
	 */
	private void checkNotNull(Object obj, String attribute) throws InvalidRegistryObjectException {
		if (obj == null) {
			IStatus status= new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: The extension \"" + getId() + "\" from plug-in \"" + fElement.getNamespace() + "\" did not specify a value for the required \"" + attribute + "\" attribute. Disabling the extension.", null);
			JavaPlugin.log(status);
			throw new InvalidRegistryObjectException();
		}
	}

	/**
	 * Returns the identifier of the described extension.
	 *
	 * @return Returns the id
	 */
	public String getId() {
		return fId;
	}

	/**
	 * Returns the name of the described extension.
	 * 
	 * @return Returns the name
	 */
	public String getName() {
		return fName;
	}
	
	/**
	 * Returns the partition types of the described extension.
	 * 
	 * @return the set of partition types (element type: {@link String})
	 */
	public Set getPartitions() {
		return fPartitions;
	}
	
	/**
	 * Returns a cached instance of the computer as described in the
	 * extension's xml. The computer is
	 * {@link #createComputer() created} the first time that this method
	 * is called and then cached.
	 * 
	 * @return a new instance of the completion proposal computer as
	 *         described by this descriptor
	 * @throws CoreException if the creation fails
	 * @throws InvalidRegistryObjectException if the extension is not
	 *         valid any longer (e.g. due to plug-in unloading)
	 */
	private synchronized ICompletionProposalComputer getComputer() throws CoreException, InvalidRegistryObjectException {
		if (fComputer == null && (fActivate || isPluginLoaded()))
			fComputer= createComputer();
		return fComputer;
	}

	private boolean isPluginLoaded() {
		String namespace= fElement.getDeclaringExtension().getNamespace();
		Bundle bundle= Platform.getBundle(namespace);
		return bundle != null && bundle.getState() == Bundle.ACTIVE;
	}

	/**
	 * Returns a new instance of the computer as described in the
	 * extension's xml. Note that the safest way to access the computer
	 * is by using the
	 * {@linkplain #computeCompletionProposals(TextContentAssistInvocationContext, IProgressMonitor) computeCompletionProposals}
	 * and
	 * {@linkplain #computeContextInformation(TextContentAssistInvocationContext, IProgressMonitor) computeContextInformation}
	 * methods. These delegate the functionality to the contributed
	 * computer, but handle instance creation and any exceptions thrown.
	 * 
	 * @return a new instance of the completion proposal computer as
	 *         described by this descriptor
	 * @throws CoreException if the creation fails
	 * @throws InvalidRegistryObjectException if the extension is not
	 *         valid any longer (e.g. due to plug-in unloading)
	 */
	public ICompletionProposalComputer createComputer() throws CoreException, InvalidRegistryObjectException {
		return (ICompletionProposalComputer) fElement.createExecutableExtension(CLASS);
	}
	
	public List computeCompletionProposals(TextContentAssistInvocationContext context, IProgressMonitor monitor) {
		IStatus status;
		try {
			List proposals= getComputer().computeCompletionProposals(context, monitor);
			if (proposals != null)
				return proposals;
			// API violation - log & disable
			status= new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: The extension \"" + getId() + "\" returned null to \"computeCompletionProposals()\". Disabling the extension.", null);
		} catch (InvalidRegistryObjectException x) {
			// extension has become invalid - ignore & disable
			status= new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: The extension \"" + getId() + "\" has become invalid. Disabling the extension.", x);
		} catch (CoreException x) {
			// unable to instantiate the extension - log & disable
			status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: Unable to instantiate the extension \"" + getId() + "\". Disabling the extension.", x);
		} catch (RuntimeException x) {
			// misbehaving extension - log & disable
			status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: The extension \"" + getId() + "\" has thrown an exception. Disabling the extension.", x);
		} finally {
			monitor.done();
		}
		
		JavaPlugin.log(status);
		
		fRegistry.remove(this);

		return Collections.EMPTY_LIST;
	}

	public List computeContextInformation(TextContentAssistInvocationContext context, IProgressMonitor monitor) {
		IStatus status;
		try {
			List proposals= getComputer().computeContextInformation(context, monitor);
			if (proposals != null)
				return proposals;
			// API violation - log & disable
			status= new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: The extension \"" + getId() + "\" returned null to \"computeContextInformation()\". Disabling the extension.", null);
		} catch (InvalidRegistryObjectException x) {
			// extension has become invalid - ignore & disable
			status= new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: The extension \"" + getId() + "\" has become invalid. Disabling the extension.", x);
		} catch (CoreException x) {
			// unable to instantiate the extension - log & disable
			status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: Unable to instantiate the extension \"" + getId() + "\". Disabling the extension.", x);
		} catch (RuntimeException x) {
			// misbehaving extension - log & disable
			status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Content Assist: The extension \"" + getId() + "\" has thrown an exception. Disabling the extension.", x);
		} finally {
			monitor.done();
		}
		
		JavaPlugin.log(status);
		
		fRegistry.remove(this);
		
		return Collections.EMPTY_LIST;
	}

	/**
	 * Returns the ordinal of this descriptor (used to keep the
	 * iteration order of created descriptors constant.
	 * 
	 * @return the ordinal of this descriptor
	 */
	int ordinal() {
		return fOrdinal;
	}
}