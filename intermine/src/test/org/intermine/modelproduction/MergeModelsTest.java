package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.Set;
import java.util.HashSet;
import java.io.StringReader;
import junit.framework.TestCase;
import org.intermine.metadata.ClassDescriptor;

import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.metadata.Model;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;


public class MergeModelsTest extends TestCase //XMLTestCase
{
    Model testModel;
    InterMineModelParser parser;


    public void setUp() throws Exception {
        parser = new InterMineModelParser();
        testModel = Model.getInstanceByName("testmodel");
    }
    
    public void testCloneClassDescriptor() throws Exception {
        ClassDescriptor cld = testModel.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        ClassDescriptor copy = ModelMerger.cloneClassDescriptor(cld);
        assertFalse("clone should not be reference to original", cld == copy);
        assertEquals(cld, copy);
    }
    
    public void testCloneAttributeDescriptors() throws Exception {
        ClassDescriptor cld = testModel.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        Set attrs = cld.getAttributeDescriptors();
        Set copy = ModelMerger.cloneAttributeDescriptors(attrs);
        assertEquals(attrs, copy);
    }
    
    public void testCloneCollectionDescriptors() throws Exception {
        ClassDescriptor cld = testModel.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set colls = cld.getCollectionDescriptors();
        Set copy = ModelMerger.cloneCollectionDescriptors(colls);
        assertEquals(colls, copy);
    }
    
    public void testCloneReferenceDescriptors() throws Exception {
        ClassDescriptor cld = testModel.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set refs = cld.getReferenceDescriptors();
        Set copy = ModelMerger.cloneReferenceDescriptors(refs);
        assertEquals(refs, copy);
    }
    
    public void testMergeReferences() throws Exception {
        
    }
    
}
