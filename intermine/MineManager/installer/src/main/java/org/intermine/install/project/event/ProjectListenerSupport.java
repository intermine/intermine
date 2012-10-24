package org.intermine.install.project.event;

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
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Source;

/**
 * Support class for <code>ProjectEvent</code>s. Handles the registration of listeners
 * and the firing of events, in the model of <code>PropertyChangeSupport</code>.
 *
 * @see java.beans.PropertyChangeSupport
 */
public class ProjectListenerSupport implements Serializable
{
    private static final long serialVersionUID = -3513564997012695410L;
    
    /**
     * The object that will be the source of ProjectEvents.
     * Typically, this will be the creator of this object.
     * @serial
     */
    private Object eventSource;
    
    /**
     * Logger.
     */
    private transient Log logger;
    
    /**
     * The registered <code>ProjectListener</code>s to this object.
     */
    private transient List<ProjectListener> listeners;
    
    /**
     * A cache of the <code>Method</code>s fetched from the
     * <code>ProjectListener</code> interface by reflection.
     */
    private transient Map<String, Method> methodCache;
    
    
    /**
     * Create a new ProjectListenerSupport object.
     * 
     * @param source The source for raised ProjectEvents. Typically, the creator
     * of this object.
     */
    public ProjectListenerSupport(Object source) {
        eventSource = source;
        logger = LogFactory.getLog(eventSource.getClass());
    }
    
    /**
     * Deserialization method.
     * 
     * @serialData Recreates the transient logger.
     * 
     * @param in The deserializing ObjectInputStream.
     * 
     * @throws IOException if there is an I/O problem.
     * @throws ClassNotFoundException if there is a missing class.
     */
    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        logger = LogFactory.getLog(eventSource.getClass());
    }
    
    /**
     * Add the given project listener.
     * 
     * @param l The ProjectListener. If <code>null</code>, no action is taken.
     */
    public void addProjectListener(ProjectListener l) {
        if (l != null) {
            if (listeners == null) {
                listeners = new ArrayList<ProjectListener>();
            }
            listeners.add(l);
        }
    }
    
    /**
     * Remove the given project listener. If it is not registered, nothing
     * happens.
     * 
     * @param l The ProjectListener. If <code>null</code>, no action is taken.
     */
    public void removeProjectListener(ProjectListener l) {
        if (l != null && listeners != null) {
            listeners.remove(l);
        }
    }
    
    /**
     * Remove all registered project listeners.
     */
    public void clearProjectListeners() {
        if (listeners != null) {
            listeners.clear();
        }
    }
    
    /**
     * Fire the <code>projectCreated</code> message.
     * 
     * @param projectFile The newly created project's <code>project.xml</code> file.
     * 
     * @see ProjectListener#projectCreated(ProjectEvent)
     */
    public void fireProjectCreated(File projectFile) {
        if (willFire()) {
            ProjectEvent event = new ProjectEvent(eventSource, projectFile);
            fireEvent("projectCreated", event);
        }
    }
    
    /**
     * Fire the <code>projectLoaded</code> message.
     * 
     * @param project The Project.
     * @param projectFile The project's <code>project.xml</code> file.
     * 
     * @see ProjectListener#projectLoaded(ProjectEvent)
     */
    public void fireProjectLoaded(Project project, File projectFile) {
        if (willFire()) {
            ProjectEvent event = new ProjectEvent(eventSource, project, projectFile);
            fireEvent("projectLoaded", event);
        }
    }
    
    /**
     * Fire the <code>sourceAdded</code> message.
     * 
     * @param project The Project.
     * @param source The new Source object.
     * 
     * @see ProjectListener#sourceAdded(ProjectEvent)
     */
    public void fireSourceAdded(Project project, Source source) {
        if (willFire()) {
            ProjectEvent event = new ProjectEvent(eventSource, project, source);
            fireEvent("sourceAdded", event);
        }
    }
    
    /**
     * Fire the <code>sourceModified</code> message.
     * 
     * @param project The Project.
     * @param source The modified Source object.
     * 
     * @see ProjectListener#sourceModified(ProjectEvent)
     */
    public void fireSourceModified(Project project, Source source) {
        if (willFire()) {
            ProjectEvent event = new ProjectEvent(eventSource, project, source);
            fireEvent("sourceModified", event);
        }
    }
    
    /**
     * Fire the <code>sourceDeleted</code> message.
     * 
     * @param project The Project.
     * @param source The removed Source object.
     * 
     * @see ProjectListener#sourceDeleted(ProjectEvent)
     */
    public void fireSourceDeleted(Project project, Source source) {
        if (willFire()) {
            ProjectEvent event = new ProjectEvent(eventSource, project, source);
            fireEvent("sourceDeleted", event);
        }
    }
    
    /**
     * Fire the <code>postProcessorsChanged</code> message.
     * 
     * @param project The Project.
     * 
     * @see ProjectListener#postProcessorsChanged(ProjectEvent)
     */
    public void firePostProcessorsChanged(Project project) {
        if (willFire()) {
            ProjectEvent event = new ProjectEvent(eventSource, project);
            fireEvent("postProcessorsChanged", event);
        }
    }
    
    /**
     * Check to see if there is any point in executing the event raising
     * code. There is no need if there are no listeners registered.
     * 
     * @return <code>true</code> if the event should be created and raised,
     * <code>false</code> if not.
     */
    protected boolean willFire() {
        return listeners != null && !listeners.isEmpty();
    }
    
    /**
     * Fire the given ProjectEvent via the named <code>ProjectListener</code>
     * method. Any errors that trickle back during event notification are logged
     * but otherwise ignored.
     * 
     * @param methodName The name of the ProjectListener method.
     * @param event The event object.
     */
    protected void fireEvent(String methodName, ProjectEvent event) {
        if (methodCache == null) {
            methodCache = new HashMap<String, Method>();
        }
        
        Method method = methodCache.get(methodName);
        if (method == null) {
            try {
                method = ProjectListener.class.getMethod(methodName, ProjectEvent.class);
                methodCache.put(methodName, method);
            } catch (NoSuchMethodException e) {
                logger.warn("No method " + methodName + " on ProjectEvent. No events firing.");
                return;
            }
        }
        
        if (willFire()) {
            ProjectListener[] array = new ProjectListener[listeners.size()];
            listeners.toArray(array);
            
            for (ProjectListener l : array) {
                try {
                    method.invoke(l, event);
                } catch (IllegalAccessException e) {
                    logger.warn("Cannot invoke " + methodName + " on " + l.getClass().getName());
                } catch (InvocationTargetException e) {
                    logger.error("Error during project event notification:",
                                 e.getTargetException());
                }
            }
        }
    }
}
