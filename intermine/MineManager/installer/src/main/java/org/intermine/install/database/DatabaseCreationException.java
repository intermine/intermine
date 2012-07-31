package org.intermine.install.database;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;

/**
 * An exception for when the creation of a database fails.
 */
public class DatabaseCreationException extends DatabaseConnectionException
{
    private static final long serialVersionUID = 8439285352368910975L;

    /**
     * The exit code from the create database process, if relevant.
     * @serial
     */
    private Integer exitCode;

    /**
     * The output from the create database process, if relevant.
     * @serial
     */
    private String output;

    /**
     * The error output from the create database process, if relevant.
     * @serial
     */
    private String errorOutput;
    
    
    /**
     * Constructs a new instance with no message and the given root cause.
     * 
     * @param cause The original error that caused this.
     */
    public DatabaseCreationException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Constructs a new instance with the given message and root cause.
     * 
     * @param message The error message.
     * @param cause The original error that caused this.
     */
    public DatabaseCreationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new instance with the given process exit code, output and
     * error output.
     * 
     * @param exitCode The create process exit code.
     * @param output The standard output from the process.
     * @param error The error output from the process.
     */
    public DatabaseCreationException(Integer exitCode, String output, String error) {
        this(null, exitCode, output, error);
    }

    /**
     * Constructs a new instance with the given error message, process exit code,
     * output and error output.
     * 
     * @param message The error message.
     * @param exitCode The create process exit code.
     * @param output The standard output from the process.
     * @param error The error output from the process.
     */
    public DatabaseCreationException(String message, Integer exitCode,
                                     String output, String error) {
        super(message);
        this.exitCode = exitCode;
        this.output = output;
        this.errorOutput = error;
    }

    /**
     * Get the process exit code.
     * @return The exit code.
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Get the process output.
     * @return The output text.
     */
    public String getOutput() {
        return output;
    }

    /**
     * Get the process error output.
     * @return The error output text.
     */
    public String getErrorOutput() {
        return errorOutput;
    }

    /**
     * Create a human-readable representation of this exception.
     * @return A printable String.
     */
    @Override
    public String toString() {
        StringBuilder full = new StringBuilder();
        final String eol = System.getProperty("line.separator"); 
        
        full.append(super.toString()).append(eol);
        
        if (StringUtils.isNotEmpty(output)) {
            full.append("Process output: ").append(output).append(eol);
        }
        if (StringUtils.isNotEmpty(errorOutput)) {
            full.append("Process error: ").append(errorOutput).append(eol);
        }
        
        return full.toString();
    }
}
