/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.codegen;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.flymine.util.StringUtil;
import org.flymine.util.TypeUtil;
import org.flymine.metadata.*;

/**
 * Maps FlyMine metadata to an OJB repository file
 *
 * @author Mark Woodbridge
 */
public class OJBModelOutput extends ModelOutput
{
    protected StringBuffer references, collections;

    /**
     * @see ModelOutput#Constructor
     */
    public OJBModelOutput(Model model, File file) throws Exception {
        super(model, file);
    }

    /**
     * @see ModelOutput#process
     */
    public void process() {
        File path = new File(file, "repository_" + model.getName() + ".xml");
        initFile(path);
        outputToFile(path, generate(model));
    }

    /**
     * @see ModelOutput#generate(Model)
     */
    protected String generate(Model model) {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ENDL)
            .append("<!DOCTYPE descriptor-repository SYSTEM \"repository.dtd\" [" + ENDL)
            .append("<!ENTITY internal SYSTEM \"repository_internal.xml\">" + ENDL)
            .append("]>" + ENDL + ENDL)
            .append("<descriptor-repository version=\"1.0\"")
            .append(" isolation-level=\"read-uncommitted\">" + ENDL);

        Iterator iter = model.getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            sb.append(generate(cld));
        }

        sb.append("&internal;" + ENDL)
            .append("</descriptor-repository>" + ENDL);

        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(ClassDescriptor)
     */
    protected String generate(ClassDescriptor cld) {
        references = new StringBuffer();
        collections = new StringBuffer();

        String tableName = null;
        if (!cld.isInterface()) {
            if (cld.getSuperclassDescriptor() == null) {
                tableName = TypeUtil.unqualifiedName(cld.getClassName());
            } else {
                tableName = TypeUtil.unqualifiedName(cld.getUltimateSuperclassDescriptor()
                                                     .getClassName());
            }
        }

        ClassDescriptor parentCld = cld.getSuperclassDescriptor();

        StringBuffer sb = new StringBuffer ();
        sb.append(INDENT)
            .append("<class-descriptor class=\"")
            .append(cld.getClassName())
            .append("\"")
            .append(parentCld == null ? "" : " extends=\"" + parentCld.getClassName() + "\"")
            .append(cld.isInterface() ? "" : " table=\"" + tableName + "\"")
            .append(">" + ENDL);

        Collection extent = cld.isInterface()
            ? cld.getImplementorDescriptors() : cld.getSubclassDescriptors();
        Iterator iter = extent.iterator();
        while (iter.hasNext()) {
            sb.append(INDENT + INDENT)
                .append("<extent-class class-ref=\"")
                .append(((ClassDescriptor) iter.next()).getClassName())
                .append("\"/>" + ENDL);
        }

        if (!cld.isInterface()) {
            sb.append(INDENT + INDENT)
                .append("<field-descriptor")
                .append(" name=\"id\"")
                .append(" column=\"ID\"")
                .append(" jdbc-type=\"INTEGER\"")
                .append(" primarykey=\"true\"")
                .append(" autoincrement=\"true\"/>" + ENDL);

            if (parentCld != null || cld.getSubclassDescriptors().size() > 0) {
                sb.append(INDENT + INDENT)
                    .append("<field-descriptor")
                    .append(" name=\"ojbConcreteClass\"")
                    .append(" column=\"CLASS\"")
                    .append(" jdbc-type=\"VARCHAR\"/>" + ENDL);
            }

            if (parentCld != null) {
                Iterator superClds = getParents(cld).iterator();
                while (superClds.hasNext()) {
                    ClassDescriptor superCld = (ClassDescriptor) superClds.next();
                    doAttributes(superCld, sb);
                    doAssociations(superCld, sb);
                }
            }

            doAttributes(cld, sb);
            doAssociations(cld, sb);
        }

        sb.append("" + references + collections)
            .append(INDENT)
            .append("</class-descriptor>" + ENDL + ENDL);
        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(AttributeDescriptor)
     */
    protected String generate(AttributeDescriptor attr) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT + INDENT)
            .append("<field-descriptor name=\"")
            .append(attr.getName())
            .append("\" column=\"")
            .append(generateSqlCompatibleName(attr.getName()))
            .append("\" jdbc-type=\"")
            .append(generateOJBSqlType(attr.getType()))
            .append("\"/>" + ENDL);
        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(ReferenceDescriptor)
     */
    protected String generate(ReferenceDescriptor ref) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT + INDENT)
            .append("<field-descriptor name=\"")
            .append(ref.getName())
            .append("Id\" column=\"")
            .append(ref.getName())
            .append("Id\" access=\"anonymous\" jdbc-type=\"INTEGER\"/>" + ENDL);
        references.append(INDENT + INDENT)
            .append("<reference-descriptor name=\"")
            .append(ref.getName())
            .append("\" class-ref=\"")
            .append(ref.getReferencedClassDescriptor().getClassName() + "\"")
            .append(" proxy=\"true\"")
            .append(">" + ENDL)
            .append(INDENT + INDENT + INDENT)
            .append("<foreignkey field-ref=\"")
            .append(ref.getName())
            .append("Id")
            .append("\"/>" + ENDL)
            .append(INDENT + INDENT)
            .append("</reference-descriptor>" + ENDL);
        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(CollectionDescriptor)
     */
    protected String generate(CollectionDescriptor col) {
        StringBuffer sb = new StringBuffer();
        String name2 = col.getName();
        ReferenceDescriptor rd = col.getReverseReferenceDescriptor();

        if (rd == null || rd instanceof CollectionDescriptor) { //many:many
            String name1;
            if (rd == null) {
                name1 = StringUtil.decapitalise(TypeUtil.unqualifiedName(col.getClassDescriptor()
                                                                         .getClassName()));
            } else {
                name1 = rd.getName();
            }
            String joiningTableName = "";
            if (name1.compareTo(name2) < 0) {
                joiningTableName = StringUtil.capitalise(name1) + StringUtil.capitalise(name2);
            } else {
                joiningTableName = StringUtil.capitalise(name2) + StringUtil.capitalise(name1);
            }

            collections.append(INDENT + INDENT)
                .append("<collection-descriptor name=\"")
                .append(name2)
                .append("\" element-class-ref=\"")
                .append(col.getReferencedClassDescriptor().getClassName())
                .append("\" collection-class=\"")
                .append(col.getCollectionClass().getName())
                .append("\" proxy=\"true\"")
                .append(" indirection-table=\"")
                .append(joiningTableName)
                .append("\">" + ENDL)
                .append(INDENT + INDENT + INDENT)
                .append("<fk-pointing-to-this-class column=\"")
                // Name of this class's primary key in linkage table
                .append(name1)
                .append("Id\"/>" + ENDL)
                .append(INDENT + INDENT + INDENT)
                .append("<fk-pointing-to-element-class column=\"")
                // Name of related class's primary key in linkage table
                .append(name2)
                .append("Id\"/>" + ENDL)
                .append(INDENT + INDENT)
                .append("</collection-descriptor>" + ENDL);
        } else if (col.getReverseReferenceDescriptor() instanceof ReferenceDescriptor) { //many:one
            String name1 = col.getReverseReferenceDescriptor().getName();
            collections.append(INDENT + INDENT)
                .append("<collection-descriptor name=\"")
                .append(name2)
                .append("\" element-class-ref=\"")
                .append(col.getReferencedClassDescriptor().getClassName())
                .append("\" collection-class=\"")
                .append(col.getCollectionClass().getName())
                .append("\" proxy=\"true\">" + ENDL)
                .append(INDENT + INDENT + INDENT)
                .append("<inverse-foreignkey field-ref=\"")
                .append(name1)
                .append("Id\"/>" + ENDL)
                .append(INDENT + INDENT)
                .append("</collection-descriptor>" + ENDL);
        }
        return sb.toString();
    }

    //=================================================================

    /**
     * Get all superclasses of the given class descriptor.
     * @param cld descriptor for class in question
     * @return a list of descriptors for superclasses
     */
    protected List getParents(ClassDescriptor cld) {
        List parentList = new ArrayList();
        ClassDescriptor superCld = cld.getSuperclassDescriptor();
        while (superCld != null) {
            parentList.add(superCld);
            superCld = superCld.getSuperclassDescriptor();
        }
        Collections.reverse(parentList);
        return parentList;
    }

    /**
     * Iterate over attributes of this class inquestion and generate
     * ouput text for each.
     * @param cld descriptor of class in question
     * @param sb a stringbuffer to write field data to
     */
    protected void doAttributes(ClassDescriptor cld, StringBuffer sb) {
        Iterator iter = cld.getAttributeDescriptors().iterator();
        while (iter.hasNext()) {
            sb.append(generate((AttributeDescriptor) iter.next()));
        }
    }

    /**
     * Iterate over associations for given class and create output data
     * for all references and collections.
     * @param cld descriptor of class in question
     * @param sb buffer to write field data to
     */
    protected void doAssociations(ClassDescriptor cld, StringBuffer sb) {
        Iterator iter;
        iter = cld.getReferenceDescriptors().iterator();
        while (iter.hasNext()) {
            sb.append(generate((ReferenceDescriptor) iter.next()));
        }
        iter = cld.getCollectionDescriptors().iterator();
        while (iter.hasNext()) {
            sb.append(generate((CollectionDescriptor) iter.next()));
        }
    }

    /**
     * Convert any sql keywords to valid names for tables/columns.
     * @param n the string to convert
     * @return a valid sql name
     */
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

    /**
     * Convert java primitive and object names to those compatible
     * with ojb reposiory file.  Returns unaltered string if no
     * conversion is required.
     * @param type the string to convert
     * @return ojb mapping file compatible name
     */
    protected String generateOJBSqlType(String type) {
        if (type.equals("int") || type.equals("java.lang.Integer")) {
            return "INTEGER";
        }
        if (type.equals("java.lang.String")) {
            return "LONGVARCHAR";
        }
        if (type.equals("boolean")) {
            return "INTEGER\" conversion=\""
                + "org.apache.ojb.broker.accesslayer.conversions.Boolean2IntFieldConversion";
        }
        if (type.equals("float") || type.equals("java.lang.Float")) {
            return "FLOAT";
        }
        if (type.equals("java.util.Date")) {
            return "DATE\" conversion=\""
                + "org.apache.ojb.broker.accesslayer.conversions.JavaDate2SqlDateFieldConversion";
        }
        return type;
    }
}
