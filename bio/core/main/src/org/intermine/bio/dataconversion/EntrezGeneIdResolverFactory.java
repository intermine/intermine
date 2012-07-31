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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.PropertiesUtil;

/**
 * ID resolver for Entrez genes.
 *
 * @author Richard Smith
 */
public class EntrezGeneIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(EntrezGeneIdResolverFactory.class);
    private final String clsName = "gene";
    private final String propName = "resolver.entrez.file"; // set in .intermine/MINE.properties
    private final String taxonId = "9606";

    /**
     * Build an IdResolver from Entrez Gene gene_info file
     * @return an IdResolver for Entrez Gene
     */
    @Override
    protected IdResolver createIdResolver() {
        Properties props = PropertiesUtil.getProperties();
        String fileName = props.getProperty(propName);

        if (StringUtils.isBlank(fileName)) {
            String message = "Entrez gene resolver has no file name specified, set " + propName
                + " to the location of the gene_info file.";
            throw new IllegalArgumentException(message);
        }

        IdResolver resolver;
        BufferedReader reader;
        try {
            FileReader fr = new FileReader(new File(fileName));
            reader = new BufferedReader(fr);
            resolver = createFromFile(reader);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Failed to open gene_info file: "
                    + fileName, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading from gene_info file: "
                    + fileName, e);
        }

        return resolver;
    }

    private IdResolver createFromFile(BufferedReader reader) throws IOException {
        IdResolver resolver = new IdResolver(clsName);

        NcbiGeneInfoParser parser = new NcbiGeneInfoParser(reader);
        Map<String, Set<GeneInfoRecord>> records = parser.getGeneInfoRecords();
        if (records == null) {
            throw new IllegalArgumentException("Failed to read any records from gene_info file.");
        }
        if (!records.containsKey(taxonId)) {
            throw new IllegalArgumentException("No records in gene_info file for taxon: "
                   + taxonId);
        }
        for (GeneInfoRecord record : records.get(taxonId)) {
            resolver.addMainIds(taxonId, record.entrez, lowerCase(record.getMainIds()));
            resolver.addSynonyms(taxonId, record.entrez, lowerCase(record.ensemblIds));
            resolver.addSynonyms(taxonId, record.entrez, lowerCase(record.synonyms));
        }
        return resolver;
    }

    private Set<String> lowerCase(Set<String> input) {
        Set<String> lower = new HashSet<String>();
        for (String s : input) {
            lower.add(s.toLowerCase());
        }
        return lower;
    }
}
