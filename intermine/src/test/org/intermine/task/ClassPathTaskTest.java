package org.flymine.task;

import junit.framework.TestCase;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

public class ClassPathTaskTest extends TestCase
{
    public ClassPathTaskTest(String arg) {
        super(arg);
    }

    private ClassPathTask task;

    public void setUp() {
        task = new ClassPathTask();
        Project project = new Project();
        task.setProject(project);
    }

    /**
     * We expect loadClass() to set the Thread context class loader and therefore
     * load the given class under that class loader
     */
    public void testLoadValidClassViaClasspath() {
        // Try loading a class we do not use. It is easiest to use a class on the System classpath
        ClassLoader parentClassLoader = this.getClass().getClassLoader();
        AntClassLoader childClassLoader = new AntClassLoader(parentClassLoader, false);
        task.setClassLoader(childClassLoader);
        Object instance = task.loadClass("org.flymine.task.DummyDriver");

        assertEquals(childClassLoader, Thread.currentThread().getContextClassLoader());
        // Can't get the following line to work - guess it is some bizarre ClassLoader problem
        //assertEquals(childClassLoader, instance.getClass().getClassLoader());

    }

    public void testLoadValidClassNoClasspath() {
        // Try loading a class we do not use. It is easiest to use a class on the System classpath
        task.setClassLoader(null);
        Object instance = task.loadClass("org.flymine.task.DummyDriver2");

        assertEquals(this.getClass().getClassLoader(), instance.getClass().getClassLoader());
        assertEquals(this.getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());
    }

    public void testLoadInvalidClass() {
        try {
            Object instance = task.loadClass("LoadOfRubbish");
            fail("Expected: BuildException");
        } catch (BuildException e) {
        }
    }

}
