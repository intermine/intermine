package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.util.TypeUtil;

/**
 * Class to represent a reference field of an object for display in the webapp
 * @author Mark Woodbridge
 */
public class DisplayReference
{
    int id;
    Set clds;
    Map identifiers = new HashMap();

    /**
     * Constructor
     * @param o the referenced object
     * @param model the metadata for the object
     * @throws Exception if an error occurs
     */
    public DisplayReference(InterMineObject o, Model model) throws Exception {
        id = o.getId().intValue();
        clds = ObjectViewController.getLeafClds(o.getClass(), model);
        Set pks = PrimaryKeyUtil.getPrimaryKeyFields(model, o.getClass());
        for (Iterator i = pks.iterator(); i.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
            if (fd.isAttribute()) {
                Object fieldValue = TypeUtil.getFieldValue(o, fd.getName());
                identifiers.put(fd.getName(), fieldValue);
            }
        }
    }

    /**
     * Get the id of the object
     * @return the id
     */
    public int getId() {
        return id;
    }
    
    /**
     * Get the clds of the object
     * @return the clds
     */
    public Set getClds() {
        return clds;
    }
    
    /**
     * Get the identifier fields and values for the object
     * @return the identifiers
     */
    public Map getIdentifiers() {
        return identifiers;
    }
}