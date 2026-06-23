package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.webservice.server.core.ResultProcessor;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.xml.full.FullRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A result processor for Items XML.
 * @author See version control
 *
 */
public class ItemsXMLProcessor extends ResultProcessor
{
    private final InterMineAPI im;

    /**
     * Constructor
     * @param im API object used to access data model
     *
     */
    public ItemsXMLProcessor(InterMineAPI im) {
        this.im = im;
    }

    @Override
    public void write(Iterator<List<ResultElement>> resultIt, Output output) {

        Model model = im.getModel();

        while (resultIt.hasNext())  {
            Map<String, FastPathObject> objMap = new HashMap<>();
            Map<String, Set<String>> fieldsMap = new HashMap<>();

            ClassDescriptor currentClassDesc = null;
            String currentClassName = null;

            // Get the next row
            List<ResultElement> elements = resultIt.next();

            // Iterate over cells in the row
            for (ResultElement element : elements) {
                Path path = element.getPath();

                ClassDescriptor classDescriptor = path.getLastClassDescriptor();

                if (currentClassDesc == null || currentClassDesc != classDescriptor) {
                    currentClassDesc = classDescriptor;
                    currentClassName = currentClassDesc.getName();

                    FastPathObject obj = element.getObject();

                    // putIfAbsent may not be necessary here
                    objMap.putIfAbsent(currentClassName, obj);
                    fieldsMap.putIfAbsent(currentClassName, new HashSet<>());

                    String stringPath = path.toString();
                    stringPath = stringPath.substring(0, stringPath.lastIndexOf("."));

                    try {
                        Path partialPath = new Path(im.getModel(), stringPath);
                        if (partialPath.endIsReference() || partialPath.endIsCollection()) {
                            FieldDescriptor fd = partialPath.getEndFieldDescriptor();
                            String parentClassName =
                                    partialPath.getSecondLastClassDescriptor().getName();
                            Set<String> parentFields = fieldsMap.get(parentClassName);
                            parentFields.add(fd.getName());
                        }
                    } catch (PathException pe) {
                        throw new BadRequestException(stringPath + " is not a valid path");
                    }
                }

                FieldDescriptor fd = path.getEndFieldDescriptor();
                fieldsMap.get(currentClassName).add(fd.getName());
            }

            for (Map.Entry<String, Set<String>> entry : fieldsMap.entrySet()) {
                String name = entry.getKey();
                Set<String> includeFields = entry.getValue();
                FastPathObject obj = objMap.get(name);

                String xml = FullRenderer.render(obj, model, includeFields);

                List<String> xmlList = new ArrayList<String>();
                xmlList.add(xml);

                output.addResultItem(xmlList);
            }
        }
    }
}
