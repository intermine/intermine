/**
 * 
 */
package org.intermine.webservice.server.output;

import junit.framework.TestCase;

/**
 * @author alex
 *
 */
public class JSONFormattingExceptionTest extends TestCase {

	/**
	 * @param name
	 */
	public JSONFormattingExceptionTest(String name) {
		super(name);
	}
	
	public void testConstruction() {
		JSONFormattingException e = new JSONFormattingException("There was an error");
		RuntimeException cause = new RuntimeException("There a was an error further down");
		
		assertTrue(e != null);
		assertEquals("There was an error", e.getMessage());
		
		e = new JSONFormattingException(cause);
		
		assertTrue(e != null);
		assertEquals(cause.toString(), e.getMessage());
		assertEquals(cause, e.getCause());
		
		e = new JSONFormattingException("It's not my fault - blame him!", cause);
		
		assertTrue(e != null);
		assertEquals("It's not my fault - blame him!", e.getMessage());
		assertEquals(cause, e.getCause());
	}

}
