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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

import org.intermine.model.bio.DataSource;


/**
 * Class to fill in organism information using UniProt.
 * @author Julie Sullivan
 */
public class DataSourceUpdater extends Task
{
    protected static final Logger LOG = Logger.getLogger(DataSourceUpdater.class);
    private String osAlias = null;
    private String outputFile = null;
    private String dataSourceFile = null;
    private Set<String> dataSourceNames = new HashSet<String>();

    /**
     * Set the ObjectStore alias.
     * @param osAlias The ObjectStore alias
     */
    public void setOsAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Set the output file name
     * @param outputFile The output file name
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Set the file name for the data file from UniProt
     * @param dataSourceFile the name of the data file to process (from uniprot)
     */
    public void setDataSourceFile(String dataSourceFile) {
        this.dataSourceFile = dataSourceFile;
    }

    /**
     * For each data set in the objectstore, retreive it's details from UniProt data file
     * @throws BuildException if an error occurs
     */
    @Override
    public void execute() {
        // Needed so that STAX can find it's implementation classes
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        if (osAlias == null) {
            throw new BuildException("osAlias attribute is not set");
        }
        if (outputFile == null) {
            throw new BuildException("outputFile attribute is not set");
        }
        if (dataSourceFile == null) {
            throw new BuildException("dataSourceFile attribute is not set");
        }

        LOG.info("Starting DataSourceUpdater");

        Writer writer = null;

        try {
            writer = new FileWriter(outputFile);
            ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);
            // get the data sources already in the database
            dataSourceNames = getDataSources(os);

            // parse the uniprot data source file
            Map<String, DataSourceHolder> allDataSources
                = parseDataFile(new FileReader(dataSourceFile));
            ItemFactory itemFactory = new ItemFactory(os.getModel(), "-1_");
            writer.write(FullRenderer.getHeader() + "\n");

            // write the relevant data sources to file
            for (DataSourceHolder holder : allDataSources.values()) {
                Item datasource = itemFactory.makeItemForClass("DataSource");
                datasource.setAttribute("name", holder.name);
                if (holder.descr != null) {
                    datasource.setAttribute("description", holder.descr);
                }
                if (holder.url != null) {
                    datasource.setAttribute("url", holder.url);
                }
                if (holder.pubMedId != null) {
                    Item publication = itemFactory.makeItemForClass("Publication");
                    publication.setAttribute("pubMedId", holder.pubMedId);
                    writer.write(FullRenderer.render(publication));

                    datasource.addToCollection("publications", publication);
                }
                writer.write(FullRenderer.render(datasource));
            }
            writer.write(FullRenderer.getFooter() + "\n");
        } catch (Exception e) {
            throw new BuildException("exception while retrieving data sets", e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

//    AC    : DB-0026
//    Abbrev: FlyBase
//    Name  : Drosophila genome database
//    Ref   : Nucleic Acids Res. 35:D486-D491(2007); PubMed=17099233; DOI=10.1093/nar/gkl827;
//    LinkTp: Explicit
//    Server: http://flybase.org/
//    Db_URL: http://flybase.org/reports/%s.html
//    Note  : Obsolete abbreviation: DMAP
//    Cat   : Organism-specific database

    private Map<String, DataSourceHolder> parseDataFile(Reader reader) throws IOException {
        Map<String, DataSourceHolder> datasources = new HashMap<String, DataSourceHolder>();
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        DataSourceHolder datasource = null;
        int labelLength = 8;
        while ((line = br.readLine()) != null) {
            if (line.length() < 8) {
                continue;
            }
            String value = line.substring(labelLength).trim();
            if (line.startsWith("Abbrev: ")) {
                if (dataSourceNames.contains(value)) {
                    datasource = new DataSourceHolder(value);
                    datasources.put(value, datasource);
                } else {
                    datasource = null;
                }
            } else if (datasource != null) {
                if (line.startsWith("Name")) {
                    datasource.descr = value;
                } else if (line.startsWith("Server: ")) {
                    datasource.url = value;
                } else if (line.startsWith("Ref   : ")) {
                    String pubMedId = parseRefString(value);
                    if (pubMedId != null) {
                        datasource.pubMedId = pubMedId;
                    }
                }
            }
        }
        return datasources;
    }

    private static String parseRefString(String refs) {
        for (String ref : refs.split("\\;")) {
            String[] refStrings = ref.split("=");
            if (refStrings.length == 2) {
                String name = refStrings[0].trim();
                String id = refStrings[1].trim();
                if ("PubMed".equals(name)) {
                    return id;
                }
            }
        }
        return null;
    }

    /**
     * get all the data sources from the database
     * @param os objectstore
     * @return set of strings representing datasources
     */
    protected Set<String> getDataSources(ObjectStore os) {
        Query q = new Query();
        QueryClass qc = new QueryClass(DataSource.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        List<?> results = os.executeSingleton(q);
        Set<String> names = new HashSet<String>();
        Iterator<?> resIter = results.iterator();
        while (resIter.hasNext()) {
            DataSource datasource = (DataSource) resIter.next();
            names.add(datasource.getName());
        }
        return names;
    }

    private class DataSourceHolder
    {
        protected String name;
        protected String descr;
        protected String pubMedId;
        protected String url;

        public DataSourceHolder(String name) {
            this.name = name;
        }
    }
}
