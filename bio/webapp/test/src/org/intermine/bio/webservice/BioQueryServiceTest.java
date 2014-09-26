package org.intermine.bio.webservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class BioQueryServiceTest {

    String[] geneView = {"Gene.secondaryIdentifier", "Gene.symbol", "Gene.primaryIdentifier", "Gene.organism.name"};
    String[] ProteinView = {"Protein.primaryIdentifier", "Protein.primaryAccession", "Protein.organism.name", "Protein.genes.secondaryIdentifier", "Protein.genes.symbol", "Protein.genes.primaryIdentifier", "Protein.genes.organism.name"};
    String[] nullView = null;
    String[] emptyView = {};
    String[] badView = {"abc;d"};

    @Before
    public void setup() {
        // TODO test path query behaviour, but how to get model? example?
    }

    @Test
    public void testGetPathQueryViews() {
        assertNull(BioQueryService.getPathQueryViews(nullView));
        assertNull(BioQueryService.getPathQueryViews(emptyView));
        assertEquals(Arrays.asList("abc;d"), BioQueryService.getPathQueryViews(badView));
        assertEquals(Arrays.asList("Gene.secondaryIdentifier", "Gene.symbol", "Gene.primaryIdentifier", "Gene.organism.name"), BioQueryService.getPathQueryViews(geneView));
        assertEquals(Arrays.asList("Protein.primaryIdentifier", "Protein.primaryAccession", "Protein.organism.name", "Protein.genes.secondaryIdentifier", "Protein.genes.symbol", "Protein.genes.primaryIdentifier", "Protein.genes.organism.name"), BioQueryService.getPathQueryViews(ProteinView));
    }
}
