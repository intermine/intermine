package org.intermine.bio.task;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.dataconversion.OboConverter;
import org.intermine.bio.dataconversion.EmapaOboConverter;
import org.intermine.bio.dataconversion.OboConverterInterface;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.task.ConverterTask;

/**
 * Initiates retrieval and conversion of data from a source database
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class OboConverterTask extends ConverterTask
{
//    protected static final Logger LOG = Logger.getLogger(OboConverterTask.class);

    private String file, ontologyName, osName, url, termClass, converterClass;

    /**
     * Set the input file name
     * @param file the database name
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Set the name of the ontology
     * @param ontologyName name of the ontology
     */
    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
    }

    /**
     * Set the objectstore name
     * @param osName the model name
     */
    @Override
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * Set the url for the source of the ontology
     *
     * @param url the URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Set the term class name
     *
     * @param termClass the class of the term
     */
    public void setTermClass(String termClass) {
        this.termClass = termClass;
    }

    /**
     * Set the converterClass
     *
     * @param converterClass the nave of the OBO converter class to use
     * Default class is OboConverter
     */
    public void setConverter(String className){
        this.converterClass = className;
    }

    /**
     * Run the task
     * @throws BuildException if a problem occurs
     */
    @Override
    public void execute() {
        if (file == null) {
            throw new BuildException("database attribute is not set");
        }
        if (ontologyName == null) {
            throw new BuildException("ontologyName attribute is not set");
        }
        if (osName == null) {
            throw new BuildException("model attribute is not set");
        }
        if (termClass == null) {
            throw new BuildException("termClass attribute is not set");
        }

        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);
            writer = new ObjectStoreItemWriter(osw);
            Model model = Model.getInstanceByName(getModelName());

            OboConverterInterface converter;
            if (file.endsWith(".obo")) {
                if(converterClass == null || converterClass.indexOf("$")!= -1){
                    converter = new OboConverter(writer, model, file, ontologyName, url, termClass);
                }else{
                    Class<?> c = Class.forName(converterClass);
                    Constructor<?> con = c.getConstructor(ItemWriter.class,Model.class,String.class,String.class,String.class,String.class);
                    converter = (OboConverterInterface) con.newInstance(writer,model,file,ontologyName,url,termClass);
                     
                }
            } else {
                throw new IllegalArgumentException("Don't know how to deal with file " + file);
            }
            configureDynamicAttributes(converter);
            converter.process();
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try {
                writer.close();
                osw.close();
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
    }
}
