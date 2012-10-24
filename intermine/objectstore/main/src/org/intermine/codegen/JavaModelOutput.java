package org.intermine.codegen;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.intermine.objectstore.intermine.NotXmlParser.DELIM;
import static org.intermine.objectstore.intermine.NotXmlParser.ENCODED_DELIM;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;

/**
 * Maps InterMine metadata to Java source files
 *
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public class JavaModelOutput
{
    protected static final String INDENT = "    ";
    protected static final String ENDL = System.getProperty("line.separator");

    protected Model model;
    protected File file; //note: this is a directory

    /**
     * Constructor.
     *
     * @param model a Model
     * @param file a File
     * @throws Exception if something goes wrong
     */
    public JavaModelOutput(Model model, File file) throws Exception {
        this.model = model;
        this.file = file;
    }

    /**
     * Perform the mapping.
     */
    public void process() {
        for (ClassDescriptor cld : model.getClassDescriptors()) {
            String cldName = cld.getName();
            if (!"org.intermine.model.InterMineObject".equals(cldName)) {
                String pkg = TypeUtil.packageName(cldName);
                String cls = TypeUtil.unqualifiedName(cld.getName());
                String separator = File.separator;
                // Escape windows path seperator
                if ("\\".equals(separator)) {
                    separator = "\\\\";
                }
                File dir = new File(file, pkg.replaceAll("[.]", separator));
                dir.mkdirs();
                File path = new File(dir, cls + ".java");
                try {
                    path.delete();
                    BufferedWriter fos = new BufferedWriter(new FileWriter(path, true));
                    fos.write(generate(cld, false));
                    fos.close();
                    if (cld.isInterface()) {
                        path = new File(dir, cls + "Shadow.java");
                        path.delete();
                        fos = new BufferedWriter(new FileWriter(path, true));
                        fos.write(generate(cld, true));
                        fos.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error creating java", e);
                }
            }
        }
    }

    /**
     * Generate the output for a ClassDescriptor.
     *
     * @param cld the ClassDescriptor
     * @param shadow whether to generate the shadow class of an interface
     * @return the relevant String representation
     */
    protected String generate(ClassDescriptor cld, boolean shadow) {
        StringBuffer sb = new StringBuffer();

        String packageName = TypeUtil.packageName(cld.getName());

        if (packageName.length() > 0) {
            sb.append("package ")
                .append(packageName)
                .append(";" + ENDL + ENDL);
        }
        if ((!cld.isInterface()) || shadow) {
            boolean hasCollections = false;
            boolean hasReferences = false;
            for (FieldDescriptor fd : cld.getAllFieldDescriptors()) {
                if (fd instanceof CollectionDescriptor) {
                    hasCollections = true;
                } else if (fd instanceof ReferenceDescriptor) {
                    hasReferences = true;
                }
            }
            sb.append("import org.intermine.objectstore.ObjectStore;" + ENDL);
            sb.append("import org.intermine.objectstore.intermine.NotXmlParser;" + ENDL);
            sb.append("import org.intermine.objectstore.intermine.NotXmlRenderer;" + ENDL);
            if (hasCollections) {
                sb.append("import org.intermine.objectstore.proxy.ProxyCollection;" + ENDL);
            }
            if (hasReferences) {
                sb.append("import org.intermine.objectstore.proxy.ProxyReference;" + ENDL);
            }
            sb.append("import org.intermine.util.StringConstructor;" + ENDL);
            sb.append("import org.intermine.util.TypeUtil;" + ENDL);
            if (shadow) {
                sb.append("import org.intermine.model.ShadowClass;" + ENDL);
            }
            sb.append(ENDL);
        }
        sb.append("public ")
            .append((cld.isInterface() && (!shadow)) ? "interface " : "class ")
            .append(TypeUtil.unqualifiedName(cld.getName()))
            .append(shadow ? "Shadow" : "");

        if (shadow) {
            sb.append(" implements ")
                .append(TypeUtil.unqualifiedName(cld.getName()))
                .append(", ShadowClass");
        } else {
            if (!cld.isInterface()) {
                if (cld.getSuperclassDescriptor() != null) {
                    sb.append(" extends ")
                        .append(cld.getSuperclassDescriptor().getName());
                }
            }

            boolean firstTime = true;

            if (cld.getSuperDescriptors().size() > 0) {
                for (ClassDescriptor superCld : cld.getSuperDescriptors()) {
                    if (superCld.isInterface()) {
                        if (firstTime) {
                            sb.append(cld.isInterface() ? " extends " : " implements ");
                            firstTime = false;
                        } else {
                            sb.append(", ");
                        }
                        sb.append(superCld.getName());
                    }
                }
            } else {
                sb.append(" implements org.intermine.model.FastPathObject");
            }
        }

        sb.append(ENDL)
            .append("{" + ENDL);

        if (shadow) {
            sb.append(INDENT)
                .append("public static final Class<")
                .append(TypeUtil.unqualifiedName(cld.getName()))
                .append("> shadowOf = ")
                .append(TypeUtil.unqualifiedName(cld.getName()))
                .append(".class;" + ENDL);
        }

        // FieldDescriptors defined for this class/interface
        if (cld.isInterface() && (!shadow)) {
            sb.append(generateFieldDescriptors(cld, false));
        } else {
            sb.append(generateFieldDescriptors(cld, true))
                .append(generateEquals(cld))
                .append(generateHashCode(cld))
                .append(generateToString(cld))
                .append(generateGetFieldValue(cld, false))
                .append(generateGetFieldValue(cld, true))
                .append(generateSetFieldValue(cld))
                .append(generateGetFieldType(cld));
            if (cld.getSuperDescriptors().size() > 0) {
                sb.append(generateGetObject(cld))
                    .append(generateSetObject(cld))
                    .append(generateAddCollectionElement(cld))
                    .append(generateGetElementType(cld));
            }
        }

        sb.append("}" + ENDL);
        return sb.toString();
    }

    /**
     * Generate all FieldDescriptors for a class/interface
     *
     * @param cld the ClassDescriptor of the class
     * @param supers true if go up the inheritence tree and output fields
     * @return the generated String
     */
    protected String generateFieldDescriptors(ClassDescriptor cld, boolean supers) {
        Set<FieldDescriptor> superclassFields = Collections.emptySet();
        if (supers && (cld.getSuperclassDescriptor() != null)) {
            superclassFields = cld.getSuperclassDescriptor().getAllFieldDescriptors();
        }
        StringBuffer sb = new StringBuffer();
        Iterator<FieldDescriptor> iter;
        if (supers) {
            iter = cld.getAllFieldDescriptors().iterator();
        } else {
            iter = cld.getFieldDescriptors().iterator();
        }
        while (iter.hasNext()) {
            FieldDescriptor fd = iter.next();
            if (!superclassFields.contains(fd)) {
                if (fd instanceof AttributeDescriptor) {
                    sb.append(generate((AttributeDescriptor) fd, supers));
                } else if (fd instanceof CollectionDescriptor) {
                    sb.append(generate((CollectionDescriptor) fd, supers));
                } else if (fd instanceof ReferenceDescriptor) {
                    sb.append(generate((ReferenceDescriptor) fd, supers));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Generates code for a single attribute.
     *
     * @param attr the AttributeDescriptor
     * @param field true if the class should have the associated field, or false if the field is in
     * the superclass
     * @return java code
     */
    protected String generate(AttributeDescriptor attr, boolean field) {
        StringBuffer sb = new StringBuffer();
        if (field) {
            sb.append(INDENT + "// Attr: " + attr.getClassDescriptor().getName() + "."
                    + attr.getName() + ENDL)
                .append(INDENT + "protected ")
                .append(attr.getType())
                .append(" ")
                .append(attr.getName())
                .append(";" + ENDL);
        }
        sb.append(generateGetSet(attr, field))
            .append(ENDL);
        return sb.toString();
    }

    /**
     * Generates code for a single reference.
     *
     * @param ref the ReferenceDescriptor
     * @param field true if the class should have the associated field, or false if the field is in
     * the superclass
     * @return java code
     */
    protected String generate(ReferenceDescriptor ref, boolean field) {
        StringBuffer sb = new StringBuffer();
        if (field) {
            sb.append(INDENT + "// Ref: " + ref.getClassDescriptor().getName() + "."
                    + ref.getName() + ENDL)
                .append(INDENT)
                .append("protected org.intermine.model.InterMineObject ")
                .append(ref.getName())
                .append(";" + ENDL);
        }
        sb.append(generateGetSet(ref, field))
            .append(ENDL);
        return sb.toString();
    }

    /**
     * Generates code for a single collection.
     *
     * @param col the CollectionDescriptor
     * @param field true if the class should have the associated field, or false if the field is in
     * the superclass
     * @return java code
     */
    protected String generate(CollectionDescriptor col, boolean field) {
        String type = "java.util.Set<" + col.getReferencedClassName() + ">";
        String impl = "java.util.HashSet<" + col.getReferencedClassName() + ">";

        StringBuffer sb = new StringBuffer();
        if (field) {
            sb.append(INDENT + "// Col: " + col.getClassDescriptor().getName() + "."
                    + col.getName() + ENDL)
                .append(INDENT)
                .append("protected ")
                .append(type)
                .append(" ")
                .append(col.getName())
                .append(" = new ")
                .append(impl)
                .append("();" + ENDL);
        }
        sb.append(generateGetSet(col, field))
            .append(ENDL);
        return sb.toString();
    }

    //=================================================================

    /**
     * Write code for getters and setters for given field.
     * @param field descriptor for field
     * @param fieldPresent true if this class has the associated field
     * @return string with generated java code
     */
    protected String generateGetSet(FieldDescriptor field, boolean fieldPresent) {
        String name = field.getName();
        String type = getType(field);

        StringBuffer sb = new StringBuffer();

        // Get method
        sb.append(INDENT)
            .append("public ")
            .append(type)
            .append(" get")
            .append(StringUtil.reverseCapitalisation(name))
            .append("()");
        if (!fieldPresent) {
            sb.append(";" + ENDL);
        } else {
            sb.append(" { ");
            if ((field instanceof ReferenceDescriptor)
                    && (!(field instanceof CollectionDescriptor))) {
                // This is an object reference.
                sb.append("if (")
                    .append(name)
                    .append(" instanceof org.intermine.objectstore.proxy.ProxyReference) { return ")
                    .append("((")
                    .append(type)
                    .append(") ((org.intermine.objectstore.proxy.ProxyReference) ")
                    .append(name)
                    .append(").getObject()); }; return (")
                    .append(type)
                    .append(") ")
                    .append(name)
                    .append("; }" + ENDL);
            } else {
                sb.append("return ")
                    .append(name)
                    .append("; }" + ENDL);
            }
        }

        // Set method
        sb.append(INDENT)
            .append("public void ")
            .append("set")
            .append(StringUtil.reverseCapitalisation(name))
            .append("(final ")
            .append(type)
            .append(" ")
            .append(name)
            .append(")");
        if (!fieldPresent) {
            sb.append(";" + ENDL);
        } else {
            sb.append(" { ")
                .append("this.")
                .append(name)
                .append(" = ")
                .append(name)
                .append("; }" + ENDL);
        }

        if (field instanceof ReferenceDescriptor) {
            if (field instanceof CollectionDescriptor) {
                sb.append(INDENT)
                    .append("public void add")
                    .append(StringUtil.reverseCapitalisation(name))
                    .append("(final ")
                    .append(((CollectionDescriptor) field).getReferencedClassDescriptor().getName())
                    .append(" arg)");
                if (fieldPresent) {
                    sb.append(" { ")
                        .append(name)
                        .append(".add(arg); }" + ENDL);
                } else {
                    sb.append(";" + ENDL);
                }
            } else {
                // This is an object reference.
                sb.append(INDENT)
                    .append("public void proxy")
                    .append(StringUtil.reverseCapitalisation(name))
                    .append("(final org.intermine.objectstore.proxy.ProxyReference ")
                    .append(name)
                    .append(")");
                if (fieldPresent) {
                    sb.append(" { this.")
                        .append(name)
                        .append(" = ")
                        .append(name)
                        .append("; }" + ENDL);
                } else {
                    sb.append(";" + ENDL);
                }
                sb.append(INDENT)
                    .append("public org.intermine.model.InterMineObject proxGet")
                    .append(StringUtil.reverseCapitalisation(name))
                    .append("()");
                if (fieldPresent) {
                    sb.append(" { return ")
                        .append(name)
                        .append("; }" + ENDL);
                } else {
                    sb.append(";" + ENDL);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Generate a .equals() method for the given class.
     * @param cld descriptor for class in question
     * @return generated java code as string
     */
    protected String generateEquals(ClassDescriptor cld) {
        if (cld.getFieldDescriptorByName("id") != null) {
            String unqualifiedName = TypeUtil.unqualifiedName(cld.getName());

            StringBuffer sb = new StringBuffer();
            sb.append(INDENT)
                .append("@Override public boolean equals(Object o) { return (o instanceof ")
                .append(unqualifiedName)
                .append(" && id != null) ? id.equals(((")
                .append(unqualifiedName)
                .append(")o).getId()) : this == o; }" + ENDL);
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * Generate a .hashCode() method for the given class.
     * @param cld descriptor for the class in question
     * @return generate java code as a string
     */
    protected String generateHashCode(ClassDescriptor cld) {
        if (cld.getFieldDescriptorByName("id") != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(INDENT)
                .append("@Override public int hashCode() { ")
                .append("return (id != null) ? id.hashCode() : super.hashCode(); ")
                .append("}" + ENDL);
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * Generate a .toString() method for the given class .
     * @param cld descriptor for the class in question
     * @return generated java code as a string
     */
    protected String generateToString(ClassDescriptor cld) {
        String unqualifiedName = TypeUtil.unqualifiedName(cld.getName());

        StringBuilder sb = new StringBuilder();
        Set<FieldDescriptor> keyFields = cld.getAllFieldDescriptors();
        if (keyFields.size() > 0) {
            sb.append(INDENT)
                .append("@Override public String toString() { ")
                .append("return \"")
                .append(unqualifiedName)
                .append(" [");
            TreeMap<String, FieldDescriptor> sortedMap = new TreeMap<String, FieldDescriptor>();
            for (FieldDescriptor field : keyFields) {
                sortedMap.put(field.getName(), field);
            }
            boolean needComma = false;
            for (Map.Entry<String, FieldDescriptor> entry : sortedMap.entrySet()) {
                FieldDescriptor field = entry.getValue();
                if (!(field instanceof CollectionDescriptor)) {
                    if (needComma) {
                        sb.append(", ");
                    }
                    needComma = true;
                    sb.append(field.getName());
                    if (field instanceof AttributeDescriptor) {
                        sb.append("=\\\"\" + " + field.getName() + " + \"\\\"");
                    } else {
                        sb.append("=\" + (" + field.getName() + " == null ? \"null\" : ("
                                + field.getName() + ".getId() == null ? \"no id\" : "
                                + field.getName() + ".getId().toString())) + \"");
                    }
                }
            }
            sb.append("]\"; }" + ENDL);
        }
        return sb.toString();
    }

    /**
     * Return the java type of a particular field.
     * @param field descriptor for the field in question
     * @return the java type
     */
    protected String getType(FieldDescriptor field) {
        String type = null;
        if (field instanceof AttributeDescriptor) {
            type = ((AttributeDescriptor) field).getType();
        } else if (field instanceof CollectionDescriptor) {
            type = "java.util.Set<" + ((CollectionDescriptor) field).getReferencedClassName() + ">";
        } else {
            type = ((ReferenceDescriptor) field).getReferencedClassDescriptor().getName();
        }
        return type;
    }

    /**
     * Generates the getoBJECT method for producing NotXml.
     *
     * @param cld the ClassDescriptor
     * @return generated java code as a String
     */
    protected String generateGetObject(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("public StringConstructor getoBJECT() {\n")
            .append(INDENT + INDENT)
            .append("if (!" + cld.getName() + (cld.isInterface() ? "Shadow" : "")
                    + ".class.equals(getClass())) {\n")
            .append(INDENT + INDENT + INDENT)
            .append("return NotXmlRenderer.render(this);\n")
            .append(INDENT + INDENT)
            .append("}\n")
            .append(INDENT + INDENT)
            .append("StringConstructor sb = new StringConstructor();\n")
            .append(INDENT + INDENT)
            .append("sb.append(\"" + DELIM + cld.getName() + "\");\n");
        for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
            if (field instanceof AttributeDescriptor) {
                AttributeDescriptor attribute = (AttributeDescriptor) field;
                if (attribute.getType().startsWith("java.")) {
                    sb.append(INDENT + INDENT)
                        .append("if (" + attribute.getName() + " != null) {\n")
                        .append(INDENT + INDENT + INDENT)
                        .append("sb.append(\"" + DELIM + "a" + field.getName() + DELIM + "\")");
                    if ("java.util.Date".equals(attribute.getType())) {
                        sb.append(".append(" + attribute.getName() + ".getTime());\n");
                    } else if ("java.lang.String".equals(attribute.getType())) {
                        sb.append(";\n")
                            .append(INDENT + INDENT + INDENT)
                            .append("String string = " + attribute.getName() + ";\n")
                            .append(INDENT + INDENT + INDENT)
                            .append("while (string != null) {\n")
                            .append(INDENT + INDENT + INDENT + INDENT)
                            .append("int delimPosition = string.indexOf(\"" + DELIM + "\");\n")
                            .append(INDENT + INDENT + INDENT + INDENT)
                            .append("if (delimPosition == -1) {\n")
                            .append(INDENT + INDENT + INDENT + INDENT + INDENT)
                            .append("sb.append(string);\n")
                            .append(INDENT + INDENT + INDENT + INDENT + INDENT)
                            .append("string = null;\n")
                            .append(INDENT + INDENT + INDENT + INDENT)
                            .append("} else {\n")
                            .append(INDENT + INDENT + INDENT + INDENT + INDENT)
                            .append("sb.append(string.substring(0, delimPosition + 3));\n")
                            .append(INDENT + INDENT + INDENT + INDENT + INDENT)
                            .append("sb.append(\"" + ENCODED_DELIM + "\");\n")
                            .append(INDENT + INDENT + INDENT + INDENT + INDENT)
                            .append("string = string.substring(delimPosition + 3);\n")
                            .append(INDENT + INDENT + INDENT + INDENT)
                            .append("}\n")
                            .append(INDENT + INDENT + INDENT)
                            .append("}\n");
                    } else {
                        sb.append(".append(" + attribute.getName() + ");\n");
                    }
                    sb.append(INDENT + INDENT)
                        .append("}\n");
                } else if ("org.intermine.objectstore.query.ClobAccess".equals(attribute
                        .getType())) {
                    sb.append(INDENT + INDENT)
                        .append("if (" + attribute.getName() + " != null) {\n")
                        .append(INDENT + INDENT + INDENT)
                        .append("sb.append(\"" + DELIM + "a" + field.getName() + DELIM + "\" + ")
                        .append(attribute.getName() + ".getDbDescription());\n")
                        .append(INDENT + INDENT)
                        .append("}\n");
                } else {
                    sb.append(INDENT).append(INDENT)
                        .append("sb.append(\"" + DELIM + "a" + field.getName() + DELIM + "\")")
                        .append(".append(" + field.getName() + ");\n");
                }
            } else if (field.isReference()) {
                sb.append(INDENT + INDENT)
                    .append("if (" + field.getName() + " != null) {\n")
                    .append(INDENT + INDENT + INDENT)
                    .append("sb.append(\"" + DELIM + "r" + field.getName() + DELIM + "\")")
                    .append(".append(" + field.getName() + ".getId());\n")
                    .append(INDENT + INDENT)
                    .append("}\n");
            }
        }
        sb.append(INDENT + INDENT)
            .append("return sb;\n")
            .append(INDENT)
            .append("}\n");
        return sb.toString();
    }

    /**
     * Generates the setoBJECT method for deserialising objects.
     *
     * @param cld a ClassDescriptor
     * @return a String containing the method
     */
    public String generateSetObject(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("public void setoBJECT(String notXml, ObjectStore os) {\n")
            .append(INDENT + INDENT)
            .append("setoBJECT(NotXmlParser.SPLITTER.split(notXml), os);\n")
            .append(INDENT)
            .append("}\n")
            .append(INDENT)
            .append("public void setoBJECT(final String[] notXml, final ObjectStore os) {\n")
            .append(INDENT + INDENT)
            .append("if (!" + cld.getName() + (cld.isInterface() ? "Shadow" : "")
                    + ".class.equals(getClass())) {\n")
            .append(INDENT + INDENT + INDENT)
            .append("throw new IllegalStateException(\"Class \" + getClass().getName() + \""
                    + " does not match code (" + cld.getName() + ")\");\n")
            .append(INDENT + INDENT)
            .append("}\n")
            .append(INDENT + INDENT)
            .append("for (int i = 2; i < notXml.length;) {\n")
            .append(INDENT + INDENT + INDENT)
            .append("int startI = i;\n");
        for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
            String fieldName = field.getName();
            if ("notXml".equals(fieldName)) {
                fieldName = "this.notXml";
            } else if ("os".equals(fieldName)) {
                fieldName = "this.os";
            }
            if (field instanceof AttributeDescriptor) {
                AttributeDescriptor attribute = (AttributeDescriptor) field;
                sb.append(INDENT + INDENT + INDENT)
                    .append("if ((i < notXml.length) && \"a" + fieldName
                            + "\".equals(notXml[i])) {\n")
                    .append(INDENT + INDENT + INDENT + INDENT)
                    .append("i++;\n")
                    .append(INDENT + INDENT + INDENT + INDENT);
                if ("boolean".equals(attribute.getType())) {
                    sb.append(fieldName + " = Boolean.parseBoolean(notXml[i]);\n");
                } else if ("short".equals(attribute.getType())) {
                    sb.append(fieldName + " = Short.parseShort(notXml[i]);\n");
                } else if ("int".equals(attribute.getType())) {
                    sb.append(fieldName + " = Integer.parseInt(notXml[i]);\n");
                } else if ("long".equals(attribute.getType())) {
                    sb.append(fieldName + " = Long.parseLong(notXml[i]);\n");
                } else if ("float".equals(attribute.getType())) {
                    sb.append(fieldName + " = Float.parseFloat(notXml[i]);\n");
                } else if ("double".equals(attribute.getType())) {
                    sb.append(fieldName + " = Double.parseDouble(notXml[i]);\n");
                } else if ("java.lang.Boolean".equals(attribute.getType())) {
                    sb.append(fieldName + " = Boolean.valueOf(notXml[i]);\n");
                } else if ("java.lang.Short".equals(attribute.getType())) {
                    sb.append(fieldName + " = Short.valueOf(notXml[i]);\n");
                } else if ("java.lang.Integer".equals(attribute.getType())) {
                    sb.append(fieldName + " = Integer.valueOf(notXml[i]);\n");
                } else if ("java.lang.Long".equals(attribute.getType())) {
                    sb.append(fieldName + " = Long.valueOf(notXml[i]);\n");
                } else if ("java.lang.Float".equals(attribute.getType())) {
                    sb.append(fieldName + " = Float.valueOf(notXml[i]);\n");
                } else if ("java.lang.Double".equals(attribute.getType())) {
                    sb.append(fieldName + " = Double.valueOf(notXml[i]);\n");
                } else if ("java.util.Date".equals(attribute.getType())) {
                    sb.append(fieldName
                            + " = new java.util.Date(Long.parseLong(notXml[i]));\n");
                } else if ("java.math.BigDecimal".equals(attribute.getType())) {
                    sb.append(fieldName + " = new java.math.BigDecimal(notXml[i]);\n");
                } else if ("org.intermine.objectstore.query.ClobAccess".equals(attribute
                        .getType())) {
                    sb.append(fieldName + " = org.intermine.objectstore.query.ClobAccess"
                            + ".decodeDbDescription(os, notXml[i]);\n");
                } else if ("java.lang.String".equals(attribute.getType())) {
                    sb.append("StringBuilder string = null;\n")
                        .append(INDENT + INDENT + INDENT + INDENT)
                        .append("while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == '"
                                + ENCODED_DELIM + "')) {\n")
                        .append(INDENT + INDENT + INDENT + INDENT + INDENT)
                        .append("if (string == null) string = new StringBuilder(notXml[i]);\n")
                        .append(INDENT + INDENT + INDENT + INDENT + INDENT)
                        .append("i++;\n")
                        .append(INDENT + INDENT + INDENT + INDENT + INDENT)
                        .append("string.append(\"" + DELIM
                                + "\").append(notXml[i].substring(1));\n")
                        .append(INDENT + INDENT + INDENT + INDENT)
                        .append("}\n")
                        .append(INDENT + INDENT + INDENT + INDENT)
                        .append(fieldName
                                + " = string == null ? notXml[i] : string.toString();\n");
                } else {
                    throw new IllegalArgumentException("Unknown type " + attribute.getType());
                }
                sb.append(INDENT + INDENT + INDENT + INDENT)
                    .append("i++;\n")
                    .append(INDENT + INDENT + INDENT)
                    .append("}\n");
            } else if (field.isReference()) {
                ReferenceDescriptor reference = (ReferenceDescriptor) field;
                sb.append(INDENT + INDENT + INDENT)
                    .append("if ((i < notXml.length) &&\"r" + fieldName
                            + "\".equals(notXml[i])) {\n")
                    .append(INDENT + INDENT + INDENT + INDENT)
                    .append("i++;\n")
                    .append(INDENT + INDENT + INDENT + INDENT)
                    .append(fieldName + " = new ProxyReference(os, Integer.valueOf(notXml[i])"
                            + ", " + reference.getReferencedClassName() + ".class);\n")
                    .append(INDENT + INDENT + INDENT + INDENT)
                    .append("i++;\n")
                    .append(INDENT + INDENT + INDENT)
                    .append("};\n");
            }
        }
        sb.append(INDENT + INDENT + INDENT)
            .append("if (startI == i) {\n")
            .append(INDENT + INDENT + INDENT + INDENT)
            .append("throw new IllegalArgumentException(\"Unknown field \" + notXml[i]);\n")
            .append(INDENT + INDENT + INDENT)
            .append("}\n")
            .append(INDENT + INDENT)
            .append("}\n");
        for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
            String fieldName = field.getName();
            if ("notXml".equals(fieldName)) {
                fieldName = "this.notXml";
            } else if ("os".equals(fieldName)) {
                fieldName = "this.os";
            }
            if (field instanceof CollectionDescriptor) {
                CollectionDescriptor coll = (CollectionDescriptor) field;
                sb.append(INDENT + INDENT)
                    .append(fieldName + " = new ProxyCollection<" + coll.getReferencedClassName()
                            + ">(os, this, \"" + fieldName + "\", " + coll.getReferencedClassName()
                            + ".class);\n");
            }
        }
        sb.append(INDENT)
            .append("}\n");
        return sb.toString();
    }

    /**
     * Generates the getFieldValue method.
     *
     * @param cld the ClassDescriptor
     * @param proxy false to make the getFieldValue method, true to make the getFieldProxy method
     * @return a String with the method
     */
    public String generateGetFieldValue(ClassDescriptor cld, boolean proxy) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("public Object getField" + (proxy ? "Proxy" : "Value")
                    + "(final String fieldName) throws IllegalAccessException {\n");
        for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
            sb.append(INDENT + INDENT)
                .append("if (\"" + field.getName() + "\".equals(fieldName)) {\n");
            String fieldName = field.getName();
            if ("fieldName".equals(fieldName)) {
                fieldName = "this.fieldName";
            }
            if (field instanceof AttributeDescriptor) {
                AttributeDescriptor attribute = (AttributeDescriptor) field;
                if ("boolean".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append("return Boolean.valueOf(" + fieldName + ");\n");
                } else if ("short".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append("return Short.valueOf(" + fieldName + ");\n");
                } else if ("int".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append("return Integer.valueOf(" + fieldName + ");\n");
                } else if ("long".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append("return Long.valueOf(" + fieldName + ");\n");
                } else if ("float".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append("return Float.valueOf(" + fieldName + ");\n");
                } else if ("double".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append("return Double.valueOf(" + fieldName + ");\n");
                } else {
                    sb.append(INDENT + INDENT + INDENT)
                        .append("return " + fieldName + ";\n");
                }
            } else if (field.isReference()) {
                sb.append(INDENT + INDENT + INDENT);
                if (proxy) {
                    sb.append("return " + fieldName + ";\n");
                } else {
                    sb.append("if (" + fieldName + " instanceof ProxyReference) {\n")
                        .append(INDENT + INDENT + INDENT + INDENT)
                        .append("return ((ProxyReference) " + fieldName + ").getObject();\n")
                        .append(INDENT + INDENT + INDENT)
                        .append("} else {\n")
                        .append(INDENT + INDENT + INDENT + INDENT)
                        .append("return " + fieldName + ";\n")
                        .append(INDENT + INDENT + INDENT)
                        .append("}\n");
                }
            } else {
                sb.append(INDENT + INDENT + INDENT)
                    .append("return " + fieldName + ";\n");
            }
            sb.append(INDENT + INDENT)
                .append("}\n");
        }
        sb.append(INDENT + INDENT)
            .append("if (!" + cld.getName() + ".class.equals(getClass())) {\n")
            .append(INDENT + INDENT + INDENT)
            .append("return TypeUtil.getField" + (proxy ? "Proxy" : "Value")
                    + "(this, fieldName);\n")
            .append(INDENT + INDENT)
            .append("}\n")
            .append(INDENT + INDENT)
            .append("throw new IllegalArgumentException(\"Unknown field \" + fieldName);\n")
            .append(INDENT)
            .append("}\n");
        return sb.toString();
    }

    /**
     * Generates the setFieldValue method.
     *
     * @param cld the ClassDescriptor
     * @return a String with the method
     */
    public String generateSetFieldValue(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("public void setFieldValue(final String fieldName, final Object value) {\n")
            .append(INDENT + INDENT);
        for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
            sb.append("if (\"" + field.getName() + "\".equals(fieldName)) {\n");
            String fieldName = field.getName();
            if ("value".equals(fieldName)) {
                fieldName = "this.value";
            } else if ("fieldName".equals(fieldName)) {
                fieldName = "this.fieldName";
            }
            if (field instanceof AttributeDescriptor) {
                AttributeDescriptor attribute = (AttributeDescriptor) field;
                if ("boolean".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append(fieldName + " = ((Boolean) value).booleanValue();\n");
                } else if ("short".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append(fieldName + " = ((Short) value).shortValue();\n");
                } else if ("int".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append(fieldName + " = ((Integer) value).intValue();\n");
                } else if ("long".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append(fieldName + " = ((Long) value).longValue();\n");
                } else if ("float".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append(fieldName + " = ((Float) value).floatValue();\n");
                } else if ("double".equals(attribute.getType())) {
                    sb.append(INDENT + INDENT + INDENT)
                        .append(fieldName + " = ((Double) value).doubleValue();\n");
                } else {
                    sb.append(INDENT + INDENT + INDENT)
                        .append(fieldName + " = (" + attribute.getType() + ") value;\n");
                }
            } else if (field.isReference()) {
                sb.append(INDENT + INDENT + INDENT)
                    .append(fieldName + " = (org.intermine.model.InterMineObject) value;\n");
            } else {
                sb.append(INDENT + INDENT + INDENT)
                    .append(fieldName + " = (java.util.Set) value;\n");
            }
            sb.append(INDENT + INDENT)
                .append("} else ");
        }
        sb.append("{\n")
            .append(INDENT + INDENT + INDENT)
            .append("if (!" + cld.getName() + ".class.equals(getClass())) {\n")
            .append(INDENT + INDENT + INDENT + INDENT)
            .append("TypeUtil.setFieldValue(this, fieldName, value);\n")
            .append(INDENT + INDENT + INDENT + INDENT)
            .append("return;\n")
            .append(INDENT + INDENT + INDENT)
            .append("}\n")
            .append(INDENT + INDENT + INDENT)
            .append("throw new IllegalArgumentException(\"Unknown field \" + fieldName);\n")
            .append(INDENT + INDENT)
            .append("}\n")
            .append(INDENT)
            .append("}\n");
        return sb.toString();
    }

    /**
     * Generates the addCollectionElement method.
     *
     * @param cld the ClassDescriptor
     * @return a String with the method
     */
    public String generateAddCollectionElement(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("public void addCollectionElement(final String fieldName,")
            .append(" final org.intermine.model.InterMineObject element) {\n")
            .append(INDENT + INDENT);
        for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
            if (field.isCollection()) {
                String fieldName = field.getName();
                if ("fieldName".equals(fieldName)) {
                    fieldName = "this.fieldName";
                } else if ("element".equals(fieldName)) {
                    fieldName = "this.element";
                }
                sb.append("if (\"" + field.getName() + "\".equals(fieldName)) {\n")
                    .append(INDENT + INDENT + INDENT)
                    .append(fieldName + ".add(("
                            + ((CollectionDescriptor) field).getReferencedClassName()
                            + ") element);\n")
                    .append(INDENT + INDENT)
                    .append("} else ");
            }
        }
        sb.append("{\n")
            .append(INDENT + INDENT + INDENT)
            .append("if (!" + cld.getName() + ".class.equals(getClass())) {\n")
            .append(INDENT + INDENT + INDENT + INDENT)
            .append("TypeUtil.addCollectionElement(this, fieldName, element);\n")
            .append(INDENT + INDENT + INDENT + INDENT)
            .append("return;\n")
            .append(INDENT + INDENT + INDENT)
            .append("}\n")
            .append(INDENT + INDENT + INDENT)
            .append("throw new IllegalArgumentException(\"Unknown collection \" + fieldName);\n")
            .append(INDENT + INDENT)
            .append("}\n")
            .append(INDENT)
            .append("}\n");
        return sb.toString();
    }

    /**
     * Generates the getFieldType method.
     *
     * @param cld the ClassDescriptor
     * @return a String with the method
     */
    public String generateGetFieldType(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("public Class<?> getFieldType(final String fieldName) {\n");
        for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
            sb.append(INDENT + INDENT)
                .append("if (\"" + field.getName() + "\".equals(fieldName)) {\n")
                .append(INDENT + INDENT + INDENT);
            if (field instanceof AttributeDescriptor) {
                AttributeDescriptor attribute = (AttributeDescriptor) field;
                if ("boolean".equals(attribute.getType())) {
                    sb.append("return Boolean.TYPE;\n");
                } else if ("short".equals(attribute.getType())) {
                    sb.append("return Short.TYPE;\n");
                } else if ("int".equals(attribute.getType())) {
                    sb.append("return Integer.TYPE;\n");
                } else if ("long".equals(attribute.getType())) {
                    sb.append("return Long.TYPE;\n");
                } else if ("float".equals(attribute.getType())) {
                    sb.append("return Float.TYPE;\n");
                } else if ("double".equals(attribute.getType())) {
                    sb.append("return Double.TYPE;\n");
                } else {
                    sb.append("return " + attribute.getType() + ".class;\n");
                }
            } else if (field.isReference()) {
                sb.append("return " + ((ReferenceDescriptor) field).getReferencedClassName()
                        + ".class;\n");
            } else {
                sb.append("return java.util.Set.class;\n");
            }
            sb.append(INDENT + INDENT)
                .append("}\n");
        }
        sb.append(INDENT + INDENT)
            .append("if (!" + cld.getName() + ".class.equals(getClass())) {\n")
            .append(INDENT + INDENT + INDENT)
            .append("return TypeUtil.getFieldType(" + cld.getName() + ".class, fieldName);\n")
            .append(INDENT + INDENT)
            .append("}\n")
            .append(INDENT + INDENT)
            .append("throw new IllegalArgumentException(\"Unknown field \" + fieldName);\n")
            .append(INDENT)
            .append("}\n");
        return sb.toString();
    }

    /**
     * Generates the getElementType method.
     *
     * @param cld the ClassDescriptor
     * @return a String with the method
     */
    public String generateGetElementType(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("public Class<?> getElementType(final String fieldName) {\n");
        for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
            if (field.isCollection()) {
                sb.append(INDENT + INDENT)
                    .append("if (\"" + field.getName() + "\".equals(fieldName)) {\n")
                    .append(INDENT + INDENT + INDENT)
                    .append("return " + ((CollectionDescriptor) field).getReferencedClassName()
                        + ".class;\n")
                    .append(INDENT + INDENT)
                    .append("}\n");
            }
        }
        sb.append(INDENT + INDENT)
            .append("if (!" + cld.getName() + ".class.equals(getClass())) {\n")
            .append(INDENT + INDENT + INDENT)
            .append("return TypeUtil.getElementType(" + cld.getName() + ".class, fieldName);\n")
            .append(INDENT + INDENT)
            .append("}\n")
            .append(INDENT + INDENT)
            .append("throw new IllegalArgumentException(\"Unknown field \" + fieldName);\n")
            .append(INDENT)
            .append("}\n");
        return sb.toString();
    }
}
