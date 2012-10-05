package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * DataConverter to load VectorBase identifiers and old Ensembl ids
 *
 * @author Richard Smith
 */
public class AnophelesIdentifiersConverter extends BioFileConverter
{
    protected String organismRefId = null;
    protected Set<String> seenEnsIds = new HashSet<String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public AnophelesIdentifiersConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model, "VectorBase", "VectorBase Anopheles");
        organismRefId = getOrganism("7165");
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // data is in format:
        // primaryIdentifier | identifier | symbol
        String clsName;
        String fileName = getCurrentFile().getPath();
        if (fileName.contains("Genes")) {
            clsName = "Gene";
        } else if (fileName.contains("Transcripts")) {
            clsName = "Transcript";
        } else {
            throw new RuntimeException("Could not determine class from filename: "
                                       + fileName);
        }

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length <= 1 || line[0].startsWith("#")) {
                continue;
            }

            if (line.length < 2) {
                throw new RuntimeException("Line does not have enough elements: "
                                           + Arrays.asList(line));
            }
            String primaryIdentifier = line[0];
            List<String> ensIds = new ArrayList<String>(Arrays.asList(line[1].split(" ")));
            String secondaryIdentifier = ensIds.get(0);
            ensIds.remove(0);

            Item feature = createItem(clsName);
            if (StringUtils.isNotEmpty(secondaryIdentifier)
                && !seenEnsIds.contains(secondaryIdentifier)) {
                feature.setAttribute("secondaryIdentifier", secondaryIdentifier);
                seenEnsIds.add(secondaryIdentifier);
            }
            if (StringUtils.isNotEmpty(primaryIdentifier)) {
                feature.setAttribute("primaryIdentifier", primaryIdentifier);
            }
            feature.setReference("organism", organismRefId);
            store(feature);
            String refId = feature.getIdentifier();
            // TODO maybe these should be xrefs?
            // create addidtional synonyms for other ensembl ids
            for (String ensId : ensIds) {
                createSynonym(refId, ensId, true);
            }
        }
    }
}
