package org.intermine.task;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;

import java.lang.reflect.Constructor;

/**
 * Generic defn of a post process step...
 * @author Peter Mclaren
 * @author Richard Smith
 * */
public class PostProcessorTask extends DynamicAttributeTask
{

    protected String clsName;
    protected String osName;

    /**
     * Set the name of the PostProcessor sub-class to load to do the postprocessing.
     * @param clsName the class name
     */
    public void setClsName(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Set the ObjectStore alias.
     * @param osName the ObjectStore alias
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }


    /**
     * Run the task
     * @throws org.apache.tools.ant.BuildException if a problem occurs
     */
    public void execute() {

        ObjectStoreWriter osw = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);

            Class c = Class.forName(clsName);

            if (!PostProcessor.class.isAssignableFrom(c)) {
                throw new IllegalArgumentException("Class (" + clsName + ") is not a subclass"
                                             + "of org.intermine.postprocess.PostProcessor!");
            }

            Constructor m = c.getConstructor(new Class[] {ObjectStoreWriter.class});

            PostProcessor pp = (PostProcessor) m.newInstance(new Object[] {osw});

            configureDynamicAttributes(pp);

            pp.postProcess();

        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try {
                if (osw != null) {
                    // NOTE this is a hack to work around running out of database connections when
                    // running the do-sources postprocess step.  Each source that has a postprocess
                    // step is run in a separate ClassLoader which means they are unable to share
                    // the Database instance held in DatabaseFactory.  Database connections aren't
                    // closed automatically until the JVM exits and ShutdownHook is called.
                    // Here we're closing the database explicitly, a better fix would be to make
                    // sure each do-sources step used the same ClassLoader in im-ant-tasks
                    // PostProcessTask.
                    if (ObjectStoreWriterInterMineImpl.class.isAssignableFrom(osw.getClass())) {
                        ((ObjectStoreWriterInterMineImpl) osw).getDatabase().shutdown();
                    }
                    osw.close();
                }
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
    }

}
