package org.flymine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

import org.flymine.model.genomic.Organism;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * A base class for Tasks that read from files and use the information to set fields in a particular
 * class.
 *
 * @author Kim Rutherford
 */

public class FileReadTask extends Task
{
    private String organismAbbreviation;
    private String className;
    private String keyFieldName;
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
     * Set the class name of the objects to operate on.
     * @param className the class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Return the className set by setClassName()
     * @return the class name
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Return the sets of file that should be read from.
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
     * return a Map from identifier to InterMine ID for all the objects of type className and
     * organism given by organismAbbreviation.
     * @param os The ObjectStore to use when creating the ID Map
     * @return the ID Map
     */
    public Map getIdMap(ObjectStore os) {
        return buildIdMap(os);
    }

    /**
     * Set the organism abbreviation.  Only objects that have a reference to this organism will have
     * their sequences set.
     * @param organismAbbreviation the organism of the objects to set
     */
    public void setOrganismAbbreviation(String organismAbbreviation) {
        this.organismAbbreviation = organismAbbreviation;
    }
    
    /**
     * Return the organismAbbreviation set by setOrganismAbbreviation()
     * @return the organism abbreviation
     */
    public String getOrganismAbbreviation() {
        return organismAbbreviation;        
    }
    
    /**
     * Build a Map from an identifier to InterMine ID for all the objects of type className and
     * organism given by organismAbbreviation.  The identifier field to use as the key is set
     * by setKeyFieldName().
     * @param keyFieldName the field in the clasto use 
     * @param os the ObjectStore to read the objects from
     * @throws BuildException 
     */
    private Map buildIdMap(ObjectStore os) throws BuildException {
        Map idMap = new HashMap();
        
        Query q = new Query();
        q.setDistinct(true);

        Class c;
        try {
            if (className.indexOf(".") == -1) {
                c = Class.forName(os.getModel().getPackageName() + "." + className);
            } else {
                c = Class.forName(className);
            }
        } catch (ClassNotFoundException e) {
            throw new BuildException("cannot find class for: " + className);
        }
        QueryClass qcObj = new QueryClass(c);
        QueryField qfObjIdentifier = new QueryField(qcObj, keyFieldName);
        q.addFrom(qcObj);
        q.addToSelect(qfObjIdentifier);
        q.addToSelect(qcObj);

        QueryClass qcOrg = new QueryClass(Organism.class);

        QueryObjectReference ref = new QueryObjectReference(qcObj, "organism");
        ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcOrg);

        SimpleConstraint sc = new SimpleConstraint(new QueryField(qcOrg, "abbreviation"),
                                                   ConstraintOp.EQUALS,
                                                   new QueryValue(organismAbbreviation));
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc);
        cs.addConstraint(sc);

        q.setConstraint(cs);

        q.addFrom(qcOrg);

        Results res = new Results(q, os, os.getSequence());

        res.setBatchSize(10000);

        Iterator iter = res.iterator();

        while (iter.hasNext()) {
            List row = (List) iter.next();

            // we queried for the InterMineObject but we just store the ID
            // we hope that the ObjectStore will cache the objects
            idMap.put(row.get(0), ((InterMineObject) row.get(1)).getId());
        }
        
        return idMap;
    }

    /**
     * Set the he name of the field (in the class specified by className) to use in buildIdMap(). 
     * @param keyFieldName the key field name
     */
    public void setKeyFieldName(String keyFieldName) {
        this.keyFieldName = keyFieldName;
    }
    
    /**
     * Return the name of the field (in the class specified by className) to use in buildIdMap(). 
     * @return the key field name
     */
    public String getKeyFieldName() {
        return keyFieldName;
    }
}
