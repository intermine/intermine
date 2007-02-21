package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Read FlyBase GFF3 files are write out only those lines whose types are supported by the FlyMine
 * genomic model.
 *
 * @author Richard Smith
 */
public class FilterFlyBaseGFF3Task extends Task
{
    private FileSet fileSet;
    private File tgtDir;
    private Map seenIds = new HashMap();

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
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("fileSet must be specified");
        }
        if (tgtDir == null) {
            throw new BuildException("tgtDir must be specified");
        }

        try {
            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                File toRead = new File(ds.getBasedir(), files[i]);
                System.err .println("Processing file " + toRead.toString());

                String outName = toRead.getName();
                File out = new File(tgtDir, outName);
                BufferedWriter writer = new BufferedWriter(new FileWriter(out));
                filterGFF3(toRead, new BufferedReader(new FileReader(toRead)), writer);
                writer.flush();
                writer.close();
            }
        } catch (Exception e) {
            throw new BuildException (e);
        }
    }

    /**
     * Filter specific feature types out of the FlyBase GFF3.
     * @param in input GFF3
     * @param out GFF3 file to write
     * @throws IOException if problems reading/writing
     */
    private void filterGFF3(File file, BufferedReader in, BufferedWriter out) throws IOException {
        Iterator iter = GFF3Parser.parse(in);
      RECORD:
        while (iter.hasNext()) {
            GFF3Record record = (GFF3Record) iter.next();

            String chrId = record.getSequenceID();

            // ignore features with unusual sources
            if (file.getName().startsWith("dmel-") && record.getSource() != null
                && !record.getSource().equals("FlyBase")) {
                continue;
            }

            if (record.getSource() != null && record.getSource().startsWith("blast")) {
                // ignore records with this source because they have no parents
                continue RECORD;
            }

            if (typeToKeep(record.getType())) {
                // if the Name is just the ID plus "-hsp", set the ID to the Name and then remove
                // the Name
                if (record.getNames() != null && record.getNames().size() == 1
                    && record.getId() != null
                    && record.getId().equals(record.getNames().get(0) + "-hsp")) {
                    record.getAttributes().remove("ID");
                    record.getAttributes().put("ID", record.getNames());
                    record.getAttributes().remove("Name");
                }

                if (record.getId().equals("-")) {
                    record.setId(record.getType() + "_" + chrId + ":" + record.getStart() + ".."
                                 + record.getEnd());
                }

                if (seenIds.containsKey(record.getId())) {
                    // add -duplicate-2, -duplicate-3, etc. to any duplicated IDs
                    String oldId = record.getId();
                    int oldCount = ((Integer) seenIds.get(record.getId())).intValue();
                    record.setId(oldId + "-duplicate-" + oldCount);
                    seenIds.put(oldId, new Integer(oldCount + 1));

                } else {
                    seenIds.put(record.getId(), new Integer(1));
                }
                out.write(record.toGFF3() + "\n");
            }
        }
        out.flush();
    }

    private boolean typeToKeep(String type) {
        if (type.startsWith("match") || type.equals("aberration_junction")
            || type.startsWith("aa_") || type.startsWith("na_")
            || type.equals("DNA_motif") || type.equals("rescue_fragment")
            || type.equals("scaffold") || type.equals("golden_path_region")
            || type.equals("golden_path") || type.equals("golden_path_fragment")
            || type.equals("chromosome") || type.equals("mature_peptide")
            || type.equals("oligo") || type.equals("chromosome_arm")
            || type.equals("tRNAscan-SE") || type.equals("genie")
            || type.equals("augustus") || type.equals("genscan")
            || type.equals("RNAiHDP") || type.equals("repeatmasker")
            || type.equals("orthologous_region") || type.equals("protein_binding_site")
	    || type.equals("CDS") || type.equals("BAC_cloned_genomic_insert")) {
            return false;
        }
        return true;
    }
}
