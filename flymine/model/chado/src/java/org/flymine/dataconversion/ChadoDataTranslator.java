package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.FileReader;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.InterMineException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.ontology.OntologyUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemReader;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.DataTranslator;

import org.apache.log4j.Logger;

/**
 * Convert Chado data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ChadoDataTranslator extends DataTranslator
{
    protected static final Logger LOG = Logger.getLogger(ChadoDataTranslator.class);

    /**
     * @see DataTranslator#DataTranslator
     */
    public ChadoDataTranslator(ItemReader srcItemReader, OntModel model, String ns) {
        super(srcItemReader, model, ns);
    }


    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {

        Collection result = new HashSet();
        String className = OntologyUtil.getFragmentFromURI(srcItem.getClassName());
        Collection translated = super.translateItem(srcItem);
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                boolean storeTgtItem = true;
                Item tgtItem = (Item) i.next();
                if ("author".equals(className)) {
                    tgtItem.addAttribute(new Attribute("name",
                                         srcItem.getAttribute("givennames").getValue()
                                         + " " + srcItem.getAttribute("surname").getValue()));
                } else if ("feature_synonym".equals(className)) {
                    promoteField(tgtItem, srcItem, "synonym", "synonym", "name");
                } else if ("feature_dbxref".equals(className)) {
                    promoteField(tgtItem, srcItem, "synonym", "dbxref", "accession");
                    promoteField(tgtItem, srcItem, "source", "dbxref", "db");
                }

                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }
        }
        return result;
    }

    /**
     * Main method
     * @param args command line arguments
     * @throws Exception if something goes wrong
     */
    public static void main (String[] args) throws Exception {
        String srcOsName = args[0];
        String tgtOswName = args[1];
        String modelName = args[2];
        String format = args[3];
        String namespace = args[4];

        Map paths = new HashMap();

        ObjectStore osSrc = ObjectStoreFactory.getObjectStore(srcOsName);
        ItemReader srcItemReader = new ObjectStoreItemReader(osSrc, paths);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new ObjectStoreItemWriter(oswTgt);

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(modelName)), null, format);
        DataTranslator dt = new ChadoDataTranslator(srcItemReader, model, namespace);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }
}
