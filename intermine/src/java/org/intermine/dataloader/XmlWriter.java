package org.flymine.dataloader;

import java.util.Collection;
import java.io.File;

import org.flymine.FlyMineException;

/**
 * Marshal business objects to XML.  Write either individual objects
 * or a collection of objects.
 *
 * @author Richard Smith
 */

public interface XmlWriter
{

    /**
     * Marshal a single object to the given XML file.
     *
     * @param obj a business object to marshal
     * @param file file to write XML to
     * @throws FlyMineException if anything goes wrong
     */
    public void writeXml(Object obj, File file) throws FlyMineException;


    /**
     * Marshal a collection of business objects to the given XML file.
     *
     * @param col a collection of business objects
     * @param file file to write XML to
     * @throws FlyMineException if anything goes wrong
     */
    public void writeXml(Collection col, File file) throws FlyMineException;

}
