package org.intermine.codegen;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.metadata.*;

/**
 * Maps InterMine metadata to Java source files
 *
 * @author Mark Woodbridge
 */
public class JavaModelOutput extends ModelOutput
{
    /**
     * @see ModelOutput#ModelOutput(Model, File)
     *
     * @param model a Model
     * @param file a File
     * @throws Exception if something goes wrong
     */
    public JavaModelOutput(Model model, File file) throws Exception {
        super(model, file);
    }

    /**
     * {@inheritDoc}
     */
    public void process() {
        Iterator iter = model.getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            String cldName = cld.getName();
            if (!"org.intermine.model.InterMineObject".equals(cldName)) {
                String pkg = TypeUtil.packageName(cldName);
                String cls = TypeUtil.unqualifiedName(cld.getName());
                String separator = File.separator;
                // Escape windows path seperator
                if (separator.equals("\\")) {
                    separator = "\\\\";
                }
                File dir = new File(file, pkg.replaceAll("[.]", separator));
                dir.mkdirs();
                File path = new File(dir, cls + ".java");
                initFile(path);
                outputToFile(path, generate(cld));
            }
        }
    }

    /**
     * This mapping generates one file per ClassDescriptor, so nothing output for the Model itself
     * {@inheritDoc}
     */
    protected String generate(Model model) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected String generate(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();

        String packageName = TypeUtil.packageName(cld.getName());

        if (packageName.length() > 0) {
            sb.append("package ")
                .append(packageName)
                .append(";" + ENDL + ENDL);
        }
        sb.append("public ")
            .append(cld.isInterface() ? "interface " : "class ")
            .append(TypeUtil.unqualifiedName(cld.getName()));

        if (!cld.isInterface()) {
            if (cld.getSuperclassDescriptor() != null) {
                sb.append(" extends ")
                    .append(cld.getSuperclassDescriptor().getName());
            }
        }

        boolean firstTime = true;

        if (cld.getSuperDescriptors().size() > 0) {
            Iterator iter = cld.getSuperDescriptors().iterator();
            while (iter.hasNext()) {
                ClassDescriptor superCld = (ClassDescriptor) iter.next();
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
        }

        sb.append(ENDL)
            .append("{" + ENDL);

        // FieldDescriptors defined for this class/interface
        if (cld.isInterface()) {
            sb.append(generateFieldDescriptors(cld, false));
        } else {
            sb.append(generateFieldDescriptors(cld, true))
                .append(generateEquals(cld))
                .append(generateHashCode(cld))
                .append(generateToString(cld));
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
        Set superclassFields = Collections.EMPTY_SET;
        if (supers && (cld.getSuperclassDescriptor() != null)) {
            superclassFields = cld.getSuperclassDescriptor().getAllFieldDescriptors();
        }
        StringBuffer sb = new StringBuffer();
        Iterator iter;
        if (supers) {
            iter = cld.getAllFieldDescriptors().iterator();
        } else {
            iter = cld.getFieldDescriptors().iterator();
        }
        while (iter.hasNext()) {
            FieldDescriptor fd = (FieldDescriptor) iter.next();
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
     * {@inheritDoc}
     */
    protected String generate(AttributeDescriptor attr) {
        return generate(attr, false);
    }
    /**
     * {@inheritDoc}
     */
    protected String generate(ReferenceDescriptor attr) {
        return generate(attr, false);
    }
    /**
     * {@inheritDoc}
     */
    protected String generate(CollectionDescriptor attr) {
        return generate(attr, false);
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    protected String generate(CollectionDescriptor col, boolean field) {
        String type = "java.util.Set";
        String impl = "java.util.HashSet";

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
                  .append(" instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((")
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
            .append("(")
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
                    .append("(")
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
                    .append("(org.intermine.objectstore.proxy.ProxyReference ")
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
                .append("public boolean equals(Object o) { return (o instanceof ")
                .append(unqualifiedName)
                .append(" && id != null) ? id.equals(((")
                .append(unqualifiedName)
                .append(")o).getId()) : false; }" + ENDL);
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
                .append("public int hashCode() { ")
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

        StringBuffer sb = new StringBuffer();
        Set keyFields = cld.getAllFieldDescriptors();
        if (keyFields.size() > 0) {
            sb.append(INDENT)
                .append("public String toString() { ")
                .append("return \"")
                .append(unqualifiedName)
                .append(" [");
            TreeMap sortedMap = new TreeMap();
            Iterator iter = keyFields.iterator();
            while (iter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) iter.next();
                sortedMap.put(field.getName(), field);
            }
            iter = sortedMap.entrySet().iterator();
            boolean needComma = false;
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                FieldDescriptor field = (FieldDescriptor) entry.getValue();
                if (needComma) {
                    sb.append(", ");
                }
                needComma = true;
                sb.append(field.getName());
                if (field instanceof AttributeDescriptor) {
                    sb.append("=\\\"\" + " + field.getName() + " + \"\\\"");
                } else if (field instanceof CollectionDescriptor) {
                    sb.append(":Collection");
                } else {
                    sb.append("=\" + (" + field.getName() + " == null ? \"null\" : ("
                            + field.getName() + ".getId() == null ? \"no id\" : "
                            + field.getName() + ".getId().toString())) + \"");
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
            type = "java.util.Set";
        } else {
            type = ((ReferenceDescriptor) field).getReferencedClassDescriptor().getName();
        }
        return type;
    }
}
