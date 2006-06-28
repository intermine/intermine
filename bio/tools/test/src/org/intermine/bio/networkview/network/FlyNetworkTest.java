package org.intermine.bio.networkview.network;

import java.util.Collection;

import org.intermine.bio.networkview.network.FlyEdge;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FlyNetworkTest extends TestCase
{
    private FlyNetwork fnw, fntest;

    private FlyNode n0, n1, n2, n3, nx;

    private FlyEdge e1, e2, e3, e1get, ex;

    private Collection cn, ce;

    String a1, a2, a3;

    protected void setUp() throws Exception {
        fnw = new FlyNetwork();
        n0 = new FlyNode("nodeWOinteraction");
        n1 = new FlyNode("node1");
        n2 = new FlyNode("node2");
        n3 = new FlyNode("node3");
        nx = new FlyNode("xxx");
        e1 = new FlyEdge(n2, n3, "pn"); // test ability to have edges between same nodes
        e2 = new FlyEdge(n3, n2, "pn"); // with same type just opposite directions
        // does NOT work with cytoscape networks 
        e3 = new FlyEdge(n3, n1, "xx");

        a1 = "attribute1";
        a2 = "attribute2";
        a3 = "attribute3";
        e1.setAttribute(a1, "value1");
        e1.setAttribute(a2, new Integer(1));
        e1.setAttribute(a3, new Double(0.1));
        e2.setAttribute(a1, "value2");
        e2.setAttribute(a2, new Integer(2));
        e2.setAttribute(a3, new Double(0.2));
        e3.setAttribute(a1, "value3");
        e3.setAttribute(a2, new Integer(3));
        e3.setAttribute(a3, new Double(0.3));

        fnw.addNode(n0);
        fnw.addNode(n1);
        fnw.addNode(n2);
        fnw.addNode(n3);
        fnw.addEdge(e1);
        fnw.addEdge(e2);
        fnw.addEdge(n1, n2, "pp", "edge1"); // use addEdge method to create edge
        e1get = fnw.getEdge("edge1"); // edge created with addEdge method
        ex = new FlyEdge(n1, n2, "edge2"); // edge not part of network -> test

        cn = fnw.getNodes(); // n1, n2 and n3
        ce = fnw.getEdges(); // e1, e2 and e1get

    }

    protected void tearDown() throws Exception {
        fnw = fntest = null;
        n1 = n2 = n3 = nx = null;
        e1 = e2 = e1get = ex = null;
        cn = ce = null;
        a1 = a2 = a3 = null;
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.FlyNetwork()'
     */
    public void testFlyNetwork() {
        assertNotNull("constructor test: ", fnw);
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.addNode(FlyNode)'
     */
    public void testAddNodeFlyNode() {
        FlyNode nx2 = new FlyNode("nodex2");
        assertTrue("addNode(FlyNode): ", fnw.addNode(nx2));
        assertFalse("addNode(FlyNode) node already exists: ", fnw.addNode(nx2));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.addNode(String)'
     */
    public void testAddNodeString() {
        assertTrue("addNode(String): ", fnw.addNode("testnode"));
        assertFalse("addNode(String) node already exists: ", fnw.addNode("testnode"));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.addEdge(FlyEdge)'
     */
    public void testAddEdgeFlyEdge() {
        assertTrue("addEdge(FlyEdge): ", fnw.addEdge(e3));
        assertFalse("addEdge(FlyEdge): ", fnw.addEdge(e3));

        // the egde itself is not added to the network, but a copy
        // therefore a later change on the edge does not have to 
        // effect the edge added to the network
        FlyEdge tmpEdge = fnw.getEdge(e3.getLabel());
        assertTrue("equaltity test: ", e3.isEqual(tmpEdge));
        e3.setAttribute(a1, "newValue");
        assertFalse("equaltity test: ", e3.isEqual(tmpEdge));
        // nevertheless the label was not altered, but it has to be 
        // unique within the network, hence you are not allowed to add
        // the altered edge:
        assertFalse("addEdge(FlyEdge): ", fnw.addEdge(tmpEdge));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.addEdge(FlyNode, FlyNode)'
     */
    public void testAddEdgeFlyNodeFlyNode() {
        assertTrue("addEdge(FlyNode,FlyNode): ", fnw.addEdge(n1, n2));
        assertFalse("addEgde(FlyNode,FlyNode) edge exists: ", fnw.addEdge(n1, n2));
        assertFalse("addEgde(FlyNode,FlyNode) missing node: ", fnw.addEdge(n1, nx));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.addEdge(FlyNode, FlyNode, String)'
     */
    public void testAddEdgeFlyNodeFlyNodeString() {
        assertTrue("addEdge(FlyNode,FlyNode,String): ", fnw.addEdge(n1, n2, "pp",
                "edge11"));
        assertTrue("addEdge(FlyNode,FlyNode,String): ", fnw.addEdge(n1, n2, "pp",
                "edge21"));
        assertFalse("addEdge(FlyNode,FlyNode,String) edge already exists: ", fnw.addEdge(
                n1, n2, "pp", "edge21"));
        assertFalse("addEdge(FlyNode,FlyNode,String) missing node: ", fnw.addEdge(n1, nx,
                "pp", "edge11"));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.addEdge(FlyNode, FlyNode, String, String)'
     */
    public void testAddEdgeFlyNodeFlyNodeStringString() {
        assertTrue("addEdge(FlyNode,FlyNode,String,String): ", fnw.addEdge(n1, n2, "px",
                "edge12"));
        assertTrue("addEdge(FlyNode,FlyNode,String,String): ", fnw.addEdge(n1, n2, "pn",
                "edge22"));
        assertFalse("addEdge(FlyNode,FlyNode,String,String): ", fnw.addEdge(n1, n2, "pn",
                "edge12"));
        assertFalse("addEdge(FlyNode,FlyNode,String,String) missing node: ", fnw.addEdge(
                n1, nx, "pp", "edge32"));

    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.containsNode(String)'
     */
    public void testContainsNodeString() {
        assertTrue("test containsNode(String): ", fnw.containsNode("node1"));
        assertFalse("test containsNode(String): ", fnw.containsNode("xyz"));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.containsNode(FlyNode)'
     */
    public void testContainsNodeFlyNode() {
        assertTrue("test containsNode(FlyNode): ", fnw.containsNode(n1));
        assertFalse("test containsNode(FlyNode): ", fnw.containsNode(nx));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.containsEdge(String)'
     */
    public void testContainsEdgeString() {
        assertTrue("test containsEdge(String): ", fnw.containsEdge("edge1"));
        assertTrue("test containsEdge(String): ", fnw.containsEdge("node3 (pn) node2"));
        assertFalse("test containsEdge(String): ", fnw.containsNode("xyz"));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.containsEdge(FlyEdge)'
     */
    public void testContainsEdgeFlyEdge() {
        assertTrue("test containsEdge(String): ", fnw.containsEdge(e1get));
        assertTrue("test containsEdge(String): ", fnw.containsEdge(e1));
        assertFalse("test containsEdge(String): ", fnw.containsEdge(ex));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.getNodes()'
     */
    public void testGetNodes() {
        assertEquals("getNodes: ", 4, cn.size());
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.getEdges()'
     */
    public void testGetEdges() {
        assertEquals("getEdges: ", 3, ce.size());
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.getEdge(String)'
     */
    public void testGetEdge() {
        FlyEdge etmp = fnw.getEdge("edge1");
        FlyEdge etmp2 = fnw.getEdge("node3 (pn) node2");
        FlyEdge etmp3 = fnw.getEdge("node2 (pn) node3");
        assertEquals("getEdge: ", "edge1", etmp.getLabel());
        assertEquals("getEdge: ", "node3 (pn) node2", etmp2.getLabel());
        assertEquals("getEdge: ", "node2 (pn) node3", etmp3.getLabel());
        assertNull("getEdge - not existing edge: ", fnw.getEdge("foo"));
        // testing attributes
        String s1 = (String) etmp2.getAttributeValue(a1);
        Integer i1 = (Integer) etmp2.getAttributeValue(a2);
        Double d1 = (Double) etmp2.getAttributeValue(a3);
        assertEquals("getEdge testing attributes: ", "value2", s1);
        assertEquals("getEdge testing attributes: ", 2, i1.intValue());
        assertEquals("getEdge testing attributes: ", 0.2, d1.doubleValue(), 0.0);
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.getNode(String)'
     */
    public void testGetNode() {
        FlyNode ntmp = fnw.getNode("node1");
        assertEquals("getNode: ", "node1", ntmp.getLabel());
        assertNull("getNode - not existing node: ", fnw.getNode("foo"));
    }

    /*
     * Test method for 'org.intermine.bio.networkview.network.FlyNetwork.equals(FlyNetwork)'
     */
    public void testEquals() {
        fntest = new FlyNetwork();
        fntest.addNode(n0);
        fntest.addNode(n1);
        fntest.addNode(n2);
        fntest.addNode(n3);
        fntest.addEdge(e1);
        fntest.addEdge(e2);
        fntest.addEdge(n1, n2, "pp", "edge1"); // use addEdge method to create and add edge
        fntest.getEdge("edge1");

        assertTrue(fnw.isEqual(fntest));

        System.out.println(fnw.toString(true));
    }

    public void testToSIF() {
        System.out.println("network in SIF format:");
        System.out.println(fnw.toSIF());
    }

    public static Test suite() {
        return new TestSuite(FlyNetworkTest.class);
    }

}
