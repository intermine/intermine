package org.intermine.objectstore.webservice.ser;

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
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import javax.xml.namespace.QName;

import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.apache.log4j.Logger;

/**
 * Maps InterMine metadata to typeMapping entries in a wsdd file
 *
 * @author Mark Woodbridge
 */
public class GenerateWSDDTask extends Task
{
    private static final Logger LOG = Logger.getLogger(GenerateWSDDTask.class);
    protected static final String INDENT = "    ";
    protected static final String ENDL = System.getProperty("line.separator");
    
    protected File destFile;

    /**
     * Sets the file that output should be written to.
     * @param destFile the file location
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * Run the task
     * @throws BuildException if a problem occurs
     */
    public void execute() throws BuildException {
        if (this.destFile == null) {
            throw new BuildException("destFile attribute is not set");
        }
        LOG.debug("Generating " + destFile.getPath());
        BufferedWriter fos = null;
        try {
            fos = new BufferedWriter(new FileWriter (destFile));
            fos.write(generate());
        } catch (IOException e) {
            LOG.error(e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * Iterate through the mapped types generating an XML element for each
     * 
     * @return an XML fragment of type mappings
     */
    protected String generate() {
        StringBuffer sb = new StringBuffer();
        try {
            TypeMapping tm = ((Call) new Service().createCall()).getTypeMapping();
            MappingUtil.registerDefaultMappings(tm);

            Class[] classes = tm.getAllClasses();
            for (int i = 0; i < classes.length; i++) {
                Class cls = classes[i];
                if (cls.getName().startsWith("org.intermine")
                   || cls.getName().equals("java.util.ArrayList")) {
                    sb.append(generateTypeMapping(cls, tm) + ENDL);
                }
            }
        } catch (Exception e) {
            LOG.error(e);
        }
        return sb.toString();
    }

    /**
     * Generates the typeMapping entry for a class
     * @param cls the Class
     * @param tm the TypeMapping that includes the serialization information for that class
     * @return an XML element as a string
     */
    protected String generateTypeMapping(Class cls, TypeMapping tm) {
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
       
        StringBuffer sb = new StringBuffer();
        sb.append("<typeMapping qname=\"")
            .append(prefix != null ? prefix + ":" : "")
            .append(localPart + "\"")
            .append(prefix != null ? " xmlns:" + prefix + "=\"" + namespace + "\"" : "")
            .append(" type=\"" + type + "\"")
            .append(" serializer=\"" + serializer + "\"")
            .append(" deserializer=\"" + deserializer + "\"")
            .append(" encodingStyle=\"" + encoding + "\"/>");
        return sb.toString();
    }
}
