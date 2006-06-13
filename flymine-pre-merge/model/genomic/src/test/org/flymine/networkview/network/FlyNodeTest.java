package org.flymine.networkview.network;

import junit.framework.TestCase;

public class FlyNodeTest extends TestCase {
	

	/*
	 * Test method for 'org.flymine.networkview.network.FlyNode.FlyNode(String)'
	 */
	public void testFlyNode() {
		FlyNode fn = new FlyNode("test");
		assertEquals("FlyNode: constructor test failed: ", "test", fn.getLabel());
	}
	
}
