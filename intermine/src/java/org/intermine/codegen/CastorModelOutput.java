package org.flymine.codegen;

import ru.novosoft.uml.xmi.XMIReader;
import ru.novosoft.uml.foundation.core.*;
import ru.novosoft.uml.foundation.data_types.*;
import ru.novosoft.uml.model_management.*;
import ru.novosoft.uml.foundation.extension_mechanisms.*;

import org.xml.sax.InputSource;

import java.io.File;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Generate a Castor mapping file to bind business objects to XML.
 *
 */


public class CastorModelOutput extends ModelOutput {


    private Collection fields = new HashSet();
    private StringBuffer references, collections;
    private Set done = new HashSet();  // keep track of which classes already done
    private boolean doneList = false;

    public CastorModelOutput(MModel mmodel) {
        super(mmodel);
    }

    protected String generateAttribute(MAttribute attr) {
        StringBuffer sb = new StringBuffer();

        String type = generateCastorJavaType(attr.getType());
        sb.append(INDENT + INDENT + "<field name=\"")
            .append(generateName(attr.getName()))
            .append("\" type=\"" + type)
            .append("\" >");

        sb.append("\n" + INDENT).append(INDENT).append(INDENT)
            .append("<bind-xml name=\"")
            .append(attr.getName());

        if (!(type.indexOf(".") > 0) || (type == "java.lang.String")){
            sb.append("\" node=\"attribute\" />\n");
        }
        else {
            sb.append("\" node=\"element\" />\n");
        }
        sb.append(INDENT + INDENT + "</field>\n");

        return sb.toString();
    }

    protected String generateAssociationEnd(MAssociationEnd ae1, MAssociationEnd ae2) {
        //if (!(ae1.isNavigable() && ae2.isNavigable()))
        if (!ae2.isNavigable()) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        String impl = "";

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
            boolean reference;

            // if a one to one relationship we need to use a reference
            if ((MMultiplicity.M1_1.equals(m1) || MMultiplicity.M1_1.equals(m2)) && ae1.isNavigable()) {
                reference = true;
            }
            else {
                reference = true;  // use references in all cases to simplify XML
            }

            if (!fields.contains(name2)) {
                fields.add(name2);
                references.append(INDENT + INDENT + "<field name=\"")
                    .append(generateNoncapitalName(name2))
                    .append("\" type=\"")
                    .append(generateQualified(ae2.getType()))
                    .append("\">\n");

                // allow castor to use default behaviour: primitives = attributes, objects = elements
                references.append(INDENT).append(INDENT).append(INDENT)
                    .append("<bind-xml name=\"")
                    .append(generateNoncapitalName(name2));
                if (reference) {
                    references.append("\" reference=\"true\" node=\"attribute\" />\n");
                } else {
                    references.append("\" />\n");
                }
                references.append(INDENT + INDENT + "</field>\n");
            }
        } else if ((MMultiplicity.M1_N.equals(m2) || MMultiplicity.M0_N.equals(m2)) ) {
                   //&& (MMultiplicity.M1_1.equals(m1) || MMultiplicity.M0_1.equals(m1))) {

            impl = "collection";

            collections.append(INDENT + INDENT + "<field")
                .append(" name=\"")
                .append(generateNoncapitalName(name2))
                .append("s\"")
                .append(" type=\"")
                .append(getPackagePath(ae2.getType()))
                .append(".")
                .append(generateClassifierRef(ae2.getType()))

                .append("\" collection=\"" + impl + "\">\n");

            // xml-bind this association
            collections.append(INDENT).append(INDENT).append(INDENT)
                .append("<bind-xml name=\"")
                .append(generateNoncapitalName(name2))
                .append("\" reference=\"true\"")
                .append(" node=\"attribute\" />\n")
                .append(INDENT + INDENT + "</field>\n");

        } /*else {
            // Else there must be many:many relationship
                        collections.append(INDENT + INDENT + "<field")
                .append(" name=\"")
                .append(generateNoncapitalName(name2))
                .append("s\"")
                .append(" type=\"")
                .append(getPackagePath(ae2.getType()))
                .append(".")
                .append(generateClassifierRef(ae2.getType()))

                .append("\" collection=\"" + impl + "\">\n");

            // xml-bind this association
            collections.append(INDENT).append(INDENT)
                .append("<bind-xml name=\"")
                .append(generateNoncapitalName(name2))
                //.append("\" reference=\"true\"")
                .append(" node=\"element\" />\n")
                .append(INDENT).append("</field>\n");



                }*/

        return sb.toString();
    }


    protected String generateClassifier(MClassifier cls) {
        StringBuffer sb = new StringBuffer();
        if (!(cls.isAbstract() || cls instanceof MInterface)
            && !(done.contains(cls))) {
            // castor mapping does not support forward references, hence any super-classes
            // must be defined before their sub-classes.  Follow parents and check in 'done'
            // set to see if we need to recurse.
            MClassifier parent = null;
            if (cls.getGeneralizations().size() > 0) {
                List parents = getParents(cls);
                parent = (MClassifier) parents.get(0);
                if (!done.contains(parent)) {
                    sb.append(generateClassifier(parent));
                }
            }
            sb.append(generateClassifierStart(cls))
                .append(generateClassifierBody(cls))
                .append(generateClassifierEnd(cls));
            done.add(cls);
        }
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

    private String generateHeader() {
        StringBuffer sb = new StringBuffer();
        sb .append("<!DOCTYPE databases PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\"")
            .append(" \"http://castor.exolab.org/mapping.dtd\">")
            .append("\n<mapping>\n");

        // include whatever mappings are in general FLyMine header file
        sb.append("\n" + INDENT + "<include href=\"castor_xml_include.xml\" />\n\n");

        return sb.toString();
    }

    private String generateFooter () {
        StringBuffer sb = new StringBuffer();
        sb.append("\n</mapping>\n");
        return sb.toString();
    }

    private StringBuffer generateClassifierStart (MClassifier cls) {
        references = new StringBuffer();
        collections = new StringBuffer();

        MClassifier parent = null;
        if (cls.getGeneralizations().size() > 0) {
            List parents = getParents(cls);
            parent = (MClassifier) parents.get(0);
        }

        StringBuffer sb = new StringBuffer ();
        sb.append(INDENT + "<class name=\"")
            .append(generateQualified(cls) + "\"")
            .append(parent == null ? "" : " extends=\"" + generateQualified(parent) + "\"")
            .append(" auto-complete=\"true\" identity=\"id\" ")
            .append(">\n");

        // bind id field to _xml-id for internal references - assume that if
        // this is a subclass the id has already been defined
        if (cls.getGeneralizations().size() == 0) {
            sb.append(INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\" >\n")
            .append(INDENT + INDENT + INDENT)
            .append("<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>\n")
            .append(INDENT + INDENT + "</field>\n");
        }

        return sb;
    }

    private StringBuffer generateClassifierBody(MClassifier cls) {
        StringBuffer sb = new StringBuffer();

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
            .append(INDENT + "</class>\n\n");
        return sb;
    }


    protected String generateCastorJavaType(MClassifier cls)
    {
        String type = generateClassifierRef(cls);
        if (type.equals("int")) {
            return "integer";
        }
        if (type.equals("String")) {
            return "java.lang.String";
        }
        if (type.equals("Date")) {
            return "java.util.Date";
        }

        return type;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: CastorModelOutput <project name> <input dir> <output dir>");
            System.exit(1);
        }
        String projectName = args[0];
        String inputDir = args[1];
        String outputDir = args[2];

        File xmiFile = new File(inputDir, projectName + "_.xmi");
        InputSource source = new InputSource(xmiFile.toURL().toString());
        File path = new File(outputDir, "castor_xml_" + projectName.toLowerCase() + ".xml");
        new CastorModelOutput(new XMIReader().parse(source)).output(path);
    }

}
