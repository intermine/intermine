package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference; 
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Transcript;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Intron;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Synonym;

/**
 * Methods for creating feature for introns.
 * @author Wenyan Ji
 */
public class IntronUtil
{
    private static final Logger LOG = Logger.getLogger(IntronUtil.class);
    
    private ObjectStoreWriter osw = null;
    private ObjectStore os;
    private DataSet dataSet;
    private DataSource dataSource;

    protected Map intronMap = new HashMap();



    /**
     * Create a new IntronUtil object that will operate on the given ObjectStoreWriter.
     * @param osw the ObjectStoreWriter to use when creating/changing objects
     */
    public IntronUtil(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
        dataSource = (DataSource) DynamicUtil.createObject(Collections.singleton(DataSource.class));
        dataSource.setName("FlyMine");
        try {
            dataSource = (DataSource) os.getObjectByExample(dataSource,
                                                            Collections.singleton("name"));
        } catch (ObjectStoreException e) {
            throw new RuntimeException("unable to fetch FlyMine DataSource object", e);
        }
    }

    /**
     * Create Intron objects
     * @throws ObjectStoreException if there is an ObjectStore problem
     * @throws IllegalAccessException if there is an ObjectStore problem
     */
    public void createIntronFeatures() 
        throws ObjectStoreException, IllegalAccessException {
        Set locationSet = new HashSet();
        Integer transcriptId = null;
   
        dataSet = (DataSet) DynamicUtil.createObject(Collections.singleton(DataSet.class));
        dataSet.setTitle("FlyMine introns");
        dataSet.setDescription("Introns created by FlyMine");
        dataSet.setVersion("" + new Date()); // current time and date
        dataSet.setUrl("http://www.flymine.org");
        dataSet.setDataSource(dataSource);

        int exonCounts;
        Query q = new Query();
        //q.setDistinct(false);
        QueryClass qc = new QueryClass(Transcript.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Results results = new Results(q, os, os.getSequence());
        results.setBatchSize(500);
        Iterator resultsIter = results.iterator();
        
        while (resultsIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resultsIter.next();            
            Transcript o = (Transcript) rr.get(0);
            exonCounts = Integer.parseInt((TypeUtil.getFieldValue(o, "exonCount")).toString());
            if (exonCounts > 1) {
                transcriptId = o.getId();
                locationSet = getLocationSet(os, transcriptId);
                Set intronSet = createIntronFeatures(locationSet, transcriptId);
            }
        }
  
        for (Iterator i = intronMap.keySet().iterator(); i.hasNext();) {
            String identifier = (String) i.next();
            Intron intron = (Intron) intronMap.get(identifier);
            LOG.info("intronMap " + identifier);
            osw.store(intron);
            osw.store(intron.getChromosomeLocation());
            osw.store((InterMineObject) intron.getSynonyms().iterator().next());
        }

        if (intronMap.size() > 1) {
            osw.store(dataSet);
        }

    }

  
    /**
     * Return a set of Intron objects that don't overlap the Locations
     * in the locationSet argument.  The caller must call ObjectStoreWriter.store() on the
     * Intron, its chromosomeLocation and the synonym in the synonyms collection.
     * @param locationSet a set of Locations for the exonss on a particular transcript
     * @param transcriptId the ID of the Transcript that the Locations refer to
     * @return a set of Intron objects
     * @throws ObjectStoreException if there is an ObjectStore problem
     */
    protected Set createIntronFeatures(Set locationSet, Integer transcriptId)
        throws ObjectStoreException {
        final Transcript transcript = (Transcript) os.getObjectById(transcriptId);
        final BitSet bs = new BitSet(transcript.getLength().intValue() + 1);
        
        if (locationSet.size() == 1) {
            return null;
        }
        Chromosome chr = transcript.getChromosome();
        
        Iterator locationIter = locationSet.iterator();

        while (locationIter.hasNext()) {
            Location location = (Location) locationIter.next();
            bs.set(location.getStart().intValue(), location.getEnd().intValue() + 1);
        }

        int prevEndPos = 0;

        while (prevEndPos != -1) {
        
            int nextIntronStart = bs.nextClearBit(prevEndPos + 1); 
            int intronEnd;
            int nextSetBit = bs.nextSetBit(nextIntronStart);
        
            if (nextSetBit == -1) {
                intronEnd = transcript.getLength().intValue();
            } else {
                intronEnd = nextSetBit - 1;
            }

            if (nextSetBit == -1
                || bs.nextClearBit(nextSetBit) > transcript.getLength().intValue()) {
                prevEndPos = -1;
            } else {
                prevEndPos = intronEnd;
            }

            int newLocStart = nextIntronStart;
            int newLocEnd = intronEnd;
        
            String identifier = "intron_chr" + chr.getIdentifier()
                + "_" + Integer.toString(newLocStart) + ".." + Integer.toString(newLocEnd);
            
            if (intronMap.get(identifier) == null) {
                
                Intron intron = (Intron)
                    DynamicUtil.createObject(Collections.singleton(Intron.class));
                Location location =
                    (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
                Synonym synonym =
                    (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));

                intron.setChromosome(chr);
                intron.setOrganism(chr.getOrganism());                
                intron.addEvidence(dataSet);
                intron.setIdentifier(identifier);

                location.setStart(new Integer(newLocStart));
                location.setEnd(new Integer(newLocEnd));
                location.setStrand(new Integer(1));
                location.setPhase(new Integer(0));
                location.setStartIsPartial(Boolean.FALSE);
                location.setEndIsPartial(Boolean.FALSE);
                location.setSubject(intron);
                location.setObject(transcript);
                location.addEvidence(dataSet);
                
                synonym.addEvidence(dataSet);
                synonym.setSource(dataSource);
                synonym.setSubject(intron);
                synonym.setType("identifier");
                synonym.setValue(intron.getIdentifier());

                intron.setChromosomeLocation(location);
                intron.addSynonyms(synonym);
                int length = location.getEnd().intValue() - location.getStart().intValue() + 1;
                intron.setLength(new Integer(length));
                intron.addTranscripts(transcript);
                                
                intronMap.put(identifier, intron);
            } else {
                Intron intron = (Intron) intronMap.get(identifier);
                intron.addTranscripts(transcript);
                intronMap.put(identifier, intron);
            }

        }
        
        Set intronSet = new HashSet();
        for (Iterator i = intronMap.keySet().iterator(); i.hasNext(); ) {
            intronSet.add(intronMap.get(i.next()));
        }
        return intronSet;
    }

    /**
     * @param os objectStore
     * @param transcriptId Integer
     * @return all the exons locationSet for the particular transcriptId
     */
    private Set getLocationSet(ObjectStore os, Integer transcriptId) {
        Set locationSet = new HashSet();
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        Query q = new Query();
        QueryClass qct = new QueryClass(Transcript.class);
        QueryField qf = new QueryField(qct, "id");
        SimpleConstraint sc1 = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                            new QueryValue(transcriptId));
        
        q.addFrom(qct);
        q.addToSelect(qf);   
        q.addToOrderBy(qf);
        cs.addConstraint(sc1);
        
        QueryCollectionReference ref1 = new QueryCollectionReference(qct, "exons");
        QueryClass qce = new QueryClass(Exon.class);
        q.addFrom(qce);
        q.addToSelect(qce);        
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qce);
        cs.addConstraint(cc1);
        
        QueryClass qcl = new QueryClass(Location.class);
        q.addFrom(qcl);
        q.addToSelect(qcl);
        QueryObjectReference ref2 = new QueryObjectReference(qcl, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qce);
        cs.addConstraint(cc2);
        
        q.setConstraint(cs);

        Results res = new Results(q, os, os.getSequence());
        Iterator iter = res.iterator();
        while (iter.hasNext()) {
            ResultsRow rr = (ResultsRow) iter.next();
            Integer id = (Integer) rr.get(0);
            Location loc = (Location) rr.get(2);
            locationSet.add(loc);
        }
        return locationSet;
    }

}
