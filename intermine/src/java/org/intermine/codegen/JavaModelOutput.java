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
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collection;
import org.xml.sax.InputSource;

public class JavaModelOutput extends ModelOutput
{
    private static final boolean OJB = true;

    public JavaModelOutput(MModel mmodel) {
        super(mmodel);
    }

    protected String generateOperation(MOperation op) {
        StringBuffer sb = new StringBuffer();
        String nameStr = generateName(op.getName());
        String clsName = generateName(op.getOwner().getName());

        // modifiers
        sb.append(INDENT);
        sb.append(generateConcurrency(op));
        sb.append(generateAbstractness(op));
        sb.append(generateChangeability(op));
        sb.append(generateScope(op));
        sb.append(generateVisibility(op));

        // return type
        MParameter rp = getReturnParameter(op);
        if (rp != null) {
            MClassifier returnType = rp.getType();
            if (returnType == null && !nameStr.equals(clsName)) {
                sb.append("void ");
            } else if (returnType != null) {
                sb.append(generateClassifierRef(returnType)).append(' ');
            }
        }

        // method name
        sb.append(nameStr);

        // parameters
        List params = new ArrayList(op.getParameters());
        params.remove(rp);
        sb.append('(');
        if (params != null) {
            for (int i = 0; i < params.size(); i++) {
                MParameter p = (MParameter) params.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(generateParameter(p));
            }
        }
        sb.append(')');

        return sb.toString();
    }

    protected String generateAttribute (MAttribute attr) {
        StringBuffer sb = new StringBuffer();

        MClassifier owner = attr.getOwner();

        // modifiers
        sb.append(INDENT);
        sb.append(generateVisibility(attr));
        sb.append(generateScope(attr));
        sb.append(generateChangability(attr));

        // type
        MClassifier type = attr.getType();
        if (type != null) {
            sb.append(generateClassifierRef(type)).append(' ');
        }

        // field name
        sb.append(generateName(attr.getName()));

        // initial value
        MExpression init = attr.getInitialValue();
        if (init != null) {
            String initStr = generateExpression(init).trim();
            if (initStr.length() > 0) {
                sb.append(" = ").append(initStr);
            }
        }

        sb.append(";\n")            
            .append(generateGetSet(generateNoncapitalName(attr.getName()), 
                                   generateClassifierRef(type)))
            .append("\n");

        return sb.toString();
    }

    protected String generateParameter(MParameter param) {
        StringBuffer sb = new StringBuffer();
        // type
        sb.append(generateClassifierRef(param.getType())).append(' ');
        // name
        sb.append(generateName(param.getName()));
        return sb.toString();
    }

    protected String generateClassifier(MClassifier cls) {
        StringBuffer sb = new StringBuffer();
        sb.append(generateClassifierStart(cls))
            .append(generateClassifierBody(cls))
            .append(generateClassifierEnd(cls));
        return sb.toString();
    }

    protected String generateAssociationEnd(MAssociationEnd ae) {
        if (!ae.isNavigable()) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        String type = "";
        String impl = "";
        String name = "";
        String construct = "";

        //sb.append(INDENT + generateVisibility(ae.getVisibility()));

        if (MScopeKind.CLASSIFIER.equals(ae.getTargetScope())) {
            sb.append("static ");
        }

        MMultiplicity m = ae.getMultiplicity();
        if (MMultiplicity.M1_1.equals(m) || MMultiplicity.M0_1.equals(m)) {
            type = generateClassifierRef(ae.getType());
        } else {
            // if(ae.getOrdering()==null || ae.getOrdering().getName().equals("unordered")) {
            // type="Set";
            // impl="HashSet";
            // } else {
            type = "List";
            impl = "ArrayList";
            //             }
            name = "s";
            construct = " = new " + impl + "()";
        }

        String n = ae.getName();
        MAssociation asc = ae.getAssociation();
        String ascName = asc.getName();

        if (n != null && n.length() > 0) {
            name = n + name;
        } else if (ascName != null && ascName.length() > 0) {
            name = ascName + name;
        } else {
            name = generateClassifierRef(ae.getType()) + name;
        }

        if (OJB && (MMultiplicity.M1_1.equals(m) || MMultiplicity.M0_1.equals(m))) {
            sb.append(INDENT + "protected int ")
                .append(generateNoncapitalName(name) + "Id;\n");
        }

        sb.append(INDENT + "protected ")
            .append(type)
            .append(' ')
            .append(generateNoncapitalName(name))
            .append(construct)
            .append(";\n")
            .append(generateGetSet(generateNoncapitalName(name), type))
            .append("\n");

        return sb.toString();
    }

    protected void generateFileStart(File path) {
    }

    protected void generateFile(MClassifier cls, File path) {
         String filename = cls.getName() + ".java";
        
         int lastIndex = -1;
         do {
             if (!path.isDirectory()) {
                 if (!path.mkdir()) {
                     LOG.debug(" could not make directory " + path);
                     return;
                 }
             }
             
             String packagePath = getPackagePath(cls);

             if (lastIndex == packagePath.length()) {
                 break;
             }

             int index = packagePath.indexOf (".", lastIndex + 1);
             if (index == -1) {
                 index = packagePath.length();
             }

             path = new File(path, packagePath.substring (lastIndex + 1, index));
             lastIndex = index;
         } while (true);

         path = new File(path, filename);
         initFile(path);

         String header = generateHeader(cls);
         String src = generate(cls);

         outputToFile(path, header + src);
    }

    protected void generateFileEnd(File path) {
    }

    //=================================================================

    private String generateHeader (MClassifier cls) {
        StringBuffer sb = new StringBuffer();
        String packagePath = getPackagePath(cls);
        if (packagePath.length() > 0) {
            sb.append("package ").append(packagePath).append(";\n\n");
        }
        sb.append("import java.util.*;\n\n");
        return sb.toString();
    }

    private StringBuffer generateClassifierStart (MClassifier cls) {
        StringBuffer sb = new StringBuffer ();

        String sClassifierKeyword = null;
        if (cls instanceof MClassImpl) {
            sClassifierKeyword = "class";
        } else if (cls instanceof MInterface) {
            sClassifierKeyword = "interface";
        }

        // Now add visibility
        sb.append(generateVisibility(cls.getVisibility()));

        // Add other modifiers
        if (cls.isAbstract() && !(cls instanceof MInterface)) {
            sb.append("abstract ");
        }

        if (cls.isLeaf()) {
            sb.append("final ");
        }

        // add classifier keyword and classifier name
        sb.append (sClassifierKeyword)
            .append(" ")
            .append (generateName(cls.getName()));

        // add base class/interface
        String baseClass = generateGeneralization(cls.getGeneralizations());
        if (!baseClass.equals("")) {
            sb.append (" ")
                .append ("extends ")
                .append (baseClass);
        }

        // add implemented interfaces, if needed
        if (cls instanceof MClass) {
            String interfaces = generateSpecification((MClass) cls);
            if (!interfaces.equals ("")) {
                sb.append (" ")
                    .append ("implements ")
                    .append (interfaces);
            }
        }

        // add opening brace
        sb.append("\n{\n");

        if (OJB && sClassifierKeyword.equals("class")) {
            if (baseClass.equals("")) {
                sb.append(INDENT + "protected int id;\n");
            }
            if (!baseClass.equals("") || cls.getSpecializations().size() > 0) {
                sb.append(INDENT + "protected String ojbConcreteClass = \"" + getPackagePath(cls) 
                          + "." + cls.getName() + "\";\n");
            }
        }

        return sb.append("\n");
    }

    private String generateGetSet(String name, String type) {
        StringBuffer sb = new StringBuffer();

        // Get method
        sb.append(INDENT)
            .append("public ");

        if (type != null) {
            sb.append(type).append(' ');
        }
        sb.append("get").append(generateCapitalName(name)).append("() { ")
            .append("return this.").append(generateName(name)).append("; }\n");

        // Set method
        sb.append(INDENT)
            .append("public void ")
            .append("set").append(generateCapitalName(name)).append("(");
        if (type != null) {
            sb.append(type).append(" ");
        }
        sb.append(generateName(name)).append(") { ")
            .append("this.").append(generateName(name)).append("=")
            .append(generateName(name)).append("; }\n");

        return sb.toString();
    }

//     private String generateHashCode(MClassifier cls) {
//         StringBuffer sb = new StringBuffer();

//         Collection keyFields = getKeys(cls);
//         if (keyFields.size() > 0) {
//             sb.append(INDENT + "public int hashCode() { ")
//                 .append("return ");
//             Iterator iter = keyFields.iterator();
//             while (iter.hasNext()) {
//                 String field = (String) iter.next();
//                 sb.append(field+".hashCode()");
//                 if (iter.hasNext()) {
//                     sb.append(" ^ ");
//                 }
//             }
//             sb.append("; }\n");
//         }
//         return sb.toString();
//     }

//     private String generateEquals(MClassifier cls) {
//         StringBuffer sb = new StringBuffer();

//         Collection keyFields = getKeys(cls);
//         if (keyFields.size() > 0) {
//             sb.append(INDENT + "public boolean equals(Object o) { ")
//                 .append("return o != null && o instanceof " + cls.getName())
//                 .append(" && o.hashCode() == hashCode(); }\n");
//         }
//         return sb.toString();
//     }

    private String generateEquals(MClassifier cls) {
        StringBuffer sb = new StringBuffer();

        Collection keyFields = getKeys(cls);
        if (keyFields.size() > 0) {
            sb.append(INDENT + "public boolean equals(Object o) { ")
                .append("return (o instanceof " + cls.getName() + " && ");
            Iterator iter = keyFields.iterator();
            while (iter.hasNext()) {
                String field = (String) iter.next();
                sb.append("((" + cls.getName() + ")o)." + field + "==" + field);
                if (iter.hasNext()) {
                    sb.append(" && ");
                }
            }
            sb.append("); }\n");
        }
        return sb.toString();
    }

    private String generateToString(MClassifier cls) {
        StringBuffer sb = new StringBuffer();

        Collection keyFields = getKeys(cls);
        if (keyFields.size() > 0) {
            sb.append(INDENT + "public String toString() {")
                .append("return \"" + cls.getName() + " [\"")
                .append(OJB ? "+id" : "+get" + generateCapitalName(cls.getName()) + "Id()")
                .append("+\"] \"+");
            Iterator iter = keyFields.iterator();
            while (iter.hasNext()) {
                String field = (String) iter.next();
                sb.append(field);
                if (iter.hasNext()) {
                    sb.append("+\", \"+");
                }
            }
            sb.append("; }\n");
        }
        return sb.toString();
    }

    private StringBuffer generateClassifierEnd(MClassifier cls) {
        StringBuffer sb = new StringBuffer();        
        sb.append(generateEquals(cls))
            .append("\n")
            .append(generateToString(cls))
            .append("}");
        return sb;
    }

    private StringBuffer generateClassifierBody(MClassifier cls) {
        StringBuffer sb = new StringBuffer();

        // (attribute) fields
        Collection strs = getAttributes(cls);
        if (!strs.isEmpty()) {
            Iterator strIter = strs.iterator();
            while (strIter.hasNext()) {
                sb.append(generate((MStructuralFeature) strIter.next()));
            }
        }
        
        // (association) fields
        Collection ends = new ArrayList(cls.getAssociationEnds());
        if (!ends.isEmpty()) {
            Iterator endIter = ends.iterator();
            while (endIter.hasNext()) {
                MAssociationEnd ae = (MAssociationEnd) endIter.next();
                sb.append(generateAssociationEnd(ae.getOppositeEnd()));
            }
        }

        // methods
        Collection behs = getOperations(cls);
        if (!behs.isEmpty()) {
            sb.append('\n');
            
            Iterator behEnum = behs.iterator();
            
            while (behEnum.hasNext()) {
                MBehavioralFeature bf = (MBehavioralFeature) behEnum.next();
                
                sb.append(generate(bf));
                
                
                if ((cls instanceof MClassImpl)
                    && (bf instanceof MOperation)
                    && (!((MOperation) bf).isAbstract())) {

                    sb.append(" {\n")
                        .append(generateMethodBody((MOperation) bf))
                        .append(INDENT)
                        .append("}\n");
                } else {
                    sb.append(";\n");
                }
            }
        }
        return sb;
    }

    private String generateMethodBody (MOperation op) {
        if (op != null) {
            Collection methods = op.getMethods();
            Iterator i = methods.iterator();
            MMethod m = null;

            while (i != null && i.hasNext()) {
                m = (MMethod) i.next();

                if (m != null) {
                    if (m.getBody() != null) {
                        return m.getBody().getBody();
                    } else {
                        return "";
                    }
                }
            }

            // pick out return type
            MParameter rp = getReturnParameter(op);
            if (rp != null) {
                MClassifier returnType = rp.getType();
                return generateDefaultReturnStatement (returnType);
            }
        }

        return generateDefaultReturnStatement (null);
    }

    private String generateDefaultReturnStatement(MClassifier cls) {
        if (cls == null) {
            return "";
        }

        String clsName = cls.getName();
        if (clsName.equals("void")) {
            return "";
        }
        if (clsName.equals("char")) {
            return INDENT + "return 'x';\n";
        }
        if (clsName.equals("int")) {
            return INDENT + "return 0;\n";
        }
        if (clsName.equals("boolean")) {
            return INDENT + "return false;\n";
        }
        if (clsName.equals("byte")) {
            return INDENT + "return 0;\n";
        }
        if (clsName.equals("long")) {
            return INDENT + "return 0;\n";
        }
        if (clsName.equals("float")) {
            return INDENT + "return 0.0;\n";
        }
        if (clsName.equals("double")) {
            return INDENT + "return 0.0;\n";
        }
        return INDENT + "return null;\n";
    }

    private String generateSpecification(MClass cls) {
        Collection realizations = getSpecifications(cls);
        if (realizations == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        Iterator clsEnum = realizations.iterator();
        while (clsEnum.hasNext()) {
            MInterface i = (MInterface) clsEnum.next();
            sb.append(generateClassifierRef(i));
            if (clsEnum.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String generateVisibility(MVisibilityKind vis) {
        if (MVisibilityKind.PUBLIC.equals(vis)) {
            return "public ";
        }
        if (MVisibilityKind.PRIVATE.equals(vis)) {
            return "private ";
        }
        if (MVisibilityKind.PROTECTED.equals(vis)) {
            return "protected ";
        }
        return "";
    }

    private String generateVisibility(MFeature f) {
        return generateVisibility(f.getVisibility());
    }

    private String generateScope(MFeature f) {
        MScopeKind scope = f.getOwnerScope();
        if (MScopeKind.CLASSIFIER.equals(scope)) {
            return "static ";
        }
        return "";
    }

    private String generateAbstractness(MOperation op) {
        if (op.isAbstract()) {
            return "abstract ";
        }
        return "";
    }

    private String generateChangeability(MOperation op) {
        if (op.isLeaf()) {
            return "final ";
        }
        return "";
    }

    private String generateChangability(MStructuralFeature sf) {
        MChangeableKind ck = sf.getChangeability();
        //if (ck == null) return "";
        if (MChangeableKind.FROZEN.equals(ck)) {
            return "final ";
        }
        //if (MChangeableKind.ADDONLY.equals(ck)) return "final ";
        return "";
    }

    private String generateConcurrency(MOperation op) {
        if (op.getConcurrency() != null
            && op.getConcurrency().getValue() == MCallConcurrencyKind._GUARDED) {
            return "synchronized ";
        }
        return "";
    }

    private Collection getKeys(MClassifier cls) {
        Set keyFields = new LinkedHashSet();
        Collection tvs = cls.getTaggedValues();
        if (tvs != null && tvs.size() > 0) {
            Iterator iter = tvs.iterator();
            while (iter.hasNext()) {
                MTaggedValue tv = (MTaggedValue) iter.next();
                if (tv.getTag().equals("key")) {
                    StringTokenizer st = new StringTokenizer(tv.getValue(), ", ");
                    while (st.hasMoreElements()) {
                        keyFields.add(st.nextElement());
                    }
                }
            }
        }
        Iterator parents = cls.getGeneralizations().iterator();
        if (parents.hasNext()) {
            keyFields.addAll(getKeys((MClassifier) ((MGeneralization) parents.next()).getParent()));
        }
        return keyFields;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage:  JavaModelOutput <project name> <input dir> <output dir>");
            System.exit(1);
        }
        String projectName = args[0];
        String inputDir = args[1];
        String outputDir = args[2];
        
        File xmiFile = new File(inputDir, projectName + "_.xmi");
        InputSource source = new InputSource(xmiFile.toURL().toString());
        File path = new File(outputDir);
        new JavaModelOutput(new XMIReader().parse(source)).output(path);
    }
}
