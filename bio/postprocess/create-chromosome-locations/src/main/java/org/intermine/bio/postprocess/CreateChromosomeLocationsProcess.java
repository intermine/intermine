package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import org.intermine.bio.util.BioQueries;
import org.intermine.bio.util.PostProcessUtil;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;

/**
 * Calculate additional mappings between annotation after loading into genomic ObjectStore.
 * Currently designed to cope with situation after loading ensembl, may need to change
 * as other annotation is loaded.  New Locations (and updated BioEntities) are stored
 * back in originating ObjectStore.
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class CreateChromosomeLocationsProcess extends PostProcessor
{
    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public CreateChromosomeLocationsProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine.
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess()
            throws ObjectStoreException {
        Results results = BioQueries.findLocationAndObjects(osw, Chromosome.class,
                SequenceFeature.class, true, false, false, 10000);
        Iterator<?> resIter = results.iterator();

        osw.beginTransaction();

        // we need to check that there is only one location before setting chromosome[Location]
        // references.  If there are duplicates do nothing - this has happened for some affy
        // probes in FlyMine.
        Integer lastChrId = null;
        SequenceFeature lastFeature = null;
        boolean storeLastFeature = true;  // will get set to false if duplicate locations seen
        Location lastLoc = null;

        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();

            Integer chrId = (Integer) rr.get(0);
            SequenceFeature lsf = (SequenceFeature) rr.get(1);
            Location locOnChr = (Location) rr.get(2);

            if (lastFeature != null && !lsf.getId().equals(lastFeature.getId())) {
                // not a duplicated so we can set references for last feature
                if (storeLastFeature) {
                    try {
                        setChromosomeReferencesAndStore(lastFeature, lastLoc, lastChrId);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Error storing chromosome reference:" + e);
                    }
                }
                storeLastFeature = true;
            } else if (lastFeature != null) {
                storeLastFeature = false;
            }

            lastFeature = lsf;
            lastChrId = chrId;
            lastLoc = locOnChr;
        }

        // make sure final feature gets stored
        if (storeLastFeature && lastFeature != null) {
            try {
                setChromosomeReferencesAndStore(lastFeature, lastLoc, lastChrId);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error storing chromosome reference:" + e);
            }
        }

        osw.commitTransaction();
    }

    private void setChromosomeReferencesAndStore(SequenceFeature lsf, Location loc,
            Integer chrId) throws ObjectStoreException, IllegalAccessException  {
        SequenceFeature lsfClone = PostProcessUtil.cloneInterMineObject(lsf);

        lsfClone.setChromosomeLocation(loc);
        if (loc.getStart() != null && loc.getEnd() != null) {
            int end = loc.getEnd().intValue();
            int start = loc.getStart().intValue();
            // only set length if it isn't already set to stop eg. mRNA lengths getting broken.
            // an alternative is to set according to type of feature.
            if (lsfClone.getLength() == null) {
                int length = Math.abs(end - start) + 1;
                lsfClone.setLength(new Integer(length));
            }
        }
        lsfClone.proxyChromosome(new ProxyReference(osw, chrId, Chromosome.class));

        osw.store(lsfClone);
    }
}
