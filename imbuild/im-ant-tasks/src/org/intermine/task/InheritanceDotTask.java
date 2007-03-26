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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Task that creates a DOT file describing the inheritance tree of java classes
 * in the classpath.
 *
 * @author Matthew Wakeling
 */
public class InheritanceDotTask extends Task
{
    protected String directory, packageName, file;
    protected Set omit, boring;
    
    /**
     * Set the directory containing java source files, such as one might use in
     * the sourceclasspath.
     *
     * @param directory the directory containing the java files
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * Set the package name to describe. All classes present in the given package and sub-packages
     * and present in the given source directory will be described.
     *
     * @param packageName the package to describe
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Set the name of the new dot file.
     *
     * @param file the new dot file
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Set a list of classnames to omit from the graph
     *
     * @param omit a comma-separated String
     */
    public void setOmit(String omit) {
        this.omit = new HashSet();
        String[] omitArray = omit.split(",");
        for (int i = 0; i < omitArray.length; i++) {
            this.omit.add(omitArray[i]);
        }
    }

    /**
     * Set a list of classnames that are boring - all classes that only extend one of these will be
     * omitted from the graph.
     *
     * @param boring a comma-separated String
     */
    public void setBoring(String boring) {
        this.boring = new HashSet();
        String[] boringArray = boring.split(",");
        for (int i = 0; i < boringArray.length; i++) {
            this.boring.add(boringArray[i]);
        }
    }
    
    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(file));
            File dir = new File(directory);
            String packageBits[] = packageName.split("\\.");
            for (int i = 0; i < packageBits.length; i++) {
                dir = new File(dir, packageBits[i]);
            }
            List classNames = new ArrayList();
            getAllClassNames(packageName, dir, classNames);

            out.println("digraph G {\nrankdir=LR\nconcentrate=true\noverlap=scale");
            Set done = new HashSet();
            Iterator iter = classNames.iterator();
            while (iter.hasNext()) {
                String className = (String) iter.next();
                maybeDoName(className, done, out);
            }
            out.println("}");
            out.flush();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private void getAllClassNames(String packageName, File dir, List classNames) {
        String[] list = dir.list();
        for (int i = 0; i < list.length; i++) {
            if (list[i].endsWith(".java")) {
                classNames.add(packageName + "." + list[i].substring(0, list[i].length() - 5));
            } else {
                File newFile = new File(dir, list[i]);
                if (newFile.isDirectory()) {
                    getAllClassNames(packageName + "." + list[i], newFile, classNames);
                }
            }
        }
    }

    private void maybeDoName(String className, Set done, PrintWriter out) {
        try {
            if (!omit.contains(className)) {
                Class clazz = Class.forName(className);
                maybeDo(clazz, done, out, false);
            }
        } catch (ClassNotFoundException e) {
            System .err.println("Error handling class name " + className);
            //e.printStackTrace(System.err);
            System.err.flush();
        } catch (ExceptionInInitializerError e) {
            System .err.println("Error handling class name " + className);
            //e.printStackTrace(System.err);
            System.err.flush();
        } catch (LinkageError e) {
            System .err.println("Error handling class name " + className);
            //e.printStackTrace(System.err);
            System.err.flush();
        }
    }

    private void maybeDo(Class clazz, Set done, PrintWriter out, boolean alwaysDo) {
        if (!done.contains(clazz)) {
            Class superClass = clazz.getSuperclass();
            Class[] interfaces = clazz.getInterfaces();
            if (alwaysDo || (superClass == null) || (!boring.contains(superClass.getName()))
                    || (interfaces.length > 0)) {
                if ((superClass != null) && (!omit.contains(superClass.getName()))) {
                    maybeDo(superClass, done, out, true);
                }
                for (int i = 0; i < interfaces.length; i++) {
                    if (!omit.contains(interfaces[i].getName())) {
                        maybeDo(interfaces[i], done, out, true);
                    }
                }
                out.println("\"" + clazz.getName() + "\" ["
                        + (clazz.isInterface() ? "" : "shape=box,")
                        + (clazz.getName().startsWith(packageName + ".")
                            ? "style=filled,fillcolor=green"
                            : (clazz.getName().startsWith("org.")
                                || clazz.getName().startsWith("junit.")
                                || clazz.getName().startsWith("net.")
                                || clazz.getName().startsWith("servletunit.")
                                ? "style=filled,fillcolor=red" : "style=filled,fillcolor=white"))
                        + "]");
                if ((superClass != null) && (!omit.contains(superClass.getName()))) {
                    out.println("\"" + superClass.getName() + "\" -> \"" + clazz.getName()
                            + "\" [arrowhead=none,arrowtail=normal]");
                }
                for (int i = 0; i < interfaces.length; i++) {
                    if (!omit.contains(interfaces[i].getName())) {
                        out.println("\"" + interfaces[i].getName() + "\" -> \"" + clazz.getName()
                                + "\" [arrowhead=none,arrowtail=empty,style=dashed,color=purple]");
                    }
                }
                done.add(clazz);
            }
        }
    }
}
