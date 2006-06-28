package org.intermine.bio.networkview;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.intermine.bio.networkview.FlyNetworkIntegrator;
import org.intermine.bio.networkview.network.FlyEdge;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;


import junit.framework.TestCase;
import cytoscape.CyEdge;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class FlyNetworkIntegratorTest extends TestCase {
	FlyNetwork fn;
	FlyNode n1;
	FlyNode n2;
	FlyEdge e1, e2, testE1, testE2;
	
	protected void setUp() throws Exception {
		fn = new FlyNetwork();
		n1 = new FlyNode("node1");
		n2 = new FlyNode("node2");
		e1 = new FlyEdge(n1, n2, null, "node1 (pp) node2");
		e2 = new FlyEdge(n1, n2, "pn", "edge2");
		testE1 = new FlyEdge(n1, n2, "ra");	// test whether edges connecting same nodes but 
		testE2 = new FlyEdge(n2, n1, "rb");	// with different types are added to the network
		
		n1.setAttribute("isGene", new Boolean(false));
		n1.setAttribute("count", new Integer(3));
		n1.setAttribute("evidence", new Double(0.3));
		n1.setAttribute("synonym", "1node");
		
		n2.setAttribute("isGene", new Boolean(false));
		n2.setAttribute("count", new Integer(7));
		n2.setAttribute("evidence", new Double(0.7));
		n2.setAttribute("synonym", "2node");
		
		e1.setAttribute("isInterSpecies", new Boolean(false));
		e1.setAttribute("count", new Integer(5));
		e1.setAttribute("evidence", new Double(0.5));
		
		e2.setAttribute("isInterSpecies", new Boolean(true));
		e2.setAttribute("count", new Integer(7));
		e2.setAttribute("evidence", new Double(0.7));
		
		fn.addNode(n1);
		fn.addNode(n2);
		fn.addEdge(e1);
		fn.addEdge(e2);
		
		fn.addEdge(testE1);
		fn.addEdge(testE2);
		
	}

	protected void tearDown() throws Exception {
		fn = null;
		n1 = n2 = null;
		e1 = e2 = null;
	}

	/*
	 * Test method for 'org.intermine.bio.networkview.network.FlyNetworkIntegrator.integrateNetwork(FlyNetwork)'
	 */
	public void testIntegrateNetwork() {
		
		//***** checking whether initial cytoscape lists are really empty
		List nodes = Cytoscape.getCyNodesList();
		List edges = Cytoscape.getCyEdgesList();
		assertEquals("CyNodesList is not empty:", 0, nodes.size());
		assertEquals("CyEdgesList is not empty:", 0, edges.size());
		
		//***** integrating the FlyNetwork (2 Nodes, 1 Edge)
		FlyNetworkIntegrator.integrateNetwork(fn);
		
		//***** checking whether cytoscape lists now have accurate size
		nodes = Cytoscape.getCyNodesList();
		edges = Cytoscape.getCyEdgesList();
		assertEquals("getting empty CyNodesList:", 2, nodes.size());
		assertEquals("getting empty CyNodesList:", 4, edges.size());

		//***** check if FlyHashGraphElements are accurately integrated
		//*** check if labels are accurately integrated as identifier
		//* check nodes
		CyNode cyn1 = Cytoscape.getCyNode(n1.getLabel());
		assertEquals("checking FlyNode.label vs CyNode.identifier: ", 
				n1.getLabel(), cyn1.getIdentifier());
		CyNode cyn2 = Cytoscape.getCyNode(n2.getLabel());
		assertEquals("checking FlyNode.label vs CyNode.identifier: ", 
				n2.getLabel(), cyn2.getIdentifier());
		//* check edges
		CyEdge cye1 = Cytoscape.getCyEdge("node1", "node1 (pp) node2", "node2", "pp");
		assertEquals("checking FlyEdge.label vs CyNode.identifier: ", 
				"node1 (pp) node2", cye1.getIdentifier());
		CyEdge cye2 = Cytoscape.getCyEdge("node1", "edge2", "node2", "pn");
		assertEquals("checking FlyEdge.label vs CyNode.identifier: ", 
				"edge2", cye2.getIdentifier());
		edges = Cytoscape.getCyEdgesList();
		assertEquals("getting CyEdgesList:", 4, edges.size());


		//*** checking if node attributes are accurately integrated
		CyAttributes nodeAtts = Cytoscape.getNodeAttributes();
		//* checking Boolean values
		Boolean b = nodeAtts.getBooleanAttribute(cyn1.getIdentifier(), "isGene");
		assertEquals("checking node attribute: ", false, b.booleanValue());
		b = nodeAtts.getBooleanAttribute(cyn2.getIdentifier(), "isGene");
		assertEquals("checking node attribute: ", false, b.booleanValue());
		//* checking Integer values
		Integer i = nodeAtts.getIntegerAttribute(cyn1.getIdentifier(), "count");
		assertEquals("checking node attribute: ", 3, i.intValue());
		i = nodeAtts.getIntegerAttribute(cyn2.getIdentifier(), "count");
		assertEquals("checking node attribute: ", 7, i.intValue());
		//* checking Double values
		Double d = nodeAtts.getDoubleAttribute(cyn1.getIdentifier(), "evidence");
		assertEquals("checking node attribute: ", 0.3, d.doubleValue(), 0.0);
		d = nodeAtts.getDoubleAttribute(cyn2.getIdentifier(), "evidence");
		assertEquals("checking node attribute: ", 0.7, d.doubleValue(), 0.0);
		//* checking String values
		String s = nodeAtts.getStringAttribute(cyn1.getIdentifier(), "synonym");
		assertEquals("checking node attribute: ", "1node", s);
		s = nodeAtts.getStringAttribute(cyn2.getIdentifier(), "synonym");
		assertEquals("checking node attribute: ", "2node", s);

		//*** checking if edge attributes are accurately integrated
		CyAttributes edgeAtts = Cytoscape.getEdgeAttributes();
		//* checking Boolean value
		Boolean bb = edgeAtts.getBooleanAttribute(cye1.getIdentifier(), "isInterSpecies");
		assertEquals("checking edge attribute: ", false, bb.booleanValue());
		//* checking Integer value
		Integer ii = edgeAtts.getIntegerAttribute(cye1.getIdentifier(), "count");
		assertEquals("checking edge attribute: ", 5, ii.intValue());
		//* checking Double value
		Double dd = edgeAtts.getDoubleAttribute(cye1.getIdentifier(), "evidence");
		assertEquals("cheching edge attribute: ", 0.5, dd.doubleValue(), 0.0);
		//* checking String value
		String ss = edgeAtts.getStringAttribute(cye1.getIdentifier(), "interaction");
		assertEquals("checking edge attribute: ", "pp", ss);

		//* checking second edge
		//* checking Boolean value
		bb = edgeAtts.getBooleanAttribute(cye2.getIdentifier(), "isInterSpecies");
		assertEquals("checking edge attribute: ", true, bb.booleanValue());
		//* checking Integer value
		ii = edgeAtts.getIntegerAttribute(cye2.getIdentifier(), "count");
		assertEquals("checking edge attribute: ", 7, ii.intValue());
		//* checking Double value
		dd = edgeAtts.getDoubleAttribute(cye2.getIdentifier(), "evidence");
		assertEquals("cheching edge attribute: ", 0.7, dd.doubleValue(), 0.0);
		//* checking String value
		ss = edgeAtts.getStringAttribute(cye2.getIdentifier(), "interaction");
		assertEquals("checking edge attribute: ", "pn", ss);
		
	}
	
	public void testConvertNodesFly2Cy(){
		Collection fnc = fn.getNodes();
		Collection cnc = FlyNetworkIntegrator.convertNodesFly2Cy(fnc);
		for (Iterator iter = cnc.iterator(); iter.hasNext();) {
			CyNode element = (CyNode) iter.next();
			System.out.println("converted node: " + element.getIdentifier());
			
		}
	}

	public void testConvertEdgesFly2Cy(){
		Collection fec = fn.getEdges();
		Collection cec = FlyNetworkIntegrator.convertEdgesFly2Cy(fec);
		
		for (Iterator iter = cec.iterator(); iter.hasNext();) {
			CyEdge element = (CyEdge) iter.next();
			System.out.println("converted edge: " + element.getIdentifier());
		}
	}

	public void testiConvertNodesFly2Cy(){
		Collection fnc = fn.getNodes();
		int[] cnc = FlyNetworkIntegrator.iConvertNodesFly2Cy(fnc);
		for (int i = 0; i < cnc.length; i++) {
			System.out.println("iConverted node: " + cnc[i]);
		}
			//assertEquals("convertEdgeFly2Cy: ", e1.getLabel(), element.getIdentifier());
	}

	public void testiConvertEdgesFly2Cy(){
		Collection fec = fn.getEdges();
		int[] cec = FlyNetworkIntegrator.iConvertEdgesFly2Cy(fec);
		for (int i = 0; i < cec.length; i++) {
			System.out.println("iConverted edge: " + cec[i]);
		}
			//assertEquals("convertEdgeFly2Cy: ", e1.getLabel(), element.getIdentifier());
	}

}
