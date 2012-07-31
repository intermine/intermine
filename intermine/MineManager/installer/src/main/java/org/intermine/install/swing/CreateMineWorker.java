package org.intermine.install.swing;

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
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.intermine.common.swing.SystemProcessSwingWorker;
import org.intermine.install.properties.InterminePropertyKeys;


/**
 * Swing worker for running the <code>make_mine</code> script.
 */
public class CreateMineWorker extends SystemProcessSwingWorker
{
    /**
     * Failure codes for <code>CreateMineWorker</code> executions.
     */
    public enum FailureCode {
        
        /**
         * Code for indicating <code>make_mine</code> ran successfully.
         */
        SUCCESS,
        
        /**
         * Code for indicating the removal of the previous mine failed.
         */
        CLEAN_FAILURE,
        
        /**
         * Code for indicating <code>make_mine</code> failed.
         */
        CREATE_FAILURE
    }
    
    /**
     * Intermine home directory.
     */
    private File intermineHome;
    
    /**
     * The name for the mine.
     */
    private String mineName;
    
    /**
     * The directory for the mine.
     */
    private File mineHome;

    /**
     * The failure code from the <code>make_mine</code> execution.
     */
    private FailureCode failureCode = FailureCode.SUCCESS;

    
    /**
     * Initialise with the given mine creation properties.
     * 
     * @param props The mine properties.
     */
    public CreateMineWorker(Properties props) {
        mineName = props.getProperty(InterminePropertyKeys.MINE_NAME);
        assert mineName != null : "No mine name";
        
        mineHome = (File) props.get(InterminePropertyKeys.MINE_HOME);
        assert mineHome != null : "No mine home directory";
        
        intermineHome = (File) props.get(InterminePropertyKeys.INTERMINE_HOME);
        assert intermineHome != null : "No intermine home directory";
        
        String[] commands = {
                "bio/scripts/make_mine",
                mineName
            };

        initialise(commands, intermineHome, true);
    }

    /**
     * Get the failure code from <code>make_mine</code> execution.
     * 
     * @return The failure code.
     */
    public FailureCode getFailureCode() {
        return failureCode;
    }

    /**
     * Code executed before the <code>make_mine</code> external process
     * is launched. The previous mine of the same name is deleted. Sets
     * the failure code to <code>CLEAN_FAILURE</code> if the clean fails.
     * 
     * @throws IOException if the directory clean fails.
     */
    @Override
    protected void preamble() throws IOException {
        failureCode = FailureCode.CREATE_FAILURE;
        
        try {
            FileUtils.deleteDirectory(mineHome);
        } catch (IOException e) {
            failureCode = FailureCode.CLEAN_FAILURE;
            throw e;
        }
    }

    /**
     * Code executed after the <code>make_mine</code> external process
     * fails. If the process ends with zero as its exit code, and there
     * is no exception that has halted execution, the failure code is changed
     * to <code>SUCCESS</code>.
     */
    @Override
    protected void cleanup() {
        if (Integer.valueOf(0).equals(exitCode) && haltingException == null) {
            failureCode = FailureCode.SUCCESS;
        }
    }
}
