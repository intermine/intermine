package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Set;

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.ontology.DagParser;
import org.intermine.ontology.DagTerm;
import org.intermine.util.TypeUtil;

/**
 * Processes list of root DagTerms to produce data
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class DagConverter extends DataConverter
{
    protected String dagFilename;
    protected String namespace;
    protected int uniqueId = 0;

    /**
     * Constructor for this class.
     *
     * @param writer an ItemWriter used to handle the resultant Items
     * @param dagFilename the name of the DAG file
     * @param namespace the namespace of the model (including the trailing hash symbol)
     */
    public DagConverter(ItemWriter writer, String dagFilename, String namespace) {
        super(writer);
        this.dagFilename = dagFilename;
        this.namespace = namespace;
    }

    /**
     * Process every DAG term and output it as a Item.
     *
     * @throws Exception if an error occurs in processing
     */
    public void process() throws Exception {
        try {
            File dagFile = new File(dagFilename);

            DagParser parser = new DagParser();
            Set rootTerms = parser.process(new FileReader(dagFile));

            Iterator termIter = rootTerms.iterator();
            while (termIter.hasNext()) {
                DagTerm term = (DagTerm) termIter.next();
                process(term);
            }
        } finally {
            writer.close();
        }
    }

    /**
     * Convert a DagTerm into an Item, and write it to the writer.
     *
     * @param term a DagTerm
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    public void process(DagTerm term) throws ObjectStoreException {
        String className = namespace + TypeUtil.javaiseClassName(term.getName());
        Item item = new Item();
        item.setIdentifier("0_" + (uniqueId++));
        item.setClassName(className);
        item.setImplementations("");
        Attribute attribute = new Attribute();
        attribute.setItem(item);
        attribute.setName("label");
        attribute.setValue(term.getName());
        item.addAttributes(attribute);
        if (term.getId() != null) {
            attribute = new Attribute();
            attribute.setItem(item);
            attribute.setName("ID");
            attribute.setValue(term.getId());
            item.addAttributes(attribute);
        }
        writer.store(item);
        Iterator iter = term.getChildren().iterator();
        while (iter.hasNext()) {
            process((DagTerm) iter.next());
        }
    }
}
