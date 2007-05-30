package org.intermine.bio.networkview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinInteraction;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullParser;

public class FlyNetworkCreatorTest extends TestCase
{
    Model model;

    String xmlResource = "FlyNetworkCreatorTest.xml";

    protected void setUp() throws Exception {
        model = Model.getInstanceByName("genomic");
    }

    protected void tearDown() throws Exception {
        model = null;
    }

    public void testCreateFlyNetwork() {
        // TODO: try to add tests for edges and attributes
        // that might be a bit more complicated
        int count = 0;
        ArrayList l1 = new ArrayList();
        ArrayList l2 = new ArrayList();
        Collection interactions = getFilteredList();

        // creating a Collection of all protein ids (primary accession) 
        // from the collection of protein interactions
        for (Iterator iter = interactions.iterator(); iter.hasNext();) {
            ProteinInteraction ion = (ProteinInteraction) iter.next();
            System.out.println("new interaction...");
            if (!l1.contains(ion.getProtein().getPrimaryAccession())) {
                l1.add(ion.getProtein().getPrimaryAccession());
            }
            Set<Protein> interacts = (Set<Protein>) ion.getInteractingProteins();
            for (Protein protein : interacts) {
                if (!l1.contains(protein.getPrimaryAccession())) {
                    l1.add(protein.getPrimaryAccession());
                }
                count++;
            }
        }
        System.out.println("total proteins: " + count);

        System.out.println("*** elements of 1.List:");
        for (Iterator iter = l1.iterator(); iter.hasNext();) {
            String acc = (String) iter.next();
            System.out.println(acc);
        }
        System.out.println("end of first 1.List");
        // converting the interactions collection into a flymine network
        FlyNetwork fn = null;
        fn = FlyNetworkCreator.createFlyNetwork(interactions);
        assertNotNull(fn);

        // creating a Collection of all node ids (label) from the network
        Collection nodes = fn.getNodes();
        count = 0;
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            FlyNode node = (FlyNode) iter.next();
            l2.add(node.getLabel());
            count++;
            System.out.println("node label: " + node.getLabel());
        }
        System.out.println("total FlyNodes: " + count);

        System.out.println("*** elements of 2.List:");
        for (Iterator iter = l2.iterator(); iter.hasNext();) {
            String acc = (String) iter.next();
            System.out.println(acc);
        }

        // test if all proteins have become nodes
        assertTrue(l2.containsAll(l1));
        assertTrue(l1.containsAll(l2));
        assertEquals("number of nodes/proteins: ", l1.size(), l2.size());
    }

    //*** helper methods
    private Collection getFilteredList() {
        ArrayList pi = new ArrayList();
        Collection list = getExpectedObjects();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Object element = (Object) iter.next();
            if (element instanceof ProteinInteraction) {
                ProteinInteraction inter = (ProteinInteraction) element;
                pi.add(inter);
            }
        }
        return pi;
    }

    private Collection getExpectedObjects() {
        Collection c = null;
        try {
            c = FullParser.realiseObjects(getExpectedItems(), model, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream(
                xmlResource));
    }
}
