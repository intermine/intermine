package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * This parser is use for parsing protein domain assignments and
 * protein domain locations in protein2ipr.dat form InterPro. <br/>
 * The file could be found at: <br/>
 * ftp://ftp.ebi.ac.uk/pub/databases/interpro/Current/protein2ipr.dat.gz
 * <p>
 * Because the file includes proteins in all species,
 * the parser will only load the proteins of the specified species which are already loaded into
 *  the mine.
 * Thus, better to run after the unirpot data source.
 * In addition, protein domain information are loaded by the interpro data source.
 * (both are kindly shared by InterMine.) </p>
 *
 * @author chenyian
 */
public class Protein2iprConverter extends BioFileConverter
{

    private static final Logger LOG = Logger.getLogger(Protein2iprConverter.class);
    private static final String DATASET_TITLE = "InterPro data set";
    private static final String DATA_SOURCE_NAME = "InterPro";

    private Collection<Integer> taxonIds = new ArrayList<Integer>();

    private Set<String> proteinIds = new HashSet<String>();
    private Set<MultiKey> xrefs = new HashSet<MultiKey>();
    private Map<String, String> proteinMap = new HashMap<String, String>();
    private Map<String, String> proteinDomainMap = new HashMap<String, String>();

    /**
     * @param taxonIds set valid taxonIds to process
     */
    public void setProtein2iprOrganisms(String taxonIds) {
        String[] taxonStringIds = StringUtils.split(taxonIds, " ");
        for (String string : taxonStringIds) {
            this.taxonIds.add(Integer.valueOf(string));
        }
        LOG.info("Setting list of organisms to " + this.taxonIds);
    }

    /**
     * Set the ObjectStore alias.
     * @param osAlias The ObjectStore alias
     */
    public void setOsAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Constructor
     *
     * @param writer
     *            the ItemWriter used to handle the resultant items
     * @param model
     *            the Model
     */
    public Protein2iprConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        if (osAlias == null) {
            throw new BuildException("osAlias attribute is not set");
        }

        getProteinIds();
        LOG.info("Found " + proteinIds.size() + " protein ids.");

        Iterator<String[]> iterator = FormattedTextParser.parseTabDelimitedReader(reader);
        int count = 0;
        int skipped = 0;
        while (iterator.hasNext()) {
            String[] cols = iterator.next();
            if (proteinIds.contains(cols[0])) {

                String proteinAccession = cols[0];
                String interproIdentifier = cols[1];
                String proteinDomainIdentifier = cols[3];
                String start = cols[4];
                String end = cols[5];

                String proteinRefId = getProtein(proteinAccession);
                String proteinDomainRefId = getProteinDomain(interproIdentifier);
                Item proteinDomainRegion = createItem("ProteinDomainRegion");

                proteinDomainRegion.setAttribute("identifier", proteinDomainIdentifier);
                proteinDomainRegion.setAttribute("database", getSource(proteinDomainIdentifier));
                proteinDomainRegion.setAttribute("start", start);
                proteinDomainRegion.setAttribute("end", end);
                proteinDomainRegion.setReference("protein", proteinRefId);
                proteinDomainRegion.setReference("proteinDomain", proteinDomainRefId);
                store(proteinDomainRegion);
                count++;
            } else {
                skipped++;
            }
        }

        LOG.info("Number of processed lines: " + count);
        LOG.info("Number of skipped lines: " + skipped);
    }

    private String getProtein(String identifier) throws ObjectStoreException {
        String refId = proteinMap.get(identifier);
        if (refId == null) {
            Item protein = createItem("Protein");
            protein.setAttribute("primaryAccession", identifier);
            store(protein);
            refId = protein.getIdentifier();
            proteinMap.put(identifier, refId);
        }
        return refId;
    }

    private String getProteinDomain(String identifier) throws ObjectStoreException {
        String refId = proteinDomainMap.get(identifier);
        if (refId == null) {
            Item proteinDomain = createItem("ProteinDomain");
            proteinDomain.setAttribute("primaryIdentifier", identifier);
            store(proteinDomain);
            refId = proteinDomain.getIdentifier();
            proteinDomainMap.put(identifier, refId);
        }
        return refId;
    }

    private String getSource(String dbId) {
        String dbName = null;
        if (dbId.startsWith("PF")) {
            dbName = "Pfam";
        } else if (dbId.startsWith("SM")) {
            dbName = "SMART";
        } else if (dbId.startsWith("SSF")) {
            dbName = "SUPERFAMILY";
        } else if (dbId.startsWith("PS")) {
            dbName = "PROSITE";
        } else if (dbId.startsWith("PR")) {
            dbName = "PRINTS";
        } else if (dbId.startsWith("PTHR")) {
            dbName = "PANTHER";
        } else if (dbId.startsWith("G3DSA")) {
            dbName = "Gene3D";
        } else if (dbId.startsWith("TIGR")) {
            dbName = "TIGRFAMs";
        } else if (dbId.startsWith("PD")) {
            dbName = "ProDom";
        } else if (dbId.startsWith("PIRSF")) {
            dbName = "PIRSF";
        } else if (dbId.startsWith("MF_")) {
            dbName = "HAMAP";
        } else {
            throw new RuntimeException("Unknown DB found. ID: " + dbId);
        }
        return dbName;
    }

    private String osAlias = null;

    @SuppressWarnings("unchecked")
    private void getProteinIds() throws Exception {
        Query q = new Query();
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);

        QueryField qfPrimaryAcc = new QueryField(qcProtein, "primaryAccession");
        QueryField qfOrganismTaxonId = new QueryField(qcOrganism, "taxonId");

        q.addFrom(qcProtein);
        q.addFrom(qcOrganism);
        q.addToSelect(qfPrimaryAcc);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // organism in our list
        cs.addConstraint(new BagConstraint(qfOrganismTaxonId, ConstraintOp.IN, taxonIds));

        // protein.organism = organism
        QueryObjectReference qor = new QueryObjectReference(qcProtein, "organism");
        cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, qcOrganism));

        q.setConstraint(cs);

        ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);

        Results results = os.execute(q);
        Iterator<Object> iterator = results.iterator();
        while (iterator.hasNext()) {
            ResultsRow<String> rr = (ResultsRow<String>) iterator.next();
            proteinIds.add(rr.get(0));
        }
    }

}
