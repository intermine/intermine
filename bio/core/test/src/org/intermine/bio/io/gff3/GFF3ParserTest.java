package org.intermine.bio.io.gff3;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;

import junit.framework.TestCase;

/**
 * Tests for the GFF3Parser class.
 *
 * @author Kim Rutherford
 */

public class GFF3ParserTest extends TestCase
{
    public GFF3ParserTest (String arg) {
        super(arg);
    }

    public void testParse() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("gff_test_data.gff3")));

        List records = new ArrayList();

        Iterator iter = GFF3Parser.parse(reader);

        while (iter.hasNext()) {
            GFF3Record record = (GFF3Record) iter.next();
            records.add(record);
        }

        String [] expected =
            new String[]{
"<GFF3Record: sequenceID: ctg123 source: null type: gene start: 1000 end: 9000 score: null strand: + phase: null attributes: {ID=[gene00001], Name=[EDEN, zen]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: TF_binding_site start: 1000 end: 1012 score: null strand: + phase: null attributes: {ID=[tfbs00001], Name=[name1, name2], Parent=[gene00001]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: mRNA start: 1050 end: 9000 score: null strand: + phase: null attributes: {ID=[mRNA00001], Parent=[gene00001], Name=[EDEN.1]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: five_prime_UTR start: 1050 end: 1200 score: null strand: + phase: null attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 1201 end: 1500 score: null strand: + phase: 0 attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 3000 end: 3902 score: null strand: + phase: 0 attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 5000 end: 5500 score: null strand: + phase: 0 attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 7000 end: 7600 score: null strand: + phase: 0 attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: three_prime_UTR start: 7601 end: 9000 score: null strand: + phase: null attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: mRNA start: 1050 end: 9000 score: null strand: + phase: null attributes: {ID=[mRNA00002], Parent=[gene00001], Name=[EDEN.2]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: five_prime_UTR start: 1050 end: 1200 score: null strand: + phase: null attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 1201 end: 1500 score: null strand: + phase: 0 attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 5000 end: 5500 score: null strand: + phase: 0 attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 7000 end: 7600 score: null strand: + phase: 0 attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: three_prime_UTR start: 7601 end: 9000 score: null strand: + phase: null attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: mRNA start: 1300 end: 9000 score: null strand: + phase: null attributes: {ID=[mRNA00003], Parent=[gene00001], Name=[EDEN.3]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: five_prime_UTR start: 1300 end: 1500 score: null strand: + phase: null attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: five_prime_UTR start: 3000 end: 3300 score: null strand: + phase: null attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 3301 end: 3902 score: null strand: + phase: 0 attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 5000 end: 5500 score: null strand: + phase: 2 attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: CDS start: 7000 end: 7600 score: null strand: + phase: 2 attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: null type: three_prime_UTR start: 7601 end: 9000 score: null strand: + phase: null attributes: {Parent=[mRNA0003]}>",

                        };

        assertEquals(expected.length, records.size());

        GFF3Record record0 = (GFF3Record) records.get(0);

        List names = (List) record0.getAttributes().get("Name");

        assertEquals("EDEN", names.get(0));
        assertEquals("zen", names.get(1));

        assertEquals(9000, record0.getEnd());

        assertNull(record0.getSource());

        GFF3Record record1 = (GFF3Record) records.get(1);

        assertEquals("name1", record1.getNames().get(0));
        assertEquals("gene00001", record1.getParents().get(0));
        assertNull(record1.getOntologyTerm());

        for (int i = 0; i < 22; i++) {
            assertEquals(expected[i], ((GFF3Record) records.get(i)).toString());
        }
    }

    public void testDecoding() throws Exception {
        String input = "2L\t.\tgene\t14263522\t14328265\t.\t+\t.\t" +
                "ID=CG15288;Name=wb;Dbxref=FlyBase:FBan0015288,FlyBase:FBgn0004002;cyto_range=35A3-35A4;" +
                "dbxref_2nd=FlyBase:FBgn0025691,FlyBase:FBgn0032544,GB:CG15288;gbunit=AE003643;synonym=wb;" +
                "synonym_2nd=A1,BEST:CK02229,BG:DS03792.1,CK02229,CT35236,DLAM,D-laminin+%26agr%3B2,l(2)09437," +
                "l(2)34Fb,l(2)br1,l34Fb,laminin,laminin+%26agr%3B1%2C2,Laminin+%26agr%3B1%2C2,LM-A/%26agr%3B2,wing+blistered";
        GFF3Record record = new GFF3Record(input);
    }
    
    public void testFixGreek() {
        String input = "<br/>&agr;<br/>&Agr;<br/>&bgr;<br/>&Bgr;<br/>&ggr;" +
                "<br/>&Ggr;<br/>&dgr;<br/>&Dgr;<br/>&egr;<br/>&Egr;<br/>&zgr;" +
                "<br/>&Zgr;<br/>&eegr;<br/>&EEgr;<br/>&thgr;<br/>&THgr;<br/>&igr;" +
                "<br/>&Igr;<br/>&kgr;<br/>&Kgr;<br/>&lgr;<br/>&Lgr;<br/>&mgr;<br/>&Mgr;" +
                "<br/>&ngr;<br/>&Ngr;<br/>&xgr;<br/>&Xgr;<br/>&ogr;<br/>&Ogr;<br/>&pgr;<br/>" +
                "&Pgr;<br/>&rgr;<br/>&Rgr;<br/>&sgr;<br/>&Sgr;<br/>&sfgr;<br/>&tgr;<br/>&Tgr;" +
                "<br/>&ugr;<br/>&Ugr;<br/>&phgr;<br/>&PHgr;<br/>&khgr;<br/>&KHgr;<br/>&psgr;" +
                "<br/>&PSgr;<br/>&ohgr;<br/>&OHgr;";
        String expect = "<br/>&alpha;<br/>&Alpha;<br/>&beta;<br/>&Beta;<br/>&gamma;" +
                "<br/>&Gamma;<br/>&delta;<br/>&Delta;<br/>&epsilon;<br/>&Epsilon;<br/>&zeta;" +
                "<br/>&Zeta;<br/>&eta;<br/>&Eta;<br/>&theta;<br/>&Theta;<br/>&iota;" +
                "<br/>&Iota;<br/>&kappa;<br/>&Kappa;<br/>&lambda;<br/>&Lambda;<br/>&mu;<br/>&Mu;" +
                "<br/>&nu;<br/>&Nu;<br/>&xi;<br/>&Xi;<br/>&omicron;<br/>&Omicron;<br/>&pi;" +
                "<br/>&Pi;<br/>&rho;<br/>&Rho;<br/>&sigma;<br/>&Sigma;<br/>&sigmaf;<br/>&tau;<br/>&Tau;" +
                "<br/>&upsilon;<br/>&Upsilon;<br/>&phi;<br/>&Phi;<br/>&chi;<br/>&Chi;<br/>&psi;" +
                "<br/>&Psi;<br/>&omega;<br/>&Omega;";
        String output = GFF3Record.fixEntityNames(input);
        assertEquals(expect, output);
    }

    public void testSpaces() throws Exception {
        String gff="4\t.\texon\t22335\t22528\t.\t-\t.\tID=CG32013:2;Parent=CG32013-RA,CG32013-RB;Gap=A+B;Target=C+D;Other=E+F\n";
        GFF3Record record = new GFF3Record(gff);
        assertEquals(Arrays.asList(new Object[]{"A+B"}), record.getAttributes().get("Gap"));
        assertEquals(Arrays.asList(new Object[]{"C+D"}), record.getAttributes().get("Target"));
        assertEquals(Arrays.asList(new Object[]{"E F"}), record.getAttributes().get("Other"));
    }
    
    public void testToGFF3() throws Exception {
        String original="4\t.\texon\t22335\t22528\t.\t-\t.\tID=CG32013%3A2;Parent=CG32013-RA\n"
            + "4\t.\texon\t22335\t22528\t1000.0\t-\t1\tID=CG32013%3A2;Parent=CG32013-RA\n";
        StringBuffer sb = new StringBuffer();
        Iterator iter = GFF3Parser.parse(new BufferedReader(new StringReader(original)));
        while (iter.hasNext()) {
            sb.append(((GFF3Record) iter.next()).toGFF3()).append("\n");
        }
        assertEquals(original, sb.toString());
    }

    public void testParents() throws Exception {
        String gff="4\t.\texon\t22335\t22528\t.\t-\t.\tID=CG32013:2;Parent=CG32013-RA,CG32013-RB\n";
        StringBuffer sb = new StringBuffer();
        Iterator iter = GFF3Parser.parse(new BufferedReader(new StringReader(gff)));
        GFF3Record record = (GFF3Record) iter.next();

        List expected = new ArrayList(Arrays.asList(new String[] {"CG32013-RA", "CG32013-RB"}));
        assertEquals(expected, record.getParents());
    }

}
