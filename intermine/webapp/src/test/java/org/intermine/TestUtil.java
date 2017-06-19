package org.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;

/**
 * @author Jakub Kulaviak
 **/
public class TestUtil
{

    public static Model getModel() {
        return Model.getInstanceByName("testmodel");
    }
    
    public static Map<String, List<FieldDescriptor>> getClassKeys(Model model) {
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(TestUtil.class.getClassLoader()
                                   .getResourceAsStream("class_keys.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Some IO error happened. ", e);
        }
        return ClassKeyHelper.readKeys(model, classKeyProps);
    }

    
}
