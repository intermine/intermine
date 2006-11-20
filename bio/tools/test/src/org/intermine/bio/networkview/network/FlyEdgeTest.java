package org.intermine.bio.networkview.network;

import org.intermine.bio.networkview.network.FlyEdge;
import org.intermine.bio.networkview.network.FlyNode;

import junit.framework.TestCase;

public class FlyEdgeTest extends TestCase {
	FlyNode n1, n2;
	FlyEdge fe;

	protected void setUp() throws Exception {
		n1 = new FlyNode("source");
		n2 = new FlyNode("target");
		fe = new FlyEdge(n1, n2, "edge-node1-node2");
	}
	
	protected void tearDown() throws Exception {
		n1 = null;
		n2 = null;
		fe = null;
	}
	
	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyEdge.getSource()'
	 */
	public void testGetSource() {
		assertEquals("FlyEdge: getSource test failed: ", fe.getSource().getLabel(), "source");
	}

	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyEdge.getTarget()'
	 */
	public void testGetTarget() {
		assertEquals("FlyEdge: getTarget test failed: ", fe.getTarget().getLabel(), "target");
	}


}
