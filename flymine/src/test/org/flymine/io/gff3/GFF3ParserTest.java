package org.flymine.io.gff3;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/gff_test_data.gff3")));

        List records = GFF3Parser.parse(reader);

        String [] expected =
            new String[]{
"<GFF3Record: sequenceID: ctg123 source: . type: gene start: 1000 end: 9000 score: -Infinity strand: + phase: . attributes: {ID=[gene00001], Name=[EDEN, zen]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: TF_binding_site start: 1000 end: 1012 score: -Infinity strand: + phase: . attributes: {ID=[tfbs00001], Name=[name1, name2], Parent=[gene00001]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: mRNA start: 1050 end: 9000 score: -Infinity strand: + phase: . attributes: {ID=[mRNA00001], Parent=[gene00001], Name=[EDEN.1]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: five_prime_UTR start: 1050 end: 1200 score: -Infinity strand: + phase: . attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 1201 end: 1500 score: -Infinity strand: + phase: 0 attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 3000 end: 3902 score: -Infinity strand: + phase: 0 attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 5000 end: 5500 score: -Infinity strand: + phase: 0 attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 7000 end: 7600 score: -Infinity strand: + phase: 0 attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: three_prime_UTR start: 7601 end: 9000 score: -Infinity strand: + phase: . attributes: {Parent=[mRNA0001]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: mRNA start: 1050 end: 9000 score: -Infinity strand: + phase: . attributes: {ID=[mRNA00002], Parent=[gene00001], Name=[EDEN.2]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: five_prime_UTR start: 1050 end: 1200 score: -Infinity strand: + phase: . attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 1201 end: 1500 score: -Infinity strand: + phase: 0 attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 5000 end: 5500 score: -Infinity strand: + phase: 0 attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 7000 end: 7600 score: -Infinity strand: + phase: 0 attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: three_prime_UTR start: 7601 end: 9000 score: -Infinity strand: + phase: . attributes: {Parent=[mRNA0002]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: mRNA start: 1300 end: 9000 score: -Infinity strand: + phase: . attributes: {ID=[mRNA00003], Parent=[gene00001], Name=[EDEN.3]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: five_prime_UTR start: 1300 end: 1500 score: -Infinity strand: + phase: . attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: five_prime_UTR start: 3000 end: 3300 score: -Infinity strand: + phase: . attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 3301 end: 3902 score: -Infinity strand: + phase: 0 attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 5000 end: 5500 score: -Infinity strand: + phase: 2 attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: CDS start: 7000 end: 7600 score: -Infinity strand: + phase: 2 attributes: {Parent=[mRNA0003]}>",
"<GFF3Record: sequenceID: ctg123 source: . type: three_prime_UTR start: 7601 end: 9000 score: -Infinity strand: + phase: . attributes: {Parent=[mRNA0003]}>",

                        };

        assertEquals(expected.length, records.size());

        for (int i = 0; i < 22; i++) {
            
            org.intermine.web.LogMe.log("i", "record: " + records.get(i));
            //            assertEquals(expected[i], ((GFF3Record) records.get(i)).toString());
        }

        GFF3Record record0 = (GFF3Record) records.get(0);

        List names = (List) record0.getAttributes().get("Name");

        assertEquals("EDEN", names.get(0));
        assertEquals("zen", names.get(1));

        assertEquals("EDEN", record0.getName());
        assertEquals("EDEN", record0.getName());

        assertEquals(9000, record0.getEnd());

        GFF3Record record1 = (GFF3Record) records.get(1);

        assertEquals("name1", record1.getName());
        assertEquals("gene00001", record1.getParent());
        assertNull(record1.getOntologyTerm());

        for (int i = 0; i < 22; i++) {
            
            org.intermine.web.LogMe.log("i", "record: " + records.get(i));
            org.intermine.web.LogMe.log("i", "expect: " + expected[i]);
            assertEquals(expected[i], ((GFF3Record) records.get(i)).toString());
        }
    }
}
