package org.intermine.task;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * A DirectDataLoaderTask for files.
 *
 * @author Kim Rutherford
 */

public abstract class FileDirectDataLoaderTask extends DirectDataLoaderTask
{
    protected List<FileSet> fileSets = new ArrayList<FileSet>();

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
        int fileCount = 0;
        for (FileSet fileSet : fileSets) {
            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();

            for (int i = 0; i < files.length; i++) {
                File file = new File(ds.getBasedir(), files[i]);
                processFile(file);
                fileCount++;
            }
        }
        if (fileCount == 0) {
            StringBuffer sb = new StringBuffer();
            String lookedIn = null;
            for (FileSet fileSet : fileSets) {
                sb.append(System.getProperty("line.separator") + "\t"
                        + fileSet.getDir(getProject()).getAbsolutePath());
            }
            if (sb.length() == 0) {
                lookedIn = "[No directories found]";
            } else {
                lookedIn = sb.toString();
            }

            throw new RuntimeException("Failed to find any files to process for source: "
                    + sourceName + " looked in: " + lookedIn);
        }
    }

    /**
     * Called by process() once for each File we need to process.  This should be implemented in
     * the sub-classes to call DirectDataLoader.createObject() and DirectDataLoader.store().
     * @param file the File
     */
    public abstract void processFile(File file);
}
