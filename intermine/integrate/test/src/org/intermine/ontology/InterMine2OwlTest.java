package org.intermine.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.rdf.model.Resource;

import org.intermine.metadata.*;

public class InterMine2OwlTest extends TestCase
{
    InterMine2Owl convertor;
    String ns = "http://www.intermine.org/model/testmodel#";

    public void setUp() {
        convertor = new InterMine2Owl();
    }


    public void testProcessClass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", ns, Collections.singleton(cld1));

        OntModel ont = convertor.process(model);
        assertNotNull(ont.getOntClass(ns + "Class1"));

    }

    public void testProcessInterface() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Interface1", null, true, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", ns, Collections.singleton(cld1));

        OntModel ont = convertor.process(model);
        assertNotNull(ont.getOntClass(ns + "Interface1"));
    }

    public void testProcessSupers() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Interface1", null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Interface2", "Interface1", true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class1", null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld4 = new ClassDescriptor("Class2", "Interface2 Class1", true, new HashSet(), new HashSet(), new HashSet());

        Model model = new Model("model", ns, new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3, cld4})));

        OntModel ont = convertor.process(model);
        assertNotNull(ont.getOntClass(ns + "Interface1"));
        assertNotNull(ont.getOntClass(ns + "Interface2"));
        assertNotNull(ont.getOntClass(ns + "Class1"));
        assertNotNull(ont.getOntClass(ns + "Class2"));
        OntClass ontCls1 = ont.getOntClass(ns + "Interface1");
        OntClass ontCls2 = ont.getOntClass(ns + "Interface2");
        OntClass ontCls3 = ont.getOntClass(ns + "Class1");
        OntClass ontCls4 = ont.getOntClass(ns + "Class2");
        assertTrue(ontCls2.hasSuperClass(ontCls1));
        assertTrue(ontCls4.hasSuperClass(ontCls2));
        assertTrue(ontCls4.hasSuperClass(ontCls3));
    }

    public void testProcessAttributes() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd2", "java.lang.Integer");
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(Arrays.asList(new Object[] {atd1, atd2})), new HashSet(), new HashSet());
        Model model = new Model("model", ns, Collections.singleton(cld1));

        OntModel ont = convertor.process(model);
        OntClass ontCls1 = ont.getOntClass(ns + "Class1");
        OntProperty ontProp1 = ont.getOntProperty(ns + "Class1__atd1");
        OntProperty ontProp2 = ont.getOntProperty(ns + "Class1__atd2");
        assertTrue(ontProp1.isDatatypeProperty());
        Iterator domains1 = ontProp1.listDomain();
        assertEquals(ontCls1, domains1.next());
        assertFalse(domains1.hasNext());
        Iterator ranges1 = ontProp1.listRange();
        Resource range1 = ont.getResource(OntologyUtil.XSD_NAMESPACE + "string");
        assertEquals(range1, ranges1.next());
        assertFalse(ranges1.hasNext());

        assertTrue(ontProp2.isDatatypeProperty());
        Iterator domains2 = ontProp2.listDomain();
        assertEquals(ontCls1, domains2.next());
        assertFalse(domains2.hasNext());
        Iterator ranges2 = ontProp2.listRange();
        Resource range2 = ont.getResource(OntologyUtil.XSD_NAMESPACE + "integer");
        assertEquals(range2, ranges2.next());
        assertFalse(ranges2.hasNext());
    }


    public void testProcessReferences() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class1", null);
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", "Class3", "rfd3");
        ReferenceDescriptor rfd3 = new ReferenceDescriptor("rfd3", "Class2", "rfd2");
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), Collections.singleton(rfd1), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), Collections.singleton(rfd2), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class3", null, false, new HashSet(), Collections.singleton(rfd3), new HashSet());
        Model model = new Model("model", ns, new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        OntModel ont = convertor.process(model);
        OntClass ontCls1 = ont.getOntClass(ns + "Class1");

        OntClass ontCls2 = ont.getOntClass(ns + "Class2");
        OntClass ontCls3 = ont.getOntClass(ns + "Class3");
        OntProperty ontProp1 = ont.getOntProperty(ns + "Class1__rfd1");
        OntProperty ontProp2 = ont.getOntProperty(ns + "Class2__rfd2");
        OntProperty ontProp3 = ont.getOntProperty(ns + "Class3__rfd3");

        assertTrue(ontProp1.isObjectProperty());
        Iterator domains1 = ontProp1.listDomain();
        assertEquals(ontCls1, domains1.next());
        assertFalse(domains1.hasNext());
        Iterator ranges1 = ontProp1.listRange();
        assertEquals(ontCls1, ranges1.next());
        assertFalse(ranges1.hasNext());
        assertTrue(OntologyUtil.hasMaxCardinalityOne(ont, ontProp1, ontCls1));
        assertFalse(ontProp1.hasInverse());

        assertTrue(ontProp2.isObjectProperty());
        Iterator domains2 = ontProp2.listDomain();
        assertEquals(ontCls2, domains2.next());
        assertFalse(domains2.hasNext());
        Iterator ranges2 = ontProp2.listRange();
        assertEquals(ontCls3, ranges2.next());
        assertFalse(ranges2.hasNext());
        assertTrue(OntologyUtil.hasMaxCardinalityOne(ont, ontProp2, ontCls2));

        assertTrue(ontProp3.isObjectProperty());
        assertTrue(OntologyUtil.hasMaxCardinalityOne(ont, ontProp3, ontCls3));
        assertTrue(ontProp3.isInverseOf(ontProp2));
    }

    public void testProcessCollections() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "Class1", null);
        CollectionDescriptor cod2 = new CollectionDescriptor("cod2", "Class3", "cod3");
        CollectionDescriptor cod3 = new CollectionDescriptor("cod3", "Class2", "cod2");
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), Collections.singleton(cod1), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), Collections.singleton(cod2), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class3", null, false, new HashSet(), Collections.singleton(cod3), new HashSet());
        Model model = new Model("model", ns, new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        OntModel ont = convertor.process(model);
        OntClass ontCls1 = ont.getOntClass(ns + "Class1");
        OntClass ontCls2 = ont.getOntClass(ns + "Class2");
        OntClass ontCls3 = ont.getOntClass(ns + "Class3");
        OntProperty ontProp1 = ont.getOntProperty(ns + "Class1__cod1");
        OntProperty ontProp2 = ont.getOntProperty(ns + "Class2__cod2");
        OntProperty ontProp3 = ont.getOntProperty(ns + "Class3__cod3");

        assertTrue(ontProp1.isObjectProperty());
        Iterator domains1 = ontProp1.listDomain();
        assertEquals(ontCls1, domains1.next());
        assertFalse(domains1.hasNext());
        Iterator ranges1 = ontProp1.listRange();
        assertEquals(ontCls1, ranges1.next());
        assertFalse(ranges1.hasNext());
        assertFalse(ontProp1.hasInverse());

        assertTrue(ontProp2.isObjectProperty());
        Iterator domains2 = ontProp2.listDomain();
        assertEquals(ontCls2, domains2.next());
        assertFalse(domains2.hasNext());
        Iterator ranges2 = ontProp2.listRange();
        assertEquals(ontCls3, ranges2.next());
        assertFalse(ranges2.hasNext());

        assertTrue(ontProp3.isObjectProperty());
        assertTrue(ontProp3.isInverseOf(ontProp2));
    }


    public void testGetOntClass() throws Exception {
        OntModel ont = ModelFactory.createOntologyModel();

        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", ns, Collections.singleton(cld1));
        OntClass ontCls1 = convertor.getOntClass(cld1, ont);
        assertNotNull(ontCls1);
        assertEquals(ns + "Class1", ontCls1.getURI().toString());
        OntClass ontCls2 = convertor.getOntClass(cld1, ont);
        assertNotNull(ontCls2);
        assertEquals(ns + "Class1", ontCls2.getURI().toString());
    }


    public void testGetObjectProperty() throws Exception {
        OntModel ont = ModelFactory.createOntologyModel();
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class1", null);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), Collections.singleton(rfd1), new HashSet());
        Model model = new Model("model", ns, Collections.singleton(cld1));
        ObjectProperty prop1 = convertor.getObjectProperty(rfd1, ont);
        assertNotNull(prop1);
        assertEquals(ns + "Class1__rfd1", prop1.getURI().toString());
        ObjectProperty prop2 = convertor.getObjectProperty(rfd1, ont);
        assertNotNull(prop2);
        assertEquals(ns + "Class1__rfd1", prop2.getURI().toString());
    }
}
