package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;


/**
 * Read a set of Uniprot XML files and write out only those elements for an organism in
 * the given set of names.
 *
 * @author Richard Smith
 */
public class UniprotFilterTask extends Task
{
    protected FileSet fileSet;
    protected File tgtDir;
    protected Set organisms = new HashSet();

    /**
     * Set the source fileset.
     * @param fileSet the fileset
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    /**
     * Set the target directory
     * @param tgtDir the target directory
     */
    public void setTgtDir(File tgtDir) {
        this.tgtDir = tgtDir;
    }

    /**
     * A space separated list of NCBI taxon ids to include in the filter output.
     * @param organismStr a comma separated list of organism names
     */
    public void setOrganisms(String organismStr) {
        StringTokenizer st = new StringTokenizer(organismStr, " ");
        while (st.hasMoreTokens()) {
            this.organisms.add(st.nextToken().trim());
        }
    }


    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("fileSet must be specified");
        } else {
            System.err.print("Fileset: " + fileSet.toString() + "\n");
        }

        if (tgtDir == null) {
            throw new BuildException("tgtDir must be specified");
        } else {
            System.err.print("TGT Dir: " + tgtDir.toString() + "\n");
        }

        if (organisms.isEmpty()) {
            System.err.print("No Organisms to Filter on - doing everything!" + "\n");
        } else {
            String spacer = " ";
            StringBuffer buff = new StringBuffer("Organism Filter List: ");
            for (Iterator it = organisms.iterator(); it.hasNext(); ) {
                buff.append(it.next() + spacer);
            }
            System.err.print(buff.toString() + "\n");
        }

        try {
            UniprotXmlFilter filter = new UniprotXmlFilter(organisms);
            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                File toRead = new File(ds.getBasedir(), files[i]);
                System.err .println("Processing file " + toRead.toString());

                String outName = toRead.getName().substring(0, toRead.getName().indexOf('.'))
                    + "_filtered.xml";
                File out = new File(tgtDir, outName);
                BufferedWriter writer = new BufferedWriter(new FileWriter(out));
                filter.filter(new BufferedReader(new FileReader(toRead)), writer);
                writer.flush();
                writer.close();

            }
        } catch (Exception e) {
            throw new BuildException (e);
        }
    }
}
