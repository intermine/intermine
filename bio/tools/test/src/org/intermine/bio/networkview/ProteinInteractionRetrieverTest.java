package org.intermine.bio.networkview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.intermine.bio.networkview.ProteinInteractionRetriever;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.xml.full.FullParser;

public class ProteinInteractionRetrieverTest extends TestCase {
	Model model;
	String xmlResource = "FlyNetworkCreatorTest.xml";
	ObjectStore os;
	ObjectStoreWriter osw;
	
	protected void setUp() throws Exception {
		model = Model.getInstanceByName("genomic");
		osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
		os = osw.getObjectStore();
		osw.beginTransaction();
		Collection c = getExpectedObjects();
		for (Iterator iter = c.iterator(); iter.hasNext();) {
			InterMineObject element = (InterMineObject) iter.next();
			osw.store(element);
		}
		osw.commitTransaction();
	}
	
	protected void tearDown() throws Exception {
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        //ObjectStore os = osw.getObjectStore();
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
        osw.close();

	}

//	public void testExpandNetworkFromProtein(){
//		ProteinInteractionRetriever ret = new ProteinInteractionRetriever(os);
//		FlyNetwork net = ret.expandNetworkFromProtein("Q9VI89");
//		Collection nodes = net.getNodes();
//		for (Iterator iter = nodes.iterator(); iter.hasNext();) {
//			FlyNode node = (FlyNode) iter.next();
//			System.out.println(node.getLabel());
//		}
//	}
//	
	public void testExpandNetworkFromProteins(){
		ProteinInteractionRetriever ret = new ProteinInteractionRetriever(os);
		ArrayList list = new ArrayList();
		list.add("Q9W5X1");
//		list.add("Q9XTN4");
//		list.add("Q9W0B3");
//		list.add("Q8MQW8");
		list.add("P09956");
//		list.add("P23758");
//		list.add("Q9VN10");
//		list.add("Q8T3W6");
//		list.add("Q9VK43");
//		list.add("Q8SX72");
//		list.add("Q9VMD7");
//		list.add("Q9VH72");

		FlyNetwork net = ret.expandNetworkFromProteins(list,2);
		Collection nodes = net.getNodes();
		System.out.println("");
		for (Iterator iter = nodes.iterator(); iter.hasNext();) {
			FlyNode node = (FlyNode) iter.next();
			System.out.println(node.getLabel());
		}
	}
	
	private Collection getExpectedObjects() {
		Collection c = null;
		try {
			c = FullParser.realiseObjects(getExpectedItems(), model, false);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return c;
	}
	
	private Collection getExpectedItems() throws Exception {
			return FullParser.parse(getClass().getClassLoader().
	    			getResourceAsStream(xmlResource));
	}

}
