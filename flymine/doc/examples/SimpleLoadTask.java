package org.flymine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;

import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinFeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * An example Task that creates ProteinFeature objects using data from a flat file.
 * @author Kim Rutherford
 */

public class SimpleLoadTask extends Task
{
    private String oswAlias;
    private List fileSets = new ArrayList();
    private ObjectStoreWriter osw;

    /**
     * The ObjectStoreWriter alias to use when querying and creating objects.
     * @param oswAlias the ObjectStoreWriter alias
     */
    public void setOswAlias(String oswAlias) {
        this.oswAlias = oswAlias;
    }

    /**
     * Return the oswAlias set by setOswAlias()
     * @return the object store alias
     */
    public String getOswAlias() {
        return oswAlias;
    }

    /**
     * Return the set of files that should be read from.
     * @return the List of FileSets
     */
    public List getFileSets() {
        return fileSets;
    }

    /**
     * Return the ObjectStoreWriter given by oswAlias.
     * @return the ObjectStoreWriter
     * @throws BuildException if there is an error while processing
     */
    protected ObjectStoreWriter getObjectStoreWriter() throws BuildException {
        if (oswAlias == null) {
            throw new BuildException("oswAlias attribute is not set");
        }
        if (osw == null) {
            try {
                osw = ObjectStoreWriterFactory.getObjectStoreWriter(oswAlias);
            } catch (ObjectStoreException e) {
                throw new BuildException("cannot get ObjectStoreWriter for: " + oswAlias, e);
            }
        }
        return osw;
    }

    /**
     * Add a FileSet to read from
     * @param fileSet the FileSet
     */
    public void addFileSet(FileSet fileSet) {
        fileSets.add(fileSet);
    }


    /**
     * Load data into the ObjectStoire given by oswAlias.
     * @throws BuildException if an ObjectStore method fails
     */
    public void execute() throws BuildException {
        if (getOswAlias() == null) {
            throw new BuildException("oswAlias not set");
        }

        try {
            getObjectStoreWriter().beginTransaction();
        } catch (ObjectStoreException e) {
            throw new BuildException("cannot begin a transaction", e);
        }

        Iterator fileSetIter = getFileSets().iterator();

        while (fileSetIter.hasNext()) {
            FileSet fileSet = (FileSet) fileSetIter.next();

            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                File file = new File(ds.getBasedir(), files[i]);
                processFile(file);
            }
        }

        try {
            getObjectStoreWriter().commitTransaction();
        } catch (ObjectStoreException e) {
            throw new BuildException("cannot begin a transaction", e);
        }
    }

    private void processFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Set proteinFeatureSingleton = Collections.singleton(ProteinFeature.class);

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                
                String bits[] = StringUtil.split(line, " ");
                String proteinName = bits[0];

                Set proteins = getProteinsForName(proteinName);
                
                String proteinStart = bits[1];
                String proteinEnd = bits[2];
                String proteinFeatureIdentifier = bits[3];
                String proteinFeatureName = bits[4];
                ProteinFeature proteinFeature =
                    (ProteinFeature) DynamicUtil.createObject(proteinFeatureSingleton);

                proteinFeature.setIdentifier(proteinFeatureIdentifier);
                proteinFeature.setName(proteinFeatureName);

                proteinFeature.setProteins(proteins);

                Iterator proteinIter = proteins.iterator();

                while (proteinIter.hasNext()) {
                    Protein thisProtein = (Protein) proteinIter.next();
                    thisProtein.addProteinFeatures(proteinFeature);
                    osw.store(thisProtein);

                    Location featureLocation =
                        (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
                    try {
                        featureLocation.setStart(new Integer(Integer.parseInt(proteinStart)));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("failed to parse start position: " 
                                                   + proteinStart, e);
                    }
                    try {
                        featureLocation.setEnd(new Integer(Integer.parseInt(proteinEnd)));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("failed to parse end position: "
                                                   + proteinEnd, e);
                    }
                
                    featureLocation.setSubject(proteinFeature);
                    featureLocation.setObject(thisProtein);
                    osw.store(featureLocation);
                }

                osw.store(proteinFeature);
            }
        } catch (FileNotFoundException e) {
            throw new BuildException("problem reading file - file not found: " + file, e);
        } catch (IOException e) {
            throw new BuildException("problem reading file - I/O exception for:" + file, e);
        } catch (ObjectStoreException e) {
            throw new BuildException("problem storing object", e);
        }
    }

    private Set getProteinsForName(String proteinIdentifier) {
        Query q = new Query();
        QueryClass qc = new QueryClass(Protein.class);
        q.addFrom(qc);
        q.addToSelect(qc);

        QueryValue qv = new QueryValue(proteinIdentifier);
        QueryField qf = new QueryField(qc, "identifier");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, qv);
        q.setConstraint(sc);
        
        Set returnList = new HashSet();
        try {
            Results res = osw.getObjectStore().execute(q);
            Iterator resIter = res.iterator();
            while (resIter.hasNext()) {
                ResultsRow rr = (ResultsRow) resIter.next();
                returnList.add(rr.get(0));
            }
        } catch (ObjectStoreException e) {
            throw new BuildException("cannot query Protein objects from database", e);
        }
        return returnList;
    }
}
