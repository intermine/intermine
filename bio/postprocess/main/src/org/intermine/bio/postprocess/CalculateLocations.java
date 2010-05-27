package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;


/**
 * Calculate additional mappings between annotation after loading into genomic ObjectStore.
 * Currently designed to cope with situation after loading ensembl, may need to change
 * as other annotation is loaded.  New Locations (and updated BioEntities) are stored
 * back in originating ObjectStore.
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class CalculateLocations
{
    private static final Logger LOG = Logger.getLogger(CalculateLocations.class);

    protected ObjectStoreWriter osw;
    protected ObjectStore os;



    /**
     * Create a new CalculateLocations object from an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public CalculateLocations(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }


    /**
     * Create OverlapRelation objects for all overlapping SequenceFeatures by querying
     * objects that are located on chromosomes and overlap.
     * @param classNamesToIgnore a List of the names of those classes that should be ignored when
     * searching for overlaps.  Sub classes to these classes are ignored too. In addition, an
     * entry can be of the form class=class, which specifies that the particular combination should
     * be ignored. Hence an entry of the form class is equivalent to class=InterMineObject
     * @param ignoreSelfMatches if true, don't create OverlapRelations between two objects of the
     * same class
     * @throws Exception if anything goes wrong
     */
    public void createOverlapRelations(List classNamesToIgnore, boolean ignoreSelfMatches)
    throws Exception {
        // nothing
    }


}

