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
import javax.xml.namespace.QName;

import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import org.flymine.metadata.*;
import org.flymine.objectstore.webservice.ser.SerializationUtil;

/**
 * Maps FlyMine metadata to a Castor XML binding mapping file
 *
 * @author Mark Woodbridge
 */
public class AxisModelOutput extends ModelOutput
{
    /**
     * @see ModelOutput#ModelOutput(Model, File)
     */
    public AxisModelOutput(Model model, File file) throws Exception {
        super(model, file);
    }

    /**
     * @see ModelOutput#process
     */
    public void process() {
        File path = new File(file, "deploy-ObjectStore.wsdd");
        initFile(path);
        outputToFile(path, generate(model));
    }

    /**
     * @see ModelOutput#generate(Model)
     */
    protected String generate(Model model) {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ENDL + ENDL)
                .append("<deployment xmlns=\"http://xml.apache.org/axis/wsdd/\"" + ENDL)
                .append(INDENT + "xmlns:java=\"http://xml.apache.org/axis/wsdd/providers/java\">"
                        + ENDL)
                .append(INDENT + "<service name=\"ObjectStore\" provider=\"java:RPC\">" + ENDL)
                .append(INDENT + INDENT + "<parameter name=\"className\""
                        + " value=\"org.flymine.objectstore.webservice.ObjectStoreServer\"/>"
                        + ENDL)
                .append(INDENT + INDENT + "<parameter name=\"scope\" value=\"Session\"/>" + ENDL)
                .append(INDENT + INDENT + "<parameter name=\"allowedMethods\" value=\"*\"/>"
                        + ENDL + ENDL);

            TypeMapping tm = ((Call) new Service().createCall()).getTypeMapping();
            SerializationUtil.registerDefaultMappings(tm);
            SerializationUtil.registerMappings(tm, model);

            Class[] classes = tm.getAllClasses();
            for (int i = 0; i < classes.length; i++) {
                Class cls = classes[i];
                if (cls.getName().startsWith("org.flymine")
                   || cls.getName().equals("java.util.ArrayList")) {

                    QName qname = tm.getTypeQName(cls);
                    String localPart = qname.getLocalPart();
                    String namespace = qname.getNamespaceURI();
                    String prefix = null;
                    if (!"".equals(namespace)) {
                        prefix = namespace.substring(namespace.lastIndexOf("/") + 1,
                                                     namespace.length());
                    }
                        
                    String type = "java:" + cls.getName();
                    String serializer = tm.getSerializer(cls).getClass().getName();
                    String deserializer = tm.getDeserializer(qname).getClass().getName();
                    String encoding = "http://schemas.xmlsoap.org/soap/encoding/";

                    sb.append(INDENT + "<typeMapping qname=\"")
                        .append(prefix != null ? prefix + ":" : "")
                        .append(localPart + "\"" + ENDL)
                        .append(prefix != null ? INDENT + INDENT + "xmlns:" + prefix + "=\""
                                + namespace + "\"" + ENDL : "")
                        .append(INDENT + INDENT + "type=\"" + type + "\"" + ENDL)
                        .append(INDENT + INDENT + "serializer=\"" + serializer + "\"" + ENDL)
                        .append(INDENT + INDENT + "deserializer=\"" + deserializer + "\"" + ENDL)
                        .append(INDENT + INDENT + "encodingStyle=\"" + encoding + "\"/>" + ENDL);
                }
            }

            sb.append(INDENT + "</service>" + ENDL)
                .append("</deployment>");
        } catch (Exception e) {
            LOG.error(e);
        }
        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(ClassDescriptor)
     */
    protected String generate(ClassDescriptor cld) {
        return "";
    }

    /**
     * @see ModelOutput#generate(AttributeDescriptor)
     */
    protected String generate(AttributeDescriptor attr) {
        return "";
    }

    /**
     * @see ModelOutput#generate(ReferenceDescriptor)
     */
    protected String generate(ReferenceDescriptor ref) {
        return "";
    }

    /**
     * @see ModelOutput#generate(CollectionDescriptor)
     */
    protected String generate(CollectionDescriptor col) {
        return "";
    }
}
