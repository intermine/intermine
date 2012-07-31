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

import java.util.EventListener;

/**
 * Project event listener interface.
 */
public interface ProjectListener extends EventListener {
    
    /**
     * Called when a new project is created.
     *  
     * @param event The event object.
     */
    void projectCreated(ProjectEvent event);
    
    /**
     * Called when a project is loaded.
     *  
     * @param event The event object.
     */
    void projectLoaded(ProjectEvent event);
    
    /**
     * Called when a new source is added to a project.
     *  
     * @param event The event object.
     */
    void sourceAdded(ProjectEvent event);
    
    /**
     * Called when a source is modified.
     *  
     * @param event The event object.
     */
    void sourceModified(ProjectEvent event);
    
    /**
     * Called when a source is removed from a project.
     *  
     * @param event The event object.
     */
    void sourceDeleted(ProjectEvent event);
    
    /**
     * Called when a project's post processors are modified.
     *  
     * @param event The event object.
     */
    void postProcessorsChanged(ProjectEvent event);
}
