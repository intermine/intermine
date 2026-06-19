package org.intermine.webservice.server.output;

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
import org.intermine.xml.full.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemsXMLProcessor extends ResultProcessor
{
    private final InterMineAPI im;

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
                // System.out.println("Processing: " + path);

                ClassDescriptor classDescriptor = path.getLastClassDescriptor();

                if (currentClassDesc == null || currentClassDesc != classDescriptor) {
                    currentClassDesc = classDescriptor;
                    currentClassName = currentClassDesc.getName();

                    // System.out.println("New currentClass: " + currentClassName);

                    FastPathObject obj = element.getObject();

                    // putIfAbsent may not be necessary here
                    objMap.putIfAbsent(currentClassName, obj);
                    // System.out.println("Added " + currentClassName + " to objMap");
                    fieldsMap.putIfAbsent(currentClassName, new HashSet<>());
                    // System.out.println("Added " + currentClassName + " to fieldsMap");

                    String stringPath = path.toString();
                    stringPath = stringPath.substring(0, stringPath.lastIndexOf("."));

                    try {
                        Path partialPath = new Path(im.getModel(), stringPath);
                        if (partialPath.endIsReference() || partialPath.endIsCollection()) {
                            FieldDescriptor fd = partialPath.getEndFieldDescriptor();
                            String parentClassName =
                                    partialPath.getSecondLastClassDescriptor().getName();
                            // System.out.println("Adding " + fd.getName() + " to " + parentClassName + " properties");
                            Set<String> parentFields = fieldsMap.get(parentClassName);
                            parentFields.add(fd.getName());
                        }
                    } catch (PathException pe) {
                        throw new BadRequestException(stringPath + " is not a valid path");
                    }
                }

                FieldDescriptor fd = path.getEndFieldDescriptor();
                // System.out.println("Adding " + fd.getName() + " to " + currentClassName + " properties");
                fieldsMap.get(currentClassName).add(fd.getName());
            }

            for (Map.Entry<String, Set<String>> entry : fieldsMap.entrySet()) {
                String name = entry.getKey();
                Set<String> includeFields = entry.getValue();
                FastPathObject obj = objMap.get(name);

                String xml = FullRenderer.render(obj, model, includeFields);

                // System.out.println("Adding XML for: " + name);
                // System.out.println(xml);

                List<String> xml_list = new ArrayList<String>();
                xml_list.add(xml);

                output.addResultItem(xml_list);
            }
        }
    }
}
