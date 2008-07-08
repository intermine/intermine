package org.intermine.webservice.client.services;

import junit.framework.TestCase;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.webservice.client.util.TestUtil;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Jakub Kulaviak
 **/
public class ModelServiceTest extends TestCase
{

    public void testGetModel() {
        ModelService service = TestUtil.getModelService();
        Model model = service.getModel();
        assertNotNull(model);
        ClassDescriptor descriptor = model.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        assertNotNull(descriptor);
        assertNotNull(descriptor.getAttributeDescriptorByName("fullTime"));
    }
}
