package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.bio.util.BioConverterUtil;
import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.DataConverterStoreHook;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * An implementation of DataConverterStoreHook that adds DataSet and DataSource references and
 * collections to Items as they are stored.
 * @author Julie Sullivan
 */
public class SOStoreHook implements DataConverterStoreHook
{
    private final Model model;
    private static final Map<String, String> SO_TERMS = new HashMap<String, String>();

    /**
     * Create a new DataSetStoreHook object.
     * @param model the data model
     */
    public SOStoreHook(Model model) {
        this.model = model;
    }

    /**
     * @see DataSetStoreHook#setDataSets(Item, Item, Item)
     * {@inheritDoc}
     */
    public void processItem(DataConverter dataConverter, Item item) {
        String soTermId = getSoTerm(dataConverter, item);
        setSOTerm(model, item, soTermId);
    }

    /**
     * Do the work of processItem() by setting DataSet and DataSource references and collections
     * on the given Item.
     * @param model the data model
     * @param item the Item to process
     * @param soTermId item id of the SO Term to add
     */
    public static void setSOTerm(Model model, Item item, String soTermId) {
        if (item.canHaveReference("sequenceOntologyTerm")
                && !item.hasReference("sequenceOntologyTerm")) {
            if (!StringUtils.isEmpty(soTermId)) {
                item.setReference("sequenceOntologyTerm", soTermId);
            }
        }
    }

    private static String getSoTerm(DataConverter dataConverter, Item item) {
        String soName = null;
        try {
            soName = BioConverterUtil.javaNameToSO(item.getClassName());
            if (soName == null) {
                return null;
            }
            String soRefId = SO_TERMS.get(soName);
            if (StringUtils.isEmpty(soRefId)) {
                Item soterm = dataConverter.createItem("SOTerm");
                soterm.setAttribute("name", soName);
                dataConverter.store(soterm);
                soRefId = soterm.getIdentifier();
                SO_TERMS.put(soName, soRefId);
            }
            return soRefId;
        } catch (IOException e) {
            return null;
        } catch (ObjectStoreException e) {
            return null;
        }
    }
}
