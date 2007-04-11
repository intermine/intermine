package org.intermine.modelproduction.uml;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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

import ru.novosoft.uml.xmi.XMIReader;
import ru.novosoft.uml.foundation.core.*;
import ru.novosoft.uml.foundation.data_types.MMultiplicity;
import ru.novosoft.uml.model_management.MPackage;

import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.io.Reader;
import org.xml.sax.InputSource;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.modelproduction.ModelParser;
import org.intermine.util.StringUtil;

import org.apache.log4j.Logger;

/**
 * Translates a model representation in XMI to InterMine metadata (Java)
 *
 * @author Mark Woodbridge
 */
public class XmiParser implements ModelParser
{
    protected String modelName, pkgName, nameSpace;

    private Set<AttributeDescriptor> attributes;
    private Set<ReferenceDescriptor> references;
    private Set<CollectionDescriptor> collections;
    private Set<ClassDescriptor> classes = new LinkedHashSet<ClassDescriptor>();

    private static final Logger LOG = Logger.getLogger(XmiParser.class);

    /**
     * Constructor that takes the modelName
     *
     * @param modelName the name of the model to produce
     * @param pkgName the name of the package
     * @param nameSpace the XML name space
     */
    public XmiParser(String modelName, String pkgName, String nameSpace) {
        this.modelName = modelName;
        this.pkgName = pkgName;
        this.nameSpace = nameSpace;
    }

    /**
     * Read source model information in XMI format and
     * construct a InterMine Model object.
     *
     * @param reader the source XMI file to parse
     * @return the InterMine Model created
     * @throws Exception if Model not created successfully
     */
    public Model process(Reader reader) throws Exception {
        recurse(new XMIReader().parse(new InputSource(reader)));
        return new Model(modelName, nameSpace + "/" + modelName + "#", classes);
    }

    /**
     * Adds a field (UML: attribute)
     * @param attr the attribute
     */
    protected void generateAttribute(MAttribute attr) {

        String name = attr.getName();

        LOG.debug("generateAttribute(.) -- Attibute Name: " + name);

        //String type = qualify(attr.getType().getName());
        String type = attr.getType().getName();
        if (type.indexOf("[") > 0) {
            int index = type.indexOf("[");
            collections.add(new CollectionDescriptor(name,
                                                     qualify(type.substring(0, index)),
                                                     null));
        } else {
            if (type.equals("any")) {
                type = "java.lang.String";
            } else {
                type = qualify(type);
            }
            attributes.add(new AttributeDescriptor(name, type));
        }
    }

    /**
    * Adds a class or interface (UML: classifier)
    * @param cls the classifier
    */
    protected void generateClassifier(MClassifier cls) {
        String name = qualified(cls);
        String extend = generateGeneralization(cls.getGeneralizations());
        String implement;
        boolean isInterface;
        if (cls instanceof MClass) {
            isInterface = false;
            implement = generateSpecification((MClass) cls);
        } else {
            isInterface = true;
            implement = null;
        }

        references = new LinkedHashSet();
        collections = new LinkedHashSet();
        attributes = new LinkedHashSet();
        Iterator strIter = getAttributes(cls).iterator();
        while (strIter.hasNext()) {
            generateAttribute((MAttribute) strIter.next());
        }

        for (MAssociationEnd end : ((Iterable<MAssociationEnd>) cls.getAssociationEnds())) {
            generateAssociationEnd(end.getOppositeEnd());
        }

        String supers = (extend == null ? "" : extend) + " " + (implement == null ? "" : implement);
        supers = supers.trim();
        classes.add(new ClassDescriptor(name, ("".equals(supers) ? null : supers), isInterface,
                    attributes, references, collections));
    }

    /**
    * Adds a reference or collection of references to business objects (UML: association)
    * @param ae the local end of the association
    */
    protected void generateAssociationEnd(MAssociationEnd ae) {
        if (ae.isNavigable()) {
            String name = nameEnd(ae);
            String referencedType = qualified(ae.getType());
            MAssociationEnd ae2 = ae.getOppositeEnd();
            String reverseReference = ae2.isNavigable() ? nameEnd(ae2) : null;
            MMultiplicity m = ae.getMultiplicity();
            if (MMultiplicity.M1_1.equals(m) || MMultiplicity.M0_1.equals(m)) {
                references.add(new ReferenceDescriptor(name, referencedType,
                            reverseReference));
            } else {
                collections.add(new CollectionDescriptor(name, referencedType,
                            reverseReference));
            }
        }
    }

    //=================================================================

    private void recurse(MNamespace ns) {
        Iterator ownedElements = ns.getOwnedElements().iterator();
        while (ownedElements.hasNext()) {
            MModelElement me = (MModelElement) ownedElements.next();
            if (me instanceof MPackage) {

   recurse((MNamespace) me);
            }
            if (me instanceof MClass && isBusinessObject((MClassifier) me)) {
                generateClassifier((MClass) me);
            }
            if (me instanceof MInterface && isBusinessObject((MClassifier) me)) {
                generateClassifier((MInterface) me);
            }
        }
    }

    private String generateSpecification(MClass cls) {
        Collection realizations = getSpecifications(cls);
        if (realizations == null || realizations.size() == 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        Iterator clsEnum = realizations.iterator();
        while (clsEnum.hasNext()) {
            MInterface i = (MInterface) clsEnum.next();
            sb.append(qualified(i));
            if (clsEnum.hasNext()) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private String generateGeneralization(Collection generalizations) {
        if (generalizations == null || generalizations.size() == 0) {
            return null;
        }
        Collection classCollection = new LinkedHashSet();
        Iterator iter = generalizations.iterator();
        while (iter.hasNext()) {
            MGeneralization g = (MGeneralization) iter.next();
            MGeneralizableElement ge = g.getParent();
            if (ge != null) {
                classCollection.add(ge);
            }
        }
        return generateClassSet(classCollection);
    }

    private String generateClassSet(Collection classifiers) {
        if (classifiers == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        Iterator clsEnum = classifiers.iterator();
        while (clsEnum.hasNext()) {
            MClassifier cls = (MClassifier) clsEnum.next();
            sb.append(qualified(cls));
            if (clsEnum.hasNext()) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }


    private String qualified(MClassifier cls) {
        if (pkgName != null && !getPackagePath(cls).startsWith("org.intermine.model")) {
            return stripIllegal(pkgName + "." + cls.getName());
        }
        return stripIllegal(getPackagePath(cls) + "." + cls.getName());
    }


    private String stripIllegal(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if ('-' != s.charAt(i)) {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private Collection getSpecifications(MClassifier cls) {
        Collection result = new LinkedHashSet();
        Iterator depIterator = cls.getClientDependencies().iterator();

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

    private Collection getAttributes(MClassifier classifier) {
        Collection result = new LinkedHashSet();
        Iterator features = classifier.getFeatures().iterator();
        while (features.hasNext()) {
            MFeature feature = (MFeature) features.next();
            if (feature instanceof MAttribute) {
                result.add(feature);
            }
        }
        return result;
    }

   private String getPackagePath(MClassifier cls) {
        String packagePath = cls.getNamespace().getName();
        MNamespace parent = cls.getNamespace().getNamespace();
        while (parent != null) {
            packagePath = parent.getName() + "." + packagePath;
            parent = parent.getNamespace();
        }
        return packagePath;
    }

    private String nameEnd(MAssociationEnd ae) {
        String name = ae.getName();
        if (name == null || name.length() == 0) {
            name = ae.getType().getName();
            MMultiplicity m = ae.getMultiplicity();
            if (!MMultiplicity.M1_1.equals(m) && !MMultiplicity.M0_1.equals(m)) {
                name = StringUtil.pluralise(name);
            }
        }

        if (Character.isLowerCase(name.charAt(1))) {
            name = StringUtil.decapitalise(name);
        }

        return stripIllegal(name);
    }

    // converts 'any' found in MAGE-OM to InterMineObject - not a long term
    // solution, awaiting handling of collections of java.lang objects
    private String qualify(String type) {
        if ((type.equals("String")) || (type.equals("Integer"))
            || (type.equals("Float")) || (type.equals("Double"))
            || (type.equals("Boolean"))) {
            return "java.lang." + type;
        }
        if (type.equals("Date")) {
            return "java.util." + type;
        }
        if (type.startsWith("enum")) {
            return "java.lang.String";
        }
        if (type.equals("any")) {
            return "org.intermine.model.InterMineObject";
        }
        if (type.equals("Char") || type.equals("char")) {
            return "java.lang.Character";
        }
        return type;
    }

    private boolean isBusinessObject(MClassifier cls) {
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
}
