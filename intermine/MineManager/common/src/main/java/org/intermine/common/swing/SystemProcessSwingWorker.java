package org.intermine.common.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.List;

import javax.swing.SwingWorker;

/**
 * SwingWorker implementation that runs a system process.
 * 
 * @see Process
 * @see ProcessBuilder
 */
public class SystemProcessSwingWorker extends SwingWorker<Integer, Void>
{
    /**
     * Name of the property fired when the process is started.
     */
    public static final String STARTED = "processStarted";
    
    /**
     * Name of the property fired when some output from the process has
     * been read. The new value of the property will be the <u>new</u>
     * characters read.
     */
    public static final String OUTPUT = "outputStream";
    
    /**
     * Name of the property fired when some error output from the process has
     * been read. The new value of the property will be the <u>new</u>
     * characters read.
     */
    public static final String ERROR = "errorStream";
    
    /**
     * Name of the property fired when the process completes or dies.
     */
    public static final String COMPLETE = "processComplete";
    
    /**
     * The process builder.
     */
    private ProcessBuilder processBuilder;
    
    /**
     * Flag indicating that the output and error streams of the process should
     * be linked to produce only one stream of output.
     */
    private boolean linkedStreams;
    
    /**
     * The amount of time between attempts to read from the output and error streams
     * and testing for process completion (milliseconds). Default is 250ms.
     */
    private long pollingInterval = 250;
    
    
    /**
     * If the process dies and causes and exception to be generated, that exception
     * is recorded in this attribute.
     */
    protected Throwable haltingException;
    
    /**
     * The exit code of the process. May be null if no such code was available.
     */
    protected Integer exitCode;

    /**
     * Internal flag indicating the process has been started. Once set, this
     * SystemProcessSwingWorker cannot be reinitialised.
     */
    private volatile boolean started;

    
    /**
     * Create a new, uninitialised SystemProcessSwingWorker. It must be initialised
     * with a call to an <code>initialise</code> method before it can be executed. 
     */
    public SystemProcessSwingWorker() {
    }
    
    /**
     * Create a new SystemProcessSwingWorker with the given commands.
     * 
     * @param commands The collection of commands and its arguments. 
     * @param execDir The directory to start the child process in.
     * @param redirectError Whether to combine the error output from the process into
     * the standard output.
     */
    public SystemProcessSwingWorker(List<String> commands, File execDir, boolean redirectError) {
        initialise(commands, execDir, redirectError);
    }
    
    /**
     * Create a new SystemProcessSwingWorker with the given commands.
     * 
     * @param commands The collection of commands and its arguments. 
     * @param execDir The directory to start the child process in.
     * @param redirectError Whether to combine the error output from the process into
     * the standard output.
     */
    public SystemProcessSwingWorker(String[] commands, File execDir, boolean redirectError) {
        initialise(commands, execDir, redirectError);
    }
    
    /**
     * Initialise this SystemProcessSwingWorker with the given commands.
     * 
     * @param commands The collection of commands and its arguments. 
     * @param execDir The directory to start the child process in.
     * @param redirectError Whether to combine the error output from the process into
     * the standard output.
     */
    public void initialise(List<String> commands, File execDir, boolean redirectError) {
        String[] array = new String[commands.size()];
        commands.toArray(array);
        initialise(array, execDir, redirectError);
    }
    
    /**
     * Initialise this SystemProcessSwingWorker with the given commands.
     * 
     * @param commands The collection of commands and its arguments. 
     * @param execDir The directory to start the child process in.
     * @param redirectError Whether to combine the error output from the process into
     * the standard output.
     */
    public synchronized void initialise(String[] commands, File execDir, boolean redirectError) {
        if (started) {
            throw new IllegalStateException(
                    "This SystemProcessSwingWorker has been started and cannot be reinitialised.");
        }
        
        processBuilder = new ProcessBuilder(commands);
        if (execDir != null) {
            processBuilder.directory(execDir);
        }
        processBuilder.redirectErrorStream(redirectError);
        linkedStreams = redirectError;
    }

    /**
     * Get the amount of time between attempts to read from the output and error streams
     * and testing for process completion (milliseconds).
     * <p>The default for this property is is 250ms.</p>
     * 
     * @return The polling interval.
     */
    public long getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Set the amount of time between attempts to read from the output and error streams
     * and testing for process completion (milliseconds).
     * 
     * @param pollingInterval The polling interval.
     */
    public void setPollingInterval(long pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    /**
     * Get the exception that was raised when the child process died, if applicable.
     * <p>Thread interruption exceptions are not recorded ({@link InterruptedException},
     * {@link InterruptedIOException}).</p>
     * 
     * @return The causal Throwable, or <code>null</code> if there is no such error.
     */
    public Throwable getHaltingException() {
        return haltingException;
    }

    /**
     * Get the exit code of the child process.
     * 
     * @return The exit code, or <code>null</code> if this was never returned.
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Check whether the output and error streams of the child process are combined into
     * one.
     * 
     * @return <code>true</code> if the streams are linked, <code>false</code> if they
     * are separate.
     */
    public boolean areStreamsLinked() {
        return linkedStreams;
    }

    /**
     * Launch the child process and read its output until it finishes.
     * <code>PropertyChangeEvent</code>s are raised while the process is running:
     * 
     * <ol>
     * <li><code>STARTED</code> as the process starts.
     * <li><code>OUTPUT</code> if there are characters read from the process's
     * output stream.
     * <li><code>ERROR</code> if there are characters read from the process's
     * error stream. This event will not be raised if the error stream is linked
     * to the output stream.
     * <li><code>COMPLETE</code> when the process ends by any means.
     * </ol>
     * 
     * @return The exit code of the process, if there is one.
     */
    @Override
    protected Integer doInBackground() {
        
        synchronized (this) {
            if (processBuilder == null) {
                throw new IllegalStateException("System process has not been initialised");
            }
            started = true;
        }
        
        try {
            preamble();
            
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            
            Process process = processBuilder.start();
            firePropertyChange(STARTED, false, true);
            try {
                BufferedReader stdin =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = null;
                if (!linkedStreams) {
                    stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                }
                boolean done = false;
                
                while (true) {
                    try {
                        boolean outputChanged = false;
                        output.setLength(0);
                        while (stdin.ready()) {
                            char c = (char) stdin.read();
                            output.append(c);
                            outputChanged = true;
                        }
                        
                        boolean errorChanged = false;
                        error.setLength(0);
                        if (!linkedStreams) {
                            while (stderr.ready()) {
                                char c = (char) stderr.read();
                                error.append(c);
                                errorChanged = true;
                            }
                        }
                        
                        if (outputChanged) {
                            firePropertyChange(OUTPUT, null, output.toString());
                        }
                        if (errorChanged) {
                            firePropertyChange(ERROR, null, error.toString());
                        }
                        
                        if (done) {
                            break;
                        }
                        
                        exitCode = process.exitValue();
                        // If this succeeds, we're done.
                        // Needs a final loop to suck in any remaining characters in the streams.
                        done = true;
    
                    } catch (IllegalThreadStateException e) {
                        // Process not done, so wait and continue.
                        Thread.sleep(pollingInterval);
                    }
                }
            } finally {
                try {
                    process.exitValue();
                } catch (IllegalThreadStateException e) {
                    // Not finished, but something has failed.
                    process.destroy();
                }
                
                cleanup();
            }
        } catch (InterruptedException e) {
            // Nothing.
        } catch (InterruptedIOException e) {
            // Nothing.
        } catch (Throwable e) {
            haltingException = e;
        } finally {
            firePropertyChange(COMPLETE, false, true);
        }

        return exitCode;
    }

    /**
     * Optional pre-execution method subclasses can override if they wish to
     * perform some action before the child process starts.
     * 
     * @throws Exception if there is an error of any type.
     */
    protected void preamble() throws Exception {
    }
    
    /**
     * Optional post-execution method subclasses can override if they wish to
     * perform some action after the child process ends. This method is called
     * regardless of how the process ends.
     * 
     * @throws Exception if there is an error of any type.
     */
    protected void cleanup() throws Exception {
    }
}
