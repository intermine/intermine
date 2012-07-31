package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;

import org.intermine.bio.ontology.OboRelation;
import org.intermine.bio.ontology.OboTerm;
import org.intermine.bio.ontology.OboTermSynonym;
import org.intermine.bio.ontology.OboTypeDefinition;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class OboConverterTest extends ItemsTestCase {
    MockItemWriter itemWriter;
    Model model = Model.getInstanceByName("genomic");

    public OboConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap());
    }

    public void test1() throws Exception {
        OboConverter converter = new OboConverter(itemWriter, model, "", "SO", "http://www.flymine.org",
                                                  "OntologyTerm");
        OboTerm a = new OboTerm("SO:42", "parent");
        OboTerm b = new OboTerm("SO:43", "child");
        OboTerm c = new OboTerm("SO:44", "partof");
        OboTerm d = new OboTerm("SO:45", "obsolete");
        d.setObsolete(true);
        c.addSynonym(new OboTermSynonym("syn2", "exact_synonym"));
        b.addSynonym(new OboTermSynonym("syn1", "narrow_synonym"));
        b.addSynonym(new OboTermSynonym("syn2", "exact_synonym"));
        d.addSynonym(new OboTermSynonym("syn2", "exact_synonym"));
        d.addSynonym(new OboTermSynonym("syn3", "narrow_synonym"));
        OboRelation r1 = new OboRelation("SO:43","SO:42",new OboTypeDefinition("is_a", "is_a", true));
        OboRelation r2 = new OboRelation("SO:44","SO:43",new OboTypeDefinition("part_of", "part_of", true));
        OboRelation r3 = new OboRelation("SO:43","SO:45",new OboTypeDefinition("is_a", "is_a", true));

        converter.setOboTerms(Arrays.asList(new OboTerm[] {a, b, c, d}));
        converter.setOboRelations(Arrays.asList(new OboRelation[] {r1,r2,r3} ));
        converter.storeItems();
        //writeItemsFile(itemWriter.getItems(), "obo-converter-tgt.xml");
        assertEquals(readItemSet("OboConverterTest.xml"), itemWriter.getItems());
    }

}
