package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.FileWriter;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.FullParser;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.flymine.io.gff3.GFF3Parser;
import org.flymine.io.gff3.GFF3Record;
import org.flymine.dataconversion.GFF3Converter;

public class ChadoGFF3RecordHandlerTest extends TestCase
{

    Model tgtModel;
    ChadoGFF3RecordHandler handler;
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    String seqClsName = "Chromosome";
    String orgAbbrev = "DM";
    String infoSourceTitle = "FlyBase";
    GFF3Converter converter;

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        handler = new ChadoGFF3RecordHandler(tgtModel);
        converter = new GFF3Converter(writer, seqClsName, orgAbbrev, infoSourceTitle, tgtModel,
                                      handler);
    }

    public void testParseFlyBaseId() throws Exception {
        List dbxrefs = new ArrayList(Arrays.asList(new String[] {"FlyBase:FBgn1234", "FlyBase:FBtr1234"}));
        assertEquals("FBgn1234", handler.parseFlyBaseId(dbxrefs, "FBgn"));
        assertEquals("FBtr1234", handler.parseFlyBaseId(dbxrefs, "FBtr"));
        dbxrefs.add("FlyBase:FBgn5678");
        try {
            handler.parseFlyBaseId(dbxrefs, "FBgn");
            fail("expected an exception due to duplicate FBgns");
        } catch (RuntimeException e) {
        }
    }

    public void testHandleGene() throws Exception {
        String gff = "4\t.\tgene\t230506\t233418\t.\t+\t.\tID=CG1587;Name=Crk;Dbxref=FlyBase:FBan0001587,FlyBase:FBgn0024811;synonym=Crk;synonym_2nd=CRK,D-CRK";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        converter.parse(srcReader);
        FileWriter writerSrc = new FileWriter(new File("chado_items.xml"));
        writerSrc.write(FullRenderer.render(writer.getItems()));
        writerSrc.close();

    }


}
