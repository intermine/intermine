package org.intermine.bio.postprocess;

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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.model.bio.CDS;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.StringUtil;

/**
 * Task to read a file mapping cds identifiers to UniProt accession numbers and create a protein
 * reference in the CDSs.
 *
 * @author Kim Rutherford
 */

public class FlyBaseUniProtRefsTask extends Task
{
    private static final Logger LOG = Logger.getLogger(FlyBaseUniProtRefsTask.class);

    protected ObjectStoreWriter osw;
    protected ObjectStore os;
    protected String objectStoreWriter = null;
    protected File linkFile = null;

    /**
     * Sets the value of objectStoreWriter
     *
     * @param objectStoreWriter an objectStoreWriter alias for operations that require one
     */
    public void setObjectStoreWriter(String objectStoreWriter) {
        this.objectStoreWriter = objectStoreWriter;
    }

    private ObjectStoreWriter getObjectStoreWriter() throws ObjectStoreException {
        if (objectStoreWriter == null) {
            throw new BuildException("objectStoreWriter attribute is not set");
        }
        if (osw == null) {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(objectStoreWriter);
        }
        return osw;
    }

    private ObjectStore getObjectStore() throws ObjectStoreException {
        return getObjectStoreWriter().getObjectStore();
    }

    /**
     * Set the linkFile attribute.
     * @param linkFile the File to read in execute()
     */
    public void setLinkFile(File linkFile) {
        this.linkFile = linkFile;
    }

    /**
     * Use the linkFile attribute (which is a file containing FlyBase Translation <-> UniProt
     * accession numbers) to link Translation objects to Protein objects in the ObjectStoreWriter.
     * Sets the protein reference in the CDS objects.
     * @throws BuildException if there is a problem while executing
     */
    public void execute()
        throws BuildException {

        try {
            getObjectStoreWriter().beginTransaction();

            // a Map from CDS identifier (in FlyMine) to Protein
            Map linkMap = new HashMap();

            BufferedReader br = new BufferedReader(new FileReader (linkFile));
            String line = null;

            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }

                String[] bits = StringUtil.split(line, "\t");

                if (bits.length != 2) {
                    throw new BuildException("the input file \"" + linkFile + "\" contains a "
                                             + "corrupt line (needs two fields): " + line);
                }

                String cdsIdentifier = bits[0].trim() + "_CDS";
                String uniprotAccNumber = bits[1].trim();

                if (uniprotAccNumber.length() == 0) {
                    continue;
                }

                linkMap.put(cdsIdentifier, uniprotAccNumber);
            }

            // map from acc # to Protein
            Map<String, Protein> uniprotAccMap = new HashMap();

            // map from translation identifier to Translation
            Map<?, ?> cdsIdMap = new HashMap();

            Query uniprotQuery = new Query();

            QueryClass proteinQc = new QueryClass(Protein.class);
            uniprotQuery.addFrom(proteinQc);
            uniprotQuery.addToSelect(proteinQc);

            BagConstraint proteinBc =
                new BagConstraint(new QueryField(proteinQc, "primaryAccession"),
                                  ConstraintOp.IN,
                                  new HashSet(linkMap.values()));

            uniprotQuery.setConstraint(proteinBc);

            Results uniprotResults = getObjectStore().execute(uniprotQuery);
            Iterator uniprotIter = uniprotResults.iterator();
            while (uniprotIter.hasNext()) {
                ResultsRow row = (ResultsRow) uniprotIter.next();
                Protein protein = (Protein) row.get(0);
                uniprotAccMap.put(protein.getPrimaryAccession(), protein);
            }

            Query cdsQuery = new Query();

            QueryClass cdsQc = new QueryClass(CDS.class);
            cdsQuery.addFrom(cdsQc);
            cdsQuery.addToSelect(cdsQc);

            BagConstraint cdsBc = new BagConstraint(new QueryField(cdsQc, "primaryIdentifier"),
                                                    ConstraintOp.IN,
                                                    linkMap.keySet());

            cdsQuery.setConstraint(cdsBc);

            Results cdsResults = getObjectStore().execute(cdsQuery);
            Iterator cdsIter = cdsResults.iterator();

            while (cdsIter.hasNext()) {
                ResultsRow row = (ResultsRow) cdsIter.next();
                CDS cds = (CDS) row.get(0);
                String cdsId = cds.getPrimaryIdentifier();

                if (linkMap.get(cdsId) == null) {
                    throw new RuntimeException("internal error: identifier missing from map");
                }

                String uniprotAcc = (String) linkMap.get(cdsId);
                Protein protein = (Protein) uniprotAccMap.get(uniprotAcc);

                if (protein == null) {
                    LOG.error("uniprot accession number not found: " + uniprotAcc);
                }

                CDS clonedCds = (CDS) PostProcessUtil.cloneInterMineObject(cds);
                clonedCds.setProtein(protein);
                getObjectStoreWriter().store(clonedCds);
            }

            getObjectStoreWriter().commitTransaction();


        } catch (IllegalAccessException e) {
            throw new BuildException("exception while clone an object", e);
        } catch (ObjectStoreException e) {
            throw new BuildException("exception while querying from ObjectStore", e);
        } catch (IOException e) {
            throw new BuildException("error while reading: " + linkFile, e);
        } finally {
            try {
                getObjectStoreWriter().close();
            } catch (ObjectStoreException e) {
                LOG.error("failed to close() ObjectStoreWriter", e);
            }
            osw = null;
        }
    }
}
