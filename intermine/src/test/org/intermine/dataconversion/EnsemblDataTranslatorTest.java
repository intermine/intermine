package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.flymine.ontology.OntologyUtil;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.ItemHelper;
import org.flymine.xml.full.FullRenderer;
import org.flymine.xml.full.FullParser;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;


public class EnsemblDataTranslatorTest extends TestCase {
    private String srcNs = "http://www.flymine.org/model/ensembl#";
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    protected Map itemMap;

    public void setUp() throws Exception {
        itemMap = new HashMap();
    }

    public void testEnsemblTranslator() throws Exception {
        HashMap itemMap = new HashMap();
        ItemWriter iw = new MockItemWriter(itemMap);
        Iterator i = getSrcItems().iterator();
        while (i.hasNext()) {
            iw.store(ItemHelper.convert((Item) i.next()));
        }
        OntModel model = getFlyMineOwl();
        DataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap), getFlyMineOwl(), tgtNs, "wildebeast", "W. beast", "1001");
        MockItemWriter tgtIw = new MockItemWriter(new HashMap());
        translator.translate(tgtIw);

        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }


    private Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/EnsemblDataTranslatorFunctionalTest_tgt.xml"));
    }

    private Collection getSrcItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/EnsemblDataTranslatorFunctionalTest_src.xml"));
    }


    protected OntModel getFlyMineOwl() {
        InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("genomic.n3"));

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(reader, null, "N3");
        return ont;
    }
}
