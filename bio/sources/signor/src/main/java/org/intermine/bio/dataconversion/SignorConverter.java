package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 *
 * @author Julie Sullivan
 */
public class SignorConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Signor data set";
    private static final String DATA_SOURCE_NAME = "Signor";
    private static final String PROTEIN = "protein";
    private Map<String, String> entities = new HashMap<String, String>();
    private Map<String, String> publications = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public SignorConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseDelimitedReader(reader, ';');

        // skip header
        lineIter.next();

        // each gene is on a new line
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length < 27) {
                continue;
            }

            String type1 = line[1];
            String identifier1 = line[2];
            String participant1 = getEntity(identifier1, type1);

            String type2 = line[5];
            String identifier2 = line[6];
            String participant2 = getEntity(identifier2, type2);

            String effect = line[8];
            String effectMechanism = line[9];
            String mechanismResidues = line[10];
            String mechanismSequences = line[11];

            String pubmedId = getPublication(line[21]);
            String notes = line[23];
            String signorId = line[26];

            Item signalling = createItem("Signalling");
            if (StringUtils.isNotEmpty(signorId)) {
                signalling.setAttribute("identifier", signorId);
            }
            signalling.setReference("participant1", participant1);
            signalling.setReference("participant2", participant2);
            store(signalling);

            Item detail = createItem("SignallingDetail");
            if (StringUtils.isNotEmpty(effect)) {
                detail.setAttribute("effect", effect);
            }
            if (StringUtils.isNotEmpty(effectMechanism)) {
                detail.setAttribute("effectMechanism", effectMechanism);
            }
            if (StringUtils.isNotEmpty(mechanismResidues)) {
                detail.setAttribute("mechanismResidues", mechanismResidues);
            }
            if (StringUtils.isNotEmpty(mechanismSequences)) {
                detail.setAttribute("mechanismSequences", mechanismSequences);
            }
            if (StringUtils.isNotEmpty(notes)) {
                detail.setAttribute("notes", notes);
            }
            if (StringUtils.isNotEmpty(pubmedId)) {
                detail.addToCollection("publications", pubmedId);
            }
            detail.setReference("signalling", signalling);
            store(detail);
        }
    }

    private String getEntity(String primaryIdentifier, String type) throws ObjectStoreException {
        String refId = entities.get(primaryIdentifier);
        if (refId == null) {
            Item item = null;
            if (PROTEIN.equals(type)) {
                item = createItem("Protein");
                item.setAttribute("primaryAccession", primaryIdentifier);
            } else {
                item = createItem("BioEntity");
                item.setAttribute("primaryIdentifier", primaryIdentifier);
            }
            store(item);
            refId = item.getIdentifier();
            entities.put(primaryIdentifier, refId);
        }
        return refId;
    }

    private String getPublication(String pubMedId) throws ObjectStoreException {
        if (StringUtils.isEmpty(pubMedId)) {
            return null;
        }
        String refId = publications.get(pubMedId);
        if (refId == null) {
            Item item = createItem("Publication");
            item.setAttribute("pubMedId", pubMedId);
            store(item);
            refId = item.getIdentifier();
            publications.put(pubMedId, refId);
        }
        return refId;
    }
}
