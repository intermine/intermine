package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

import org.intermine.bio.dataconversion.GFF3RecordHandler;
import org.intermine.bio.io.gff3.GFF3Record;

/**
 * A converter/retriever for the long oligo dataset.
 *
 * @author Kim Rutherford
 */

public class LongOligoGFF3RecordHandler extends GFF3RecordHandler
{
    private String tgtNs;

    /**
     * Create a new LongOligoGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public LongOligoGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);
        tgtNs = tgtModel.getNameSpace().toString();
    }

    /**
     * @see GFF3RecordHandler#process(GFF3Record)
     */
    public void process(GFF3Record record) {
        Item oligo = getFeature();

        String olen = (String) ((List) record.getAttributes().get("olen")).get(0);
        oligo.setAttribute("length", olen);
        String oaTm = (String) ((List) record.getAttributes().get("oaTm")).get(0);
        oligo.setAttribute("tm", oaTm);

        oligo.setReference("transcript", getSequence().getIdentifier());

        String residues = (String) ((List) record.getAttributes().get("sequence")).get(0);
        if (residues != null) {
            Item seqItem = getItemFactory().makeItem(null, tgtNs + "Sequence", "");
            seqItem.setAttribute("residues", residues);
            addItem(seqItem);
            oligo.setReference("sequence", seqItem.getIdentifier());
        }

        List aliases = (List) record.getAttributes().get("Alias");

        Iterator aliasIter = aliases.iterator();

        while (aliasIter.hasNext()) {
            addItem(createSynonym(oligo, "identifier", (String) aliasIter.next()));
        }
    }

    /**
     * Create a synonym Item from the given information.
     */
    private Item createSynonym(Item subject, String type, String value) {
        Item synonym = getItemFactory().makeItem(null, tgtNs + "Synonym", "");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("subject", subject.getIdentifier());
        synonym.setReference("source", getDataSource().getIdentifier());
        return synonym;
    }
}
