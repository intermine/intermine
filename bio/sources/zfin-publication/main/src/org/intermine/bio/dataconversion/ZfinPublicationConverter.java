package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.dataconversion.BioFileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * Parser for ZFIN pulication data
 * @author Fengyuan Hu
 */
public class ZfinPublicationConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "ZFIN Bibliography";
    private static final String DATA_SOURCE_NAME = "ZFIN";
    private static final String NONE_STRING = "none";

    protected static final Logger LOG = Logger
    .getLogger(ZfinPublicationConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ZfinPublicationConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        @SuppressWarnings("rawtypes")
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String zfinPubId = line[0];
            String pubmedId = line[1];

            List<String> authors;

            if (line[2].contains("., ")) {
                authors = Arrays.asList(StringUtil.split(line[2], "., "));
            } else {
                authors = Arrays.asList(StringUtil.split(line[2], " and "));
            }

            String title = line[3];
            String journal = line[4];
            String year = line[5];
            String volume = line[6];
            String pages = line[7];

            Item publication = createItem("ZfinBibliography");

            if (!StringUtils.isEmpty(zfinPubId)) {
                publication.setAttribute("zfinPublicationId", zfinPubId);
            }

            if (!StringUtils.isEmpty(pubmedId) && !pubmedId.equals(NONE_STRING)) {
                publication.setAttribute("pubMedId", pubmedId);
            }

            if (!StringUtils.isEmpty(title)) {
                publication.setAttribute("title", title);
            }

            if (!StringUtils.isEmpty(journal)) {
                publication.setAttribute("journal", journal);
            }

            if (!StringUtils.isEmpty(year)) {
                publication.setAttribute("year", year);
            }

            if (!StringUtils.isEmpty(volume)) {
                publication.setAttribute("volume", volume);
            }

            if (!StringUtils.isEmpty(pages)) {
                publication.setAttribute("pages", pages);
            }

            for (String authorString : authors) {
                Item author = createItem("Author");
                authorString = authorString.trim();

                if (!authorString.endsWith(".")) {
                    authorString = authorString + ".";
                }

                if (authorString.startsWith("and ")) {
                    authorString = authorString.split("and ")[1];
                }

                if (authorString.endsWith("Jr.") || authorString.endsWith("3rd.")) {
                    authorString = authorString.substring(0, authorString.length() - 1);
                }

                author.setAttribute("name", authorString);
                store(author);

                publication.addToCollection("authors", author);

                if (!publication.hasAttribute("firstAuthor")) {
                    publication.setAttribute("firstAuthor", authorString);
                }
            }

            store(publication);
        }
    }
}
