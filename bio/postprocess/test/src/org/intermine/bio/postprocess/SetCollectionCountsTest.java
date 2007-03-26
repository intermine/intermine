package org.intermine.bio.postprocess;

import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.dummy.ObjectStoreWriterDummyImpl;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Transcript;

import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.Assert;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Tests for the SetCollectionCounts class.
 *
 * @author Kim Rutherford
 */

public class SetCollectionCountsTest extends TestCase
{
    public void testSetCountFields() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setModel(Model.getInstanceByName("genomic"));
        ObjectStoreWriterDummyImpl osw = new ObjectStoreWriterDummyImpl(os);

        Transcript transcript1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        transcript1.setId(new Integer(100));
        Transcript transcript2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        transcript2.setId(new Integer(200));
        Exon exon1 =
            (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon1.setId(new Integer(300));
        Exon exon2 =
            (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon2.setId(new Integer(400));
        Exon exon3 =
            (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon3.setId(new Integer(500));
        
        transcript1.addExons(exon1);
        transcript2.addExons(exon2);
        transcript2.addExons(exon3);
        ResultsRow rr1 = new ResultsRow();
        rr1.add(transcript1);
        os.addRow(rr1);
        ResultsRow rr2 = new ResultsRow();
        rr2.add(transcript2);
        os.addRow(rr2);

        os.setResultsSize(2);

        SetCollectionCounts setCounts = new SetCollectionCounts(osw);

        setCounts.setCollectionCountField(Transcript.class, "exons", "exonCount");

        Map storedObjects = osw.getStoredObjects();

        Assert.assertEquals(2, storedObjects.size());

        Transcript resTranscript1 = (Transcript) storedObjects.get(new Integer(100));
        Transcript resTranscript2 = (Transcript) storedObjects.get(new Integer(200));

        Assert.assertEquals(1, resTranscript1.getExonCount().intValue());
        Assert.assertEquals(2, resTranscript2.getExonCount().intValue());
    }
}
