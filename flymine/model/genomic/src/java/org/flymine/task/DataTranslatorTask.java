package org.flymine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemReader;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriterFactory;

/**
 * Task to translate data
 * @author Mark Woodbridge
 */
public class DataTranslatorTask extends Task
{
    protected String translator, source, target, model, targetNamespace, organism, dataLocation;

    /**
     * Set the translator
     * @param translator the translator
     */
    public void setTranslator(String translator) {
        this.translator = translator;
    }

    /**
     * Set the source
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Set the target
     * @param target the target
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Set the model
     * @param model the model
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Set the target namespace
     * @param targetNamespace the target namespace
     */
    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }

    /**
     * Set the organism abbreviation (for ensembl)
     * @param organism the organism abbreviation
     */
    public void setOrganism(String organism) {
        this.organism = organism;
    }

    /**
     * Set the data location (for protein-structure)
     * @param dataLocation the data location
     */
    public void setDataLocation(String dataLocation) {
        this.dataLocation = dataLocation;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (translator == null) {
            throw new BuildException("translator attribute not set");
        }
        if (source == null) {
            throw new BuildException("source attribute not set");
        }
        if (target == null) {
            throw new BuildException("target attribute not set");
        }
        if (model == null) {
            throw new BuildException("model attribute not set");
        }
        if (targetNamespace == null) {
            throw new BuildException("targetNamespace attribute not set");
        }

        try {
            Class cls = Class.forName(translator);
            ItemReader reader = null;
            try {
                Method m = cls.getMethod("getPrefetchDescriptors", null);
                reader = new ObjectStoreItemReader(ObjectStoreFactory.getObjectStore(source),
                                                   (Map) m.invoke(null, null));
            } catch (NoSuchMethodException e) {
                reader = new ObjectStoreItemReader(ObjectStoreFactory.getObjectStore(source));
            }
            Class[] types = null;
            Object[] args = null;
            if ("org.flymine.dataconversion.UniprotDataTranslator".equals(translator)) {
                types = new Class[] {ItemReader.class, String.class};
                args = new Object[] {reader, targetNamespace};
            } else {
                OntModel modelValue = ModelFactory.createOntologyModel();
                modelValue.read(new FileReader(new File(model)), null, "N3");
                if ("org.flymine.dataconversion.EnsemblDataTranslator".equals(translator)
                    || "org.flymine.dataconversion.EnsemblHumanDataTranslator".equals(translator)) {
                    if (organism == null) {
                        throw new BuildException("organism attribute not set");
                    }
                    types = new Class[]
                        {ItemReader.class, OntModel.class, String.class, String.class};
                    args = new Object[] {reader, modelValue, targetNamespace, organism};
                } else if ("org.flymine.dataconversion.ProteinStructureDataTranslator"
                           .equals(translator)) {
                    if (dataLocation == null) {
                        throw new BuildException("dataLocation attribute not set");
                    }
                    types = new Class[]
                        {ItemReader.class, OntModel.class, String.class, String.class};
                    args = new Object[] {reader, modelValue, targetNamespace, dataLocation};
                } else if ("org.flymine.dataconversion.ChadoDataTranslator".equals(translator)
                           || "org.flymine.dataconversion.PsiDataTranslator".equals(translator)
                           || "org.flymine.dataconversion.MageDataTranslator".equals(translator)) {
                    types = new Class[] {ItemReader.class, OntModel.class, String.class};
                    args = new Object[] {reader, modelValue, targetNamespace};
                }
                modelValue = null;
            }
            DataTranslator dt = (DataTranslator) cls.getConstructor(types).newInstance(args);
            ItemWriter writer = new ObjectStoreItemWriter(ObjectStoreWriterFactory
                                                          .getObjectStoreWriter(target));
            dt.translate(writer);
            writer.close();
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}