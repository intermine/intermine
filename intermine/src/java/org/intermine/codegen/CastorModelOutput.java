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
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.flymine.metadata.*;

/**
 * Maps FlyMine metadata to a Castor XML binding mapping file
 *
 * @author Mark Woodbridge
 */
public class CastorModelOutput extends ModelOutput
{
    protected Set seen = new HashSet();
    protected StringBuffer references, collections;

    /**
     * @see ModelOutput#Constructor
     */
    public CastorModelOutput(Model model, File file) throws Exception {
        super(model, file);
    }

    /**
     * @see ModelOutput#process
     */
    public void process() {
        File path = new File(file, "castor_xml_" + model.getName() + ".xml");
        initFile(path);
        outputToFile(path, generate(model));
    }

    /**
     * @see ModelOutput#generate(Model)
     */
    protected String generate(Model model) {
        StringBuffer sb = new StringBuffer();
        sb .append("<!DOCTYPE databases PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\" ")
            .append("\"http://castor.exolab.org/mapping.dtd\">" + ENDL)
            .append("<mapping>" + ENDL)
            .append(INDENT)
            .append("<include href=\"castor_xml_include.xml\"/>" + ENDL + ENDL);

        Iterator iter = model.getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            if (!cld.isInterface() && !seen.contains(cld)) {
                sb.append(generate(cld));
            }
        }

        sb.append("</mapping>" + ENDL);
        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(ClassDescriptor)
     */
    protected String generate(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();

        ClassDescriptor superCld = cld.getSuperclassDescriptor();
        if (superCld != null && !seen.contains(superCld)) {
            sb.append(generate(superCld)); //avoid forward references
        }

        seen.add(cld);

        references = new StringBuffer();
        collections = new StringBuffer();

        sb.append(INDENT)
            .append("<class name=\"")
            .append(cld.getClassName())
            .append("\"");
        if (superCld != null) {
            sb.append(" extends=\"")
                .append(superCld.getClassName())
                .append("\"");
        }

        sb.append(" auto-complete=\"true\" identity=\"id\">" + ENDL);

        if (superCld == null) {
            sb.append(INDENT + INDENT)
                .append("<field name=\"id\" type=\"java.lang.Integer\">" + ENDL)
                .append(INDENT + INDENT + INDENT)
                .append("<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL)
                .append(INDENT + INDENT)
                .append("</field>" + ENDL);
        }

        Iterator iter = cld.getAttributeDescriptors().iterator();
        while (iter.hasNext()) {
            sb.append(generate((AttributeDescriptor) iter.next()));
        }
        iter = cld.getReferenceDescriptors().iterator();
        while (iter.hasNext()) {
            generate((ReferenceDescriptor) iter.next());
        }
        iter = cld.getCollectionDescriptors().iterator();
        while (iter.hasNext()) {
            generate((CollectionDescriptor) iter.next());
        }

        sb.append("" + references + collections)
            .append(INDENT)
            .append("</class>" + ENDL + ENDL);

        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(AttributeDescriptor)
     */
    protected String generate(AttributeDescriptor attr) {
        StringBuffer sb = new StringBuffer();

        sb.append(INDENT + INDENT)
            .append("<field name=\"")
            .append(attr.getName())
            .append("\" type=\"")
            .append(convertType(attr.getType()))
            .append("\">" + ENDL);

        sb.append(INDENT + INDENT + INDENT)
            .append("<bind-xml name=\"")
            .append(attr.getName());

        if (isPrimitive(attr.getType()) || attr.getType().equals("java.lang.String")) {
            sb.append("\" node=\"attribute\"/>" + ENDL);
        } else {
            sb.append("\" node=\"element\"/>" + ENDL);
        }
        sb.append(INDENT + INDENT)
            .append("</field>" + ENDL);

        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(ReferenceDescriptor)
     */
    protected String generate(ReferenceDescriptor ref) {
        references.append(INDENT + INDENT)
            .append("<field name=\"")
            .append(ref.getName())
            .append("\" type=\"")
            .append(ref.getReferencedClassDescriptor().getClassName())
            .append("\">" + ENDL)
            .append(INDENT + INDENT + INDENT)
            .append("<bind-xml name=\"")
            .append(ref.getName())
            .append("\" reference=\"true\" node=\"attribute\"/>" + ENDL)
            .append(INDENT + INDENT)
            .append("</field>" + ENDL);
        return "";
    }

    /**
     * @see ModelOutput#generate(CollectionDescriptor)
     */
    protected String generate(CollectionDescriptor col) {
        collections.append(INDENT + INDENT)
            .append("<field name=\"")
            .append(col.getName())
            .append("\" type=\"")
            .append(col.getReferencedClassDescriptor().getClassName())
            .append("\" collection=\"collection\">" + ENDL)
            .append(INDENT + INDENT + INDENT)
            .append("<bind-xml name=\"")
            .append(col.getName())
            .append("\" reference=\"true\" node=\"attribute\"/>" + ENDL)
            .append(INDENT + INDENT)
            .append("</field>" + ENDL);
        return "";
    }

    /**
     * Convert names of primitives/java objects to names compatible with
     * Castor mapping file.  Returns unaltered string if no conversion needed.
     * @param type the name to be converted
     * @return Castor mapping file compatible string
     */
    protected String convertType(String type) {
        if (type.equals("int")) {
            return "integer";
        }
        return type;
    }
}
