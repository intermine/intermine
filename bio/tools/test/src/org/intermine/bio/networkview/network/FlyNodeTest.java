package org.intermine.bio.networkview.network;

import org.intermine.bio.networkview.network.FlyNode;

import junit.framework.TestCase;

public class FlyNodeTest extends TestCase {
	

	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyNode.FlyNode(String)'
	 */
	public void testFlyNode() {
		FlyNode fn = new FlyNode("test");
		assertEquals("FlyNode: constructor test failed: ", "test", fn.getLabel());
	}
	
}
