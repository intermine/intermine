package org.flymine.codegen;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.flymine.util.StringUtil;
import org.flymine.util.TypeUtil;
import org.flymine.metadata.*;

/**
 * Maps FlyMine metadata to Java source files
 *
 * @author Mark Woodbridge
 */
public class JavaModelOutput extends ModelOutput
{
    /**
     * @see ModelOutput#ModelOutput(Model, File)
     */
    public JavaModelOutput(Model model, File file) throws Exception {
        super(model, file);
    }

    /**
     * @see ModelOutput#process
     */
    public void process() {
        Iterator iter = model.getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            String cldName = cld.getName();
            String pkg = TypeUtil.packageName(cldName);
            String cls = TypeUtil.unqualifiedName(cld.getName());
            File dir = new File(file, pkg.replaceAll("[.]", File.separator));
            dir.mkdirs();
            File path = new File(dir, cls + ".java");
            initFile(path);
            outputToFile(path, generate(cld));
        }
    }

    /**
     * This mapping generates one file per ClassDescriptor, so nothing output for the Model itself
     * @see ModelOutput#generate(Model)
     */
    protected String generate(Model model) {
        return null;
    }

    /**
     * @see ModelOutput#generate(ClassDescriptor)
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

        if (cld.getSuperclassDescriptor() != null) {
            sb.append(" extends ")
                .append(TypeUtil.unqualifiedName(cld.getSuperclassDescriptor().getName()));
        }

        if (cld.getInterfaceDescriptors().size() > 0) {
            sb.append(" implements ");
            Iterator iter = cld.getInterfaceDescriptors().iterator();
            while (iter.hasNext()) {
                ClassDescriptor interfaceCld = (ClassDescriptor) iter.next();
                sb.append(TypeUtil.unqualifiedName(interfaceCld.getName()));
                if (iter.hasNext()) {
                    sb.append(", ");
                }
            }
        }

        sb.append(ENDL)
            .append("{" + ENDL);

        if (!cld.isInterface()) {
            if (cld.getSuperclassDescriptor() == null) {
                sb.append(INDENT + "protected Integer id;" + ENDL)
                    .append(INDENT + "public Integer getId() { return id; }" + ENDL + ENDL);
            }

            if (cld.getSuperclassDescriptor() != null || !cld.getSubclassDescriptors().isEmpty()) {
                sb.append(INDENT + "protected String ojbConcreteClass = \"")
                    .append(cld.getName())
                    .append("\";" + ENDL + ENDL);
            }
        }

        Iterator iter;
        iter = cld.getAttributeDescriptors().iterator();
        while (iter.hasNext()) {
            sb.append(generate((AttributeDescriptor) iter.next()));
        }
        iter = cld.getReferenceDescriptors().iterator();
        while (iter.hasNext()) {
            sb.append(generate((ReferenceDescriptor) iter.next()));
        }
        iter = cld.getCollectionDescriptors().iterator();
        while (iter.hasNext()) {
            sb.append(generate((CollectionDescriptor) iter.next()));
        }

        sb.append(generateEquals(cld))
            .append(generateEqualsPK(cld))
            .append(generateHashCode(cld))
            .append(generateToString(cld))
            .append("}");

        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(AttributeDescriptor)
     */
    protected String generate(AttributeDescriptor attr) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT + "protected ")
            .append(attr.getType())
            .append(" ")
            .append(attr.getName())
            .append(";" + ENDL)
            .append(generateGetSet(attr))
            .append(ENDL);

        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(ReferenceDescriptor)
     */
    protected String generate(ReferenceDescriptor ref) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("protected ")
            .append(TypeUtil.unqualifiedName(ref.getReferencedClassDescriptor().getName()))
            .append(" ")
            .append(ref.getName())
            .append(";" + ENDL)
            .append(generateGetSet(ref))
            .append(ENDL);
        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(CollectionDescriptor)
     */
    protected String generate(CollectionDescriptor col) {
        String type = col.isOrdered() ? "java.util.List" : "java.util.Set";
        String impl = col.isOrdered() ? "java.util.ArrayList" : "java.util.HashSet";

        StringBuffer sb = new StringBuffer();
        sb.append(INDENT)
            .append("protected ")
            .append(type)
            .append(" ")
            .append(col.getName())
            .append(" = new ")
            .append(impl)
            .append("();" + ENDL)
            .append(generateGetSet(col))
            .append(ENDL);
        return sb.toString();
    }

    //=================================================================

    /**
     * Write code for getters and setters for given field.
     * @param field descriptor for field
     * @return string with generated java code
     */
    protected String generateGetSet(FieldDescriptor field) {
        String name = field.getName(), type = getType(field);

        StringBuffer sb = new StringBuffer();

        // Get method
        sb.append(INDENT)
            .append("public ")
            .append(type)
            .append(" get")
            .append(StringUtil.capitalise(name))
            .append("() { ")
            .append("return this.")
            .append(name)
            .append("; }" + ENDL);

        // Set method
        sb.append(INDENT)
            .append("public void ")
            .append("set")
            .append(StringUtil.capitalise(name))
            .append("(")
            .append(type)
            .append(" ")
            .append(name)
            .append(") { ")
            .append("this.")
            .append(name)
            .append("=")
            .append(name)
            .append("; }" + ENDL);

        return sb.toString();
    }


    /**
     * Generate a .equals() method for the given class.
     * @param cld descriptor for class in question
     * @return generated java code as string
     */
    protected String generateEquals(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();

        String unqualifiedName = TypeUtil.unqualifiedName(cld.getName());

        Collection keyFields = cld.getPkFieldDescriptors();
        if (keyFields.size() > 0) {
            sb.append(INDENT)
                .append("public boolean equals(Object o) {" + ENDL)
                .append(INDENT + INDENT)
                .append("if (!(o instanceof ")
                .append(unqualifiedName)
                .append(")) return false;" + ENDL)
                .append(INDENT + INDENT)
                .append("return (id == null) ? equalsPK(o) : id.equals(((")
                .append(unqualifiedName)
                .append(")o).getId());" + ENDL)
                .append(INDENT)
                .append("}" + ENDL + ENDL);
        }

        return sb.toString();
    }

    /**
     * Generate a .equals() method based on the object's primary keys.
     * @param cld descriptor for class in question
     * @return generated java code as string
     */
    protected String generateEqualsPK(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();

        String unqualifiedName = TypeUtil.unqualifiedName(cld.getName());

        Collection keyFields = cld.getPkFieldDescriptors();
        if (keyFields.size() > 0) {
            sb.append(INDENT)
                .append("public boolean equalsPK(Object o) {" + ENDL)
                .append(INDENT + INDENT)
                .append("if (!(o instanceof ")
                .append(unqualifiedName)
                .append(")) return false;" + ENDL)
                .append(INDENT + INDENT)
                .append(unqualifiedName)
                .append(" obj = (")
                .append(unqualifiedName)
                .append(") o;" + ENDL)
                .append(INDENT + INDENT)
                .append("return obj.getId() == null && ");
            Iterator iter = keyFields.iterator();
            while (iter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) iter.next();
                if (cld.getAllAttributeDescriptors().contains(field)
                    && isPrimitive(((AttributeDescriptor) field).getType())) {
                    sb.append("obj.get")
                        .append(StringUtil.capitalise(field.getName()))
                        .append("() == ")
                        .append(field.getName());
                } else {
                    String thatField = "obj.get" + StringUtil.capitalise(field.getName()) + "()";
                    sb.append("(" + thatField + " == null ? (" + field.getName() + " == null) : "
                              + thatField + ".equals(" + field.getName() + "))");
//                     sb.append(field.getName())
//                         .append(".equals(obj.get")
//                         .append(StringUtil.capitalise(field.getName()))
//                         .append("())");
                }
                if (iter.hasNext()) {
                    sb.append(" && ");
                }
            }
            sb.append(";" + ENDL)
                .append(INDENT)
                .append("}" + ENDL + ENDL);
        }

        return sb.toString();
    }


    /**
     * Generate a .hashCode() method for the given class.
     * @param cld descriptor for the class in question
     * @return generate java code as a string
     */
    protected String generateHashCode(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();

        Collection keyFields = cld.getPkFieldDescriptors();
        if (keyFields.size() > 0) {
            sb.append(INDENT)
                .append("public int hashCode() {" + ENDL)
                .append(INDENT + INDENT)
                .append("if (id != null) return id.hashCode();" + ENDL)
                .append(INDENT + INDENT)
                .append("return ");
            Iterator iter = keyFields.iterator();
            while (iter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) iter.next();
                if (cld.getAllAttributeDescriptors().contains(field)
                    && isPrimitive(((AttributeDescriptor) field).getType())) {
                    if (((AttributeDescriptor) field).getType().equals("boolean")) {
                        sb.append("(" + field.getName() + " ? 0 : 1)");
                    } else {
                        sb.append(field.getName());
                    }
                } else {
                    // sb.append(field.getName() + ".hashCode()");
                    sb.append("(" + field.getName() + " == null ? 0 : " + field.getName()
                              + ".hashCode())");
                }
                if (iter.hasNext()) {
                    sb.append(" ^ ");
                }
            }
            sb.append(";" + ENDL)
                .append(INDENT)
                .append("}" + ENDL + ENDL);
        }
        return sb.toString();
    }

    /**
     * Generate a .toString() method for the given class .
     * @param cld descriptor for the class in question
     * @return generated java code as a string
     */
    protected String generateToString(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();

        String unqualifiedName = TypeUtil.unqualifiedName(cld.getName());

        Set keyFields = cld.getPkFieldDescriptors();
        if (keyFields.size() > 0) {
            sb.append(INDENT)
                .append("public String toString() { ")
                .append("return \"")
                .append(unqualifiedName)
                .append(" [\"+id+\"] \"+");
            Iterator iter = keyFields.iterator();
            while (iter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) iter.next();
                sb.append(field.getName());
                if (iter.hasNext()) {
                    sb.append("+\", \"+");
                }
            }
            sb.append("; }" + ENDL);
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
            if (((CollectionDescriptor) field).isOrdered()) {
                type = "java.util.List";
            } else {
                type = "java.util.Set";
            }
        } else {
            type = ((ReferenceDescriptor) field).getReferencedClassDescriptor().getName();
        }
        return type;
    }
}
