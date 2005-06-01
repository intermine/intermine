package org.intermine.ontology;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.intermine.metadata.*;
import org.intermine.util.TypeUtil;

import com.hp.hpl.jena.ontology.OntModel;

public class OwlFunctionalTest extends TestCase
{
    public void testRoundTrip() throws Exception {
        Model original = Model.getInstanceByName("testmodel");
        InterMine2Owl i2o = new InterMine2Owl();
        Model newModel = primitivesToObjectsModel(original);
        OntModel ont = i2o.process(newModel);
        Owl2InterMine o2i = new Owl2InterMine(original.getName(), original.getPackageName());
        assertEquals(newModel, o2i.process(ont, original.getNameSpace().toString()));
    }

    /**
     * converts primitives to corresponding java.lang objects
     * removes all primary key information
     * makes all classes interfaces
     */
    private Model primitivesToObjectsModel(Model model) throws Exception {
        Set classes = new HashSet();

        Iterator i = model.getClassDescriptors().iterator();
        while (i.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) i.next();
            Set atds = new HashSet();
            Set rfds = new HashSet();
            Set cods = new HashSet();

            Iterator j = cld.getAttributeDescriptors().iterator();
            while (j.hasNext()) {
                AttributeDescriptor atd = (AttributeDescriptor) j.next();

                AttributeDescriptor newAtd = new AttributeDescriptor(atd.getName(),
                                                                     TypeUtil.instantiate(atd.getType()).getName());
                atds.add(newAtd);
            }
            j = cld.getReferenceDescriptors().iterator();
            while (j.hasNext()) {
                ReferenceDescriptor rfd = (ReferenceDescriptor) j.next();

                String reverseRef = null;
                if (rfd.getReverseReferenceDescriptor() != null) {
                    reverseRef = rfd.getReverseReferenceDescriptor().getName();
                }
                ReferenceDescriptor newRfd = new ReferenceDescriptor(rfd.getName(),
                                                                     rfd.getReferencedClassDescriptor().getName(),
                                                                     reverseRef);
                rfds.add(newRfd);
            }
            j = cld.getCollectionDescriptors().iterator();
            while (j.hasNext()) {
                CollectionDescriptor cod = (CollectionDescriptor) j.next();

                String reverseRef = null;
                if (cod.getReverseReferenceDescriptor() != null) {
                    reverseRef = cod.getReverseReferenceDescriptor().getName();
                }
                CollectionDescriptor newCod = new CollectionDescriptor(cod.getName(),
                                                                       cod.getReferencedClassDescriptor().getName(),
                                                                       reverseRef,
                                                                       true);
                cods.add(newCod);
            }

            String supers = "";
            Iterator k = cld.getSuperDescriptors().iterator();
            while (k.hasNext()) {
                supers = supers + ((ClassDescriptor) k.next()).getName() + " ";
            }
            ClassDescriptor newCld = new ClassDescriptor(cld.getName(), supers.equals("") ? null : supers.trim(), true, atds, rfds, cods);
            classes.add(newCld);
        }
        Model newModel = new Model(model.getName(), model.getNameSpace().toString(), classes);
        return newModel;
    }
}
