package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.core.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;

public class NLSInfo {
    
    private class AccessorClassHelper {
        
        private String fName;
        private ITypeBinding fBinding;
        private SourceRange fSourceRage;
        
        AccessorClassHelper(ASTNode atsNode, NLSElement nlsElement) {
            if (!nlsElement.hasTag()) {
                throw new IllegalArgumentException("NLSElement must be \"nlsed\" before.");            
            }
            TextRegion region = nlsElement.getPosition();
            ASTNode nlsStringLiteral = NodeFinder.perform(fAST, region.getOffset(), region.getLength());        
            if ((nlsStringLiteral != null) && (nlsStringLiteral.getParent() instanceof MethodInvocation)) {
                ASTNode parent = nlsStringLiteral.getParent();
                MethodInvocation methodInvocation = (MethodInvocation) parent;
                if (methodInvocation.arguments().indexOf(nlsStringLiteral) == 0) {
                    IMethodBinding binding = methodInvocation.resolveMethodBinding();
                    if (binding != null && Modifier.isStatic(binding.getModifiers())) {
                        fName = binding.getDeclaringClass().getName();
                        fSourceRage = new SourceRange(parent.getStartPosition(), parent.getLength());
                        fBinding = binding.getDeclaringClass();
                    }
                }
            }
            
            if ((fName == null) && (fBinding == null) && (fSourceRage == null)) {
                throw new IllegalArgumentException("AccessorClass Node does not exist");
            }
        }

        public ITypeBinding getBinding() {
            return fBinding;
        }
        
        public String getName() {
            return fName;
        }
        
        public SourceRange getSourceRage() {
            return fSourceRage;
        }
    }    
    
    private ICompilationUnit fCu;
    private ASTNode fAST;
    
	// TODO: get Name from "ResourceBundle.getBundle(RESOURCE_BUNDLE)"
    private final static String[] fFieldNames = {"BUNDLE_NAME", "RESOURCE_BUNDLE"};


    public NLSInfo(ICompilationUnit cu) {
        fCu = cu;
    }
    
    public ITypeBinding getAccessorClass(NLSElement nlsElement) {
        // cachen
        if (fAST == null) {
            fAST = ASTCreator.createAST(fCu, null);            
        }
        
        try {
            AccessorClassHelper accessorClass = new AccessorClassHelper(fAST, nlsElement);
            return accessorClass.getBinding();
        } catch (Exception e) {
            // dont throw illegalargument for now !!!
        }
        
        return null;
    }
    
    public AccessorClassInfo getAccessorClassInfo(NLSElement nlsElement) {
        // cachen
        if (fAST == null) {
            fAST = ASTCreator.createAST(fCu, null);            
        }
        
        try {
            AccessorClassHelper accessorClass = new AccessorClassHelper(fAST, nlsElement);
            return new AccessorClassInfo(accessorClass.getName(), accessorClass.getSourceRage());
        } catch (Exception e) {
            // dont throw illegalargument for now !!!
        }
        
        return null;
    }
    
    public IPackageFragment getPackageOfAccessorClass(ITypeBinding accessorBinding) throws JavaModelException {
        return (IPackageFragment) Bindings.findCompilationUnit(accessorBinding, fCu.getJavaProject()).getParent();
    }
    
    public String getResourceBundle(NLSElement nlsElement) throws JavaModelException {
        ITypeBinding accessorClass = getAccessorClass(nlsElement);
        return getResourceBundle(accessorClass, getPackageOfAccessorClass(accessorClass));
    }
    
    public String getResourceBundle(ITypeBinding accessorClassBinding, IPackageFragment accessorPackage) throws JavaModelException {
        IJavaProject javaProject = fCu.getJavaProject();
        IType messageClass = javaProject.findType(accessorPackage.getElementName() + '.' + accessorClassBinding.getName()); 
        if (messageClass != null) {
            ASTNode ast = ASTCreator.createAST(messageClass.getCompilationUnit(), null);
            ResourceBundleNameFinder resourceBundleNameFinder = new ResourceBundleNameFinder(NLSRefactoring.BUNDLE_NAME);
            ast.accept(resourceBundleNameFinder);
            String resourceBundleName = resourceBundleNameFinder.getResourceBundleName() + NLSRefactoring.PROPERTY_FILE_EXT;
            return resourceBundleName;                
        }
        
        return null;
    }

    public IPackageFragment getResourceBundlePackage(String resourceBundleName) throws JavaModelException {
        IJavaProject javaProject = fCu.getJavaProject();         
        return findPackageFragmentOfResource(javaProject, resourceBundleName);
    }
    
    public IFile getResourceBundleFile(NLSElement nlsElement) {        
        try {
            String resourceBundle = getResourceBundle(nlsElement);
            IPackageFragment packageFragment = getResourceBundlePackage(resourceBundle);
            if (packageFragment != null) {
	            IPath path = packageFragment.getPath().append(getResourceNameHelper(resourceBundle));
	            return (IFile) (ResourcesPlugin.getWorkspace().getRoot().findMember(path));
            }
        } catch (JavaModelException e) {
            // do nothing, no file        
        }
        return null;
    }
    
    private class ResourceBundleNameFinder extends ASTVisitor {

        private String fResourceBundleName;
        
        public ResourceBundleNameFinder(String fieldName) {
            fResourceBundleName = null;
        }        
        
        public boolean visit(FieldDeclaration node) {
            List fragments = node.fragments();
            if (fragments.size() == 1) {
                VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragments.get(0);
                String id = variableDeclarationFragment.getName().getIdentifier();
                for (int i = 0; i < fFieldNames.length; i++) {					
                	if (id.equals(fFieldNames[i])) {
                		Expression initializer = variableDeclarationFragment.getInitializer();                    
                		if (initializer != null) {
                			if(initializer instanceof StringLiteral) {
                				fResourceBundleName = ((StringLiteral) initializer).getLiteralValue();
                			} else if (initializer instanceof MethodInvocation) {
                				MethodInvocation methInvocation = (MethodInvocation) initializer;
                				Expression exp = methInvocation.getExpression();
                				if ((exp != null) && (exp instanceof TypeLiteral)) {                                
                					SimpleType simple = (SimpleType) ((TypeLiteral) exp).getType();
                					ITypeBinding typeBinding = simple.resolveBinding();
                					fResourceBundleName = typeBinding.getQualifiedName();                                                                
                				}                                                        
                			}
                		}
                	}
				}
            }
            return true;
        }

        public String getResourceBundleName() {
            return fResourceBundleName;
        }
    }
    
    private IPackageFragment findPackageFragmentOfResource(IJavaProject javaProject, String fullyQualifiedResource) throws JavaModelException {
        String packageString = getPackagePartHelper(fullyQualifiedResource);
        String resourceName = getResourceNameHelper(fullyQualifiedResource);
        IPackageFragmentRoot[] allRoots = javaProject.getAllPackageFragmentRoots();
        for (int i = 0; i < allRoots.length; i++) {
            IPackageFragmentRoot root = allRoots[i];
            if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                IPackageFragment packageFragment = root.getPackageFragment(packageString);
                if (packageFragment.exists()) {
                    Object[] resources = packageFragment.getNonJavaResources();
                    for (int j = 0; j < resources.length; j++) {
                        Object object = resources[j];
                        if (object instanceof IFile) {
                            IFile file = (IFile) object;
                            if (file.getName().equals(resourceName)) { 
                                return packageFragment;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getResourceNameHelper(String fullyQualifiedResource) {
        int propertyDot = fullyQualifiedResource.lastIndexOf('.');
        int lastPackageDot = fullyQualifiedResource.lastIndexOf('.', propertyDot - 1);
        if (lastPackageDot == -1) {
            return fullyQualifiedResource;
        } else {
            return fullyQualifiedResource.substring(lastPackageDot + 1);
        }
    }

    private String getPackagePartHelper(String fullyQualifiedResource) {
        int propertyDot = fullyQualifiedResource.lastIndexOf('.');
        int lastPackageDot = fullyQualifiedResource.lastIndexOf('.', propertyDot - 1);
        if (lastPackageDot == -1) {
            return ""; //$NON-NLS-1$
        } else {
            return fullyQualifiedResource.substring(0, lastPackageDot);
        }
    }    
}
