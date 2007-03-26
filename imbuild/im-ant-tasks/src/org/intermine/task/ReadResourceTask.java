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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * A Task that reads a resource (from the class path) and sets a property with its contents.
 * It can set a property to be the contents of a file in the class path.
 * @author Kim Rutherford
 */
public class ReadResourceTask extends Task
{
    private String resource;
    private String property;
    private Path classpath;

    /**
     * Set the resource to search for.
     * @param resource the resource to look for in the class path.
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * The Ant property to set.
     * @param property the property
     */
    public void setProperty(String property) {
        this.property = property;
    }

    // copied from Ant's Property Task.
    /**
     * The classpath to use when looking up a resource.
     * @param classpath to add to any existing classpath
     */
    public void setClasspath(Path classpath) {
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * Create an empty Path.
     * @return the new Path
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Set the classpath to use when looking up a resourca,
     * given as reference to a Path defined elsewhere
     * @param reference the Reference
     */
    public void setClasspathRef(Reference reference) {
        createClasspath().setRefid(reference);
    }

    /**
     * Execute the task to set a property from a resource.
     * @throws BuildException if something goes wrong
     */
    public void execute() throws BuildException {
        if (resource == null) {
            throw new BuildException("no resource given");
        }
        if (property == null) {
            throw new BuildException("no property given");
        }

        ClassLoader classLoader = null;

        if (classpath != null) {
            classLoader = getProject().createClassLoader(classpath);
        } else {
            classLoader = this.getClass().getClassLoader();
        }

        InputStream is;
        if (classLoader == null) {
            is = ClassLoader.getSystemResourceAsStream(resource);
        } else {
            is = classLoader.getResourceAsStream(resource);
        }
        
        if (is == null) {
            throw new BuildException("cannot find resource \"" + resource + "\" in the classpath");
        }
        
        InputStreamReader reader = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                printWriter.println(line);
            }
        } catch (IOException e) {
            throw new BuildException("IO problem while reading resource \"" + resource + "\"", e);
        }
        printWriter.close();
        try {
            stringWriter.close();
        } catch (IOException e) {
            throw new BuildException("failed to close StringWriter", e);
        }
        getProject().setProperty(property, stringWriter.toString());
    }
}
