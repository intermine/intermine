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

import ru.novosoft.uml.xmi.XMIReader;
import ru.novosoft.uml.foundation.core.*;
import ru.novosoft.uml.foundation.data_types.*;
import ru.novosoft.uml.model_management.*;
import ru.novosoft.uml.foundation.extension_mechanisms.*;

import java.io.File;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import org.xml.sax.InputSource;

public class OJBModelOutput extends ModelOutput
{
    //    private File f;
    private Collection fields = new HashSet();
    private StringBuffer references, collections;

    public OJBModelOutput(MModel mmodel) {
        super(mmodel);
    }

    protected String generateAttribute (MAttribute attr) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT + INDENT + "<field-descriptor name=\"")
            .append(generateName(attr.getName()))
            .append("\" column=\"")
            .append(generateSqlCompatibleName(attr.getName()))
            .append("\" jdbc-type=\"")
            .append(generateOJBSqlType(attr.getType()))
            .append("\" />\n");
        return sb.toString();
    }

    protected String generateClassifier(MClassifier cls) {
        StringBuffer sb = new StringBuffer();
        sb.append(generateClassifierStart(cls))
            .append((cls.isAbstract() || cls instanceof MInterface)
                    ? new StringBuffer() : generateClassifierBody(cls))
            .append(generateClassifierEnd(cls));
        return sb.toString();
    }

    protected void generateFileStart(File path) {
        initFile(path);

        outputToFile(path, generateHeader());
    }

    protected void generateFile(MClassifier cls, File path) {
        outputToFile(path, generate(cls));
    }

    protected void generateFileEnd(File path) {
        outputToFile(path, generateFooter());
    }

    //=================================================================

    private String generateHeader() {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<!DOCTYPE descriptor-repository SYSTEM \"repository.dtd\" [\n")
            .append("<!ENTITY internal SYSTEM \"repository_internal.xml\">\n]>\n\n")
            .append("<descriptor-repository version=\"0.9.9\""
                    + " isolation-level=\"read-uncommitted\">\n");
        return sb.toString();
    }

    private String generateFooter () {
        StringBuffer sb = new StringBuffer();
        sb.append("&internal;\n</descriptor-repository>\n");
        return sb.toString();
    }

    private StringBuffer generateClassifierStart (MClassifier cls) {
        references = new StringBuffer();
        collections = new StringBuffer();

        MClassifier parent = null;
        String tableName = null;
        Iterator parents = cls.getGeneralizations().iterator();
        if (parents.hasNext()) {
            parent = (MClassifier) ((MGeneralization) parents.next()).getParent();
            tableName = parent.getName();
        } else {
            tableName = cls.getName();
        }
            
        StringBuffer sb = new StringBuffer ();
        sb.append(INDENT + "<class-descriptor class=\"")
            .append(generateQualified(cls) + "\"")
            //.append(parent == null ? "" : " extends=\"" + generateQualified(parent) + "\"")
            .append((cls.isAbstract() || cls instanceof MInterface) 
                    ? "" : " table=\"" + tableName + "\"")
            .append(">\n");

        Collection ems = new ArrayList(); // extent members i.e. subclasses or implementations
        if (cls instanceof MInterface) {
            Collection deps = cls.getSupplierDependencies();
            Iterator depIterator = deps.iterator();
            while (depIterator.hasNext()) {
                MDependency dep = (MDependency) depIterator.next();
                if ((dep instanceof MAbstraction)) {
                    MClassifier mc = (MClassifier) dep.getClients().toArray()[0];
                    ems.add(generateQualified(mc));
                }
            }
        } else {
            Collection specs = cls.getSpecializations();
            Iterator specIterator = specs.iterator();
            while (specIterator.hasNext()) {
                MClassifier mc = (MClassifier) (((MGeneralization) specIterator.next()).getChild());
                ems.add(generateQualified(mc));
            }
        }

        if (ems != null && ems.size() > 0) {
            Iterator iter = ems.iterator();
            while (iter.hasNext()) {
                sb.append(INDENT + INDENT + "<extent-class class-ref=\"")
                    .append((String) iter.next())
                    .append("\" />\n");
            }
        }
        return sb;
    }

    private StringBuffer generateClassifierBody(MClassifier cls) {
        StringBuffer sb = new StringBuffer();
        
        sb.append(INDENT + INDENT + "<field-descriptor name=\"id\"")
            .append(" column=\"ID\"")
            .append(" jdbc-type=\"INTEGER\"")
            .append(" primarykey=\"true\"")
            .append(" autoincrement=\"true\" />\n");
        
        Iterator parents = cls.getGeneralizations().iterator();
        if (parents.hasNext() || cls.getSpecializations().size() > 0) {
            sb.append(INDENT + INDENT + "<field-descriptor")
                .append(" name=\"ojbConcreteClass\"")
                .append(" column=\"CLASS\"")
                .append(" jdbc-type=\"VARCHAR\" />\n");
            if (parents.hasNext()) {
                MClassifier parent = (MClassifier) ((MGeneralization) parents.next()).getParent();
                doAttributes(getAttributes(parent), sb);
                doAssociations(parent.getAssociationEnds(), sb);
            }
        }

        doAttributes(getAttributes(cls), sb);
        doAssociations(cls.getAssociationEnds(), sb);
        return sb;
    }

    void doAttributes(Collection c, StringBuffer sb) {
        if (!c.isEmpty()) {
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                sb.append(generate((MStructuralFeature) iter.next()));
            }
        }
    }
    
    void doAssociations(Collection c, StringBuffer sb) {
        if (!c.isEmpty()) {
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                MAssociationEnd ae = (MAssociationEnd) iter.next();
                sb.append(generateAssociationEnd(ae, ae.getOppositeEnd()));
            }
        }
    }

    private StringBuffer generateClassifierEnd(MClassifier cls) {
        fields.clear();
        StringBuffer sb = new StringBuffer();
        sb.append("" + references + collections)
            .append(INDENT + "</class-descriptor>\n\n");
        return sb;
    }

    private String generateAssociationEnd(MAssociationEnd ae1, MAssociationEnd ae2) {
        //if (!(ae1.isNavigable() && ae2.isNavigable())) 
        if (!ae2.isNavigable()) {
            return "";
        }
        StringBuffer sb = new StringBuffer();

        MMultiplicity m1 = ae1.getMultiplicity();
        String endName1 = ae1.getName();

        String name1 = "";

        if (endName1 != null && endName1.length() > 0) {
            name1 = endName1;
        } else {
            name1 = generateClassifierRef(ae1.getType());
        }

        MMultiplicity m2 = ae2.getMultiplicity();
        String endName2 = ae2.getName();

        String name2 = "";

        if (endName2 != null && endName2.length() > 0) {
            name2 = endName2;
        } else {
            name2 = generateClassifierRef(ae2.getType());
        }

        // If one or zero of the other class
        if ((MMultiplicity.M1_1.equals(m2) || MMultiplicity.M0_1.equals(m2)) && ae2.isNavigable()) {
            if (!fields.contains(name2)) {
                fields.add(name2);
                sb.append(INDENT + INDENT + "<field-descriptor name=\"")
                    .append(generateNoncapitalName(name2))
                    .append("Id\" column=\"")
                    .append(generateNoncapitalName(name2))
                    .append("Id\" jdbc-type=\"INTEGER\" />\n");
                
                references.append(INDENT + INDENT + "<reference-descriptor name=\"")
                    .append(generateNoncapitalName(name2))
                    .append("\" class-ref=\"" + generateQualified(ae2.getType()) + "\"")
                    .append(" auto-delete=\"true\"")
                    .append(">\n" + INDENT + INDENT + INDENT + "<foreignkey field-ref=\"")
                    .append(generateNoncapitalName(name2) + "Id")
                    .append("\" />\n" + INDENT + INDENT + "</reference-descriptor>\n");
            }
        } else if ((MMultiplicity.M1_N.equals(m2) || MMultiplicity.M0_N.equals(m2)) 
                   && (MMultiplicity.M1_1.equals(m1) || MMultiplicity.M0_1.equals(m1))) {
            // If more than one of the other class AND one or zero of this one
            collections.append(INDENT + INDENT + "<collection-descriptor name=\"")
                .append(generateNoncapitalName(name2))
                .append("s\" element-class-ref=\"" + generateQualified(ae2.getType()) + "\"")
                .append(" auto-delete=\"true\"")
                .append(">\n" + INDENT + INDENT + INDENT + "<inverse-foreignkey field-ref=\"")
                .append(generateNoncapitalName(name1) + "Id")
                .append("\"/>\n")
                .append(INDENT + INDENT + "</collection-descriptor>\n");
        } else {
            // Else there must be many:many relationship
            String joiningTableName = "";
            if (name1.compareTo(name2) < 0) {
                joiningTableName = generateCapitalName(name1) + generateCapitalName(name2);
            } else {
                joiningTableName = generateCapitalName(name2) + generateCapitalName(name1);
            }

            collections.append(INDENT + INDENT + "<collection-descriptor name=\"")
                .append(generateNoncapitalName(name2))
                .append("s\" element-class-ref=\"" + generateQualified(ae2.getType()) + "\"")
                .append(" auto-delete=\"true\"")
                .append(" indirection-table=\"")
                .append(joiningTableName)
                .append("\">\n")
                .append(INDENT + INDENT + INDENT + "<fk-pointing-to-this-class column=\"")
                // Name of this class's primary key in linkage table
                .append(generateNoncapitalName(name1))
                .append("Id\"/>\n")
                .append(INDENT + INDENT + INDENT + "<fk-pointing-to-element-class column=\"")
                // Name of related class's primary key in linkage table
                .append(generateNoncapitalName(generateCapitalName(name2)))
                .append("Id\"/>\n")
                .append(INDENT + INDENT + "</collection-descriptor>\n");
        }
        return sb.toString();
    }

    private String generateOJBSqlType(MClassifier cls) {
        String type = generateClassifierRef(cls);
        if (type.equals("int")) {
            return "INTEGER";
        }
        if (type.equals("String")) {
            return "LONGVARCHAR";
        }
        if (type.equals("boolean")) {
            return "INTEGER\" conversion=\""
                + "org.apache.ojb.broker.accesslayer.conversions.Boolean2IntFieldConversion";
        }
        if (type.equals("float")) {
            return "FLOAT";
        }
        if (type.equals("Date")) {
            return "DATE";
        }
        return type;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage:  OJBModelOutput <project name> <input dir> <output dir>");
            System.exit(1);
        }
        String projectName = args[0];
        String inputDir = args[1];
        String outputDir = args[2];
        
        File xmiFile = new File(inputDir, projectName + "_.xmi");
        InputSource source = new InputSource(xmiFile.toURL().toString());
        File path = new File(outputDir, "repository_" + projectName.toLowerCase() + ".xml");
        new OJBModelOutput(new XMIReader().parse(source)).output(path);
    }
}
