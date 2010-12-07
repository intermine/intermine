package org.intermine.bio.dataconversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;

public class EntrezGeneIdResolverFactory {
    protected static final Logger LOG = Logger.getLogger(HgncIdResolverFactory.class);
    private final String clsName = "gene";
    private final String propName = "resolver.hgnc.file";
    private final String taxonId = "9606";

    /**
     * Build an IdResolver from Entrez Gene gene_info file
     * @return an IdResolver for Entrex Gene
     */
    protected IdResolver createIdResolver() {
        Properties props = PropertiesUtil.getProperties();
        String fileName = props.getProperty(propName);

        if (StringUtils.isBlank(fileName)) {
            String message = "HGNC resolver has no file name specified, set " + propName
                + " to the file location.";
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

        // Creating the resolver is fast so we don't really need to cache
//        String cacheFileName = "build/hgnc_resolver.cache";
//        try {
//            resolver.writeToFile(new File(cacheFileName));
//            System.out. println("Written cache file: " + cacheFileName);
//        } catch (IOException e) {
//            throw new IllegalArgumentException("Error writing HGNC resolver cache file: "
//                    + cacheFileName, e);
//        }

        return resolver;
    }

    private IdResolver createFromFile(BufferedReader reader) throws IOException {
        IdResolver resolver = new IdResolver(clsName);

        // HGNC ID | Approved Symbol | Approved Name | Status | Previous Symbols | Aliases
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String symbol = line[1];

            String taxonId = line[0];
            String entrez = line[1];
            String defaultSymbol = line[2];
            String synonyms = line[4];
            String xrefs = line[5];
            String mapLocation = line[7];
            String defaultName = line[8];
            String officialSymbol = line[10];
            String officialName = line[11];
            
            resolver.addMainIds(taxonId, entrez, Collections.singleton(symbol));

            if (!"-".equals(officialSymbol)) {
                //resolver.addMainIds(taxonId, , Collections.singleton(symbol));
            }
            addSynonyms(resolver, symbol, line[4]);
            addSynonyms(resolver, symbol, line[5]);
        }
        return resolver;
    }

    private void addSynonyms(IdResolver resolver, String symbol, String ids) {
        if (!StringUtils.isBlank(ids)) {
            Set<String> synonyms = new HashSet<String>();
            for (String alias : ids.split(",")) {
                synonyms.add(alias.trim());
            }
            resolver.addSynonyms(taxonId, symbol, synonyms);
        }
    }
}
