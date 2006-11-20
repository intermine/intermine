package org.intermine.bio.networkview.network;

import java.util.Collection;

import org.intermine.bio.networkview.network.FlyEdge;
import org.intermine.bio.networkview.network.FlyNode;

import junit.framework.TestCase;

public class FlyHashGraphElementTest extends TestCase {
	FlyNode fn1, fn2, fn3;
	FlyEdge fe1, fe2, fe3;
	String s1, s2, s3;

	protected void setUp() throws Exception {
		fn1 = new FlyNode("test");
		fn2 = new FlyNode("test2");
		fn3 = new FlyNode("test");
		
		fe1 = new FlyEdge(fn1, fn2);
		fe2 = new FlyEdge(fn1, fn3);
		fe3 = new FlyEdge(fn1, fn2);
		
		s1 = "attribute1";
		s2 = "attribute2";
		s3 = "attribute3";
		
		fn1.setAttribute(s1, new Integer(1));
		fn1.setAttribute(s2, new Integer(2));
		fn1.setAttribute(s3, new Integer(3));
		fn1.setAttribute(s3, new Integer(5));
		
		fn3.setAttribute(s1, new Integer(1));
		fn3.setAttribute(s2, new Integer(2));
		fn3.setAttribute(s3, new Integer(3));
		fn3.setAttribute(s3, new Integer(5));

		fe1.setAttribute(s1, new Integer(1));
		fe1.setAttribute(s2, new Integer(2));
		fe1.setAttribute(s3, new Integer(3));
		fe1.setAttribute(s3, new Integer(5));

		fe2.setAttribute(s1, new Integer(1));
		fe2.setAttribute(s2, new Integer(2));
		fe2.setAttribute(s3, new Integer(3));
		fe2.setAttribute(s3, new Integer(5));

		fe3.setAttribute(s1, new Integer(1));
		fe3.setAttribute(s2, new Integer(2));
		fe3.setAttribute(s3, new Integer(3));
		fe3.setAttribute(s3, new Integer(5));

	}

	protected void tearDown() throws Exception {
		fn1 = fn2 = null;
		s1 = s2 = s3 = null;
		fe1 = fe2 = fe3 = null;
	}

	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyHashGraphElement.getLabel()'
	 */
	public void testGetLabel() {
		assertEquals("FlyHashGraphElement: getLabel test failed: ", fn1.getLabel(), "test");
	}

	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyHashGraphElement.getAttributNames()'
	 */
	public void testGetAttributNames() {
		Collection c = fn1.getAttributeNames();
		c.remove(s1); 
		c.remove(s2);
		c.remove(s3);
		
		assertTrue("HashGraphElement: getAttributeNames test failed: ", c.isEmpty());
	}

	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyHashGraphElement.getAttributeValue(String)'
	 */
	public void testSetGetAttribute() {
		//test whether value for attribute "attribute1" was accurately set
		Integer i = (Integer) fn1.getAttributeValue(s1);
		assertEquals("HashGraphElement: get/set attribute test failed: ", 1, i.intValue());
		// try to set attribute "attribute1" but do not alter if already exists 
		// -> update = false
		boolean updated = fn1.setAttribute(s1, new Integer(7), false);
		assertFalse("HashGraphElement: set attribute (update=false) test failed: ", updated);
		assertEquals("HashGraphElement: get/set attribute test failed: ", 1, i.intValue());
		// try to set attribute "attribute1" whether it already exists or not
		// -> update = true
		updated = fn1.setAttribute(s1, new Integer(7), true);
		i = (Integer) fn1.getAttributeValue(s1);
		assertTrue("HashGraphElement: set attribute (update=false) test failed: ", updated);
		assertEquals("HashGraphElement: get/set attribute test failed: ", 7, i.intValue());
	}

	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyHashGraphElement.setAttribute(String, Object, boolean)'
	 */
	public void testUpdateAttributeValue() {
		// try to assign a new type to "attribute1"
		// TODO: this may cause problems with cytoscape -> check
		fn1.setAttribute(s1, "newValue", true);
		String s = (String) fn1.getAttributeValue(s1);
		assertEquals("HashGraphElement: update attribute value test failed: ", "newValue", s);
	}

	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyHashGraphElement.equals(FlyHashGraphElement)'
	 */
	public void testEquals(){
		FlyNode n1 = new FlyNode("element");
		FlyNode n2 = new FlyNode("element");
		FlyEdge e1 = new FlyEdge(n1, n2, null, "element");
		
		// test different FlyHashGraphElementS
		assertFalse("test for equality", e1.isEqual(n1));
		assertFalse("test for equality", n1.isEqual(e1));
		
		// test nodes for equality
		assertTrue("test for equality", fn1.isEqual(fn3));
		assertFalse("test for equality", fn1.isEqual(fn2));
		// test edges for equality
		assertTrue("test for equality", fe1.isEqual(fe3));
		assertFalse("test for equality", fe1.isEqual(fe2));
		System.out.println(fe1);
	}
}
