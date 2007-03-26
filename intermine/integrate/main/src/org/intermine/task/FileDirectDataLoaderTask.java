package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.io.File;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * A DirectDataLoaderTask for files.
 *
 * @author Kim Rutherford
 */

public abstract class FileDirectDataLoaderTask extends DirectDataLoaderTask
{
    protected List fileSets = new ArrayList();

    /**
     * Add a FileSet to read from.
     * @param fileSet the FileSet
     */
    public void addFileSet(FileSet fileSet) {
        fileSets.add(fileSet);
    }

    /**
     * @see DirectDataLoaderTask#process()
     */
    public void process() {
        for (Iterator fileSetIter = fileSets.iterator(); fileSetIter.hasNext();) {
            FileSet fileSet = (FileSet) fileSetIter.next();

            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();

            for (int i = 0; i < files.length; i++) {
                File file = new File(ds.getBasedir(), files[i]);
                processFile(file);
            }
        }
    }

    /**
     * Called by process() once for each File we need to process.  This should be implemented in
     * the sub-classes to call DirectDataLoader.createObject() and DirectDataLoader.store().
     * @param file the File
     */
    public abstract void processFile(File file);
}
