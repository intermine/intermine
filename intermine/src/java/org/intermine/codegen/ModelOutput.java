package org.flymine.codegen;

// Most of this code originated in the ArgoUML project, which carries
// the following copyright
//
// Copyright (c) 1996-2001 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

import ru.novosoft.uml.foundation.core.*;
import ru.novosoft.uml.foundation.data_types.*;
import ru.novosoft.uml.model_management.*;
import ru.novosoft.uml.foundation.extension_mechanisms.*;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

import org.flymine.util.StringUtil;

public abstract class ModelOutput
{
    protected static final Logger LOG = Logger.getLogger(ModelOutput.class);
    private MModel mmodel;

    protected static final String INDENT = "    ";
    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public ModelOutput(MModel mmodel) {
        this.mmodel = mmodel;
        BasicConfigurator.configure();
    }

    protected String generate(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof MAttribute) {
            return generateAttribute((MAttribute) o);
        }
        if (o instanceof MClassifier) {
            return generateClassifier((MClassifier) o);
        }
        if (o instanceof MAssociationEnd) {
            return generateAssociationEnd((MAssociationEnd) o);
        }
        if (o instanceof MParameter) {
            return generateParameter((MParameter) o);
        }
        if (o instanceof MOperation) {
            return generateOperation((MOperation) o);
        }
//         if (o instanceof MPackage)
//             return generatePackage((MPackage) o);
//        if (o instanceof MTaggedValue) {
//            return generateTaggedValue((MTaggedValue) o);
//        }
//         if (o instanceof MAssociation)
//             return generateAssociation((MAssociation)o);
//         if (o instanceof MMultiplicity)
//             return generateMultiplicity((MMultiplicity)o);

//         if (o instanceof MExpression)
//             return generateExpression((MExpression) o);
//         if (o instanceof String)
//             return generateName((String) o);
//         if (o instanceof MModelElement)
//             return generateName(((MModelElement)o).getName());

        return o.toString();
    }

    protected  String generateOperation(MOperation op) { return ""; }
    protected abstract String generateAttribute(MAttribute attr);
    protected  String generateParameter(MParameter param) { return ""; }
    protected  String generatePackage(MPackage p) { return ""; }
    protected abstract String generateClassifier(MClassifier cls);
    protected  String generateTaggedValue(MTaggedValue s) { return ""; }
    protected  String generateAssociation(MAssociation a) { return ""; }
    protected  String generateMultiplicity(MMultiplicity m) { return ""; }
    protected  String generateAssociationEnd(MAssociationEnd ae) { return ""; }

    protected abstract void generateFileStart(File path);
    protected abstract void generateFile(MClassifier cls, File path);
    protected abstract void generateFileEnd(File path);

    protected String generateExpression(MExpression expr) {
        if (expr == null) {
            return "";
        }
        return generateUninterpreted(expr.getBody());
    }

    protected String generateExpression(MConstraint expr) {
        if (expr == null) {
            return "";
        }
        return generateExpression(expr.getBody());
    }

    protected String generateName(String n) {
        return n;
    }

    protected String generateCapitalName(String n) {
        if (n.length() <= 1) {
            return n.toUpperCase();
        }
        return n.substring(0, 1).toUpperCase() + n.substring(1, n.length());
    }

    protected String generateNoncapitalName(String n) {
        if (n.length() <= 1) {
            return n.toLowerCase();
        }
        // If the first and second letter are capital, leave as it is
        if (Character.isUpperCase(n.charAt(1)) && Character.isUpperCase(n.charAt(2))) {
            return n;
        }
        return n.substring(0, 1).toLowerCase() + n.substring(1, n.length());
    }

    protected String generateLowercaseName(String n) {
        return n.toLowerCase();
    }
    
    protected String generateSqlCompatibleName(String n) {
        //n should start with a lower case letter
        if (n.equalsIgnoreCase("end")) {
            return StringUtil.toSameInitialCase("finish", n);
        }
        if (n.equalsIgnoreCase("id")) {
            return StringUtil.toSameInitialCase("identifier", n);
        }
        if (n.equalsIgnoreCase("index")) {
            return StringUtil.toSameInitialCase("number", n);
        }
        return n;
    }
    
    protected String generateUninterpreted(String un) {
        if (un == null) {
            return "";
        }
        return un;
    }

    protected String generateClassifierRef(MClassifier cls) {
        if (cls == null) {
            return "";
        }
        return cls.getName();
    }

    protected String getPackagePath(MClassifier cls) {
        String packagePath = cls.getNamespace().getName();
        MNamespace parent = cls.getNamespace().getNamespace();
        while (parent != null) {
            // ommit root package name; it's the model's root
            //if (parent.getNamespace() != null)
                packagePath = parent.getName() + "." + packagePath;
            parent = parent.getNamespace();
        }
        return packagePath;
    }

    public void output(File path) {
        MNamespace ns = (MNamespace) mmodel;

        // Set up file or directories etc.
        generateFileStart(path);

        recurse(ns, path);

        // Any finishing off things to do at the end of the model
        generateFileEnd(path);
    }

    private void recurse(MNamespace ns, File path) {
        Iterator ownedElements = ns.getOwnedElements().iterator();
        while (ownedElements.hasNext()) {
            MModelElement me = (MModelElement) ownedElements.next();
            if (me instanceof MPackage) {
                recurse((MNamespace) me, path);
            }
            if (me instanceof MClass && isBusinessObject((MClassifier) me)) {
                generateFile((MClass) me, path);
            }
            if (me instanceof MInterface && isBusinessObject((MClassifier) me)) {
                generateFile((MInterface) me, path);
            }
        }
    }
    
    public boolean isBusinessObject(MClassifier cls) {
        String name = cls.getName();
        if (name == null || name.length() == 0 
            || name.equals("void") || name.equals("char") || name.equals("byte")
            || name.equals("short") || name.equals("int") || name.equals("long")
            || name.equals("boolean") || name.equals("float") || name.equals("double")) {
            return false;
        }

        String packagePath = getPackagePath(cls);

        if (packagePath.endsWith("java.lang") || packagePath.endsWith("java.util")) {
            return false;
        }
        
        return true;
    }

    protected void initFile(File f) {
        //if the file exists, delete it and start again - we are not trying to update
        if (f.exists()) {
            try {
                f.delete();
                LOG.info("Deleted " + f.getPath());
            } catch (Exception exp) {
                LOG.debug("Cannot delete: " + f.getPath());
            }
        }
        LOG.info("Generating (new) " + f.getPath());
    }

    protected void outputToFile(File f, String src) {
        BufferedWriter fos = null;
        try {
            fos = new BufferedWriter(new FileWriter (f, true));
            fos.write (src);
        } catch (IOException exp) {
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException exp) {
                LOG.debug("FAILED: " + f.getPath());
            }
        }
    }

    protected MParameter getReturnParameter(MOperation operation) {
        List returnParams = new ArrayList();
        MParameter firstReturnParameter = null;
        Iterator params = operation.getParameters().iterator();
        while (params.hasNext()) {
            MParameter parameter = (MParameter) params.next();
            if ((parameter.getKind()).equals(MParameterDirectionKind.RETURN)) {
                returnParams.add(parameter);
            }
        }
        switch (returnParams.size()) {
        case 1:
            return (MParameter) returnParams.get(0);
        case 0:
            LOG.info("No ReturnParameter found!");
            return null;
        default:
            LOG.info("More than one ReturnParameter found, returning first!");
            return (MParameter) returnParams.get(0);
        }
    }

    protected Collection getOperations(MClassifier classifier) {
        Collection result = new ArrayList();
        Iterator features = classifier.getFeatures().iterator();
        while (features.hasNext()) {
            MFeature feature = (MFeature) features.next();
            if (feature instanceof MOperation) {
                result.add(feature);
            }
        }
        return result;
    }

    protected Collection getAttributes(MClassifier classifier) {
        Collection result = new ArrayList();
        Iterator features = classifier.getFeatures().iterator();
        while (features.hasNext()) {
            MFeature feature = (MFeature) features.next();
            if (feature instanceof MAttribute) {
                result.add(feature);
            }
        }
        return result;
    }

    protected Collection getSpecifications(MClassifier cls) {
        Collection result = new ArrayList();
        Collection deps = cls.getClientDependencies();
        Iterator depIterator = deps.iterator();

        while (depIterator.hasNext()) {
            MDependency dep = (MDependency) depIterator.next();
            if ((dep instanceof MAbstraction) 
                && dep.getStereotype() != null
                && dep.getStereotype().getName() != null
                && dep.getStereotype().getName().equals("realize")) {
                MInterface i = (MInterface) dep.getSuppliers().toArray()[0];
                result.add(i);
            }
        }
        return result;
    }

    protected String generateGeneralization(Collection generalizations) {
        if (generalizations == null) {
            return "";
        }
        Collection classes = new ArrayList();
        Iterator enum = generalizations.iterator();
        while (enum.hasNext()) {
            MGeneralization g = (MGeneralization) enum.next();
            MGeneralizableElement ge = g.getParent();
            if (ge != null) {
                classes.add(ge);
            }
        }
        return generateClassList(classes);
    }

    protected String generateClassList(Collection classifiers) {
        if (classifiers == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        Iterator clsEnum = classifiers.iterator();
        while (clsEnum.hasNext()) {
            sb.append(generateClassifierRef((MClassifier) clsEnum.next()));
            if (clsEnum.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}

