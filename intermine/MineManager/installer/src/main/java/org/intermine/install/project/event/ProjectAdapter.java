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

import java.io.Serializable;

/**
 * Convenience implementation of the <code>ProjectListener</code> interface.
 * This class contains only no-op implementations of the methods, allowing
 * subclasses to only override the methods they are interested in, a-la
 * {@link java.awt.event.WindowAdapter} etc.
 */
public class ProjectAdapter implements Serializable, ProjectListener
{
    private static final long serialVersionUID = -4635132645226226732L;

    @Override
    public void projectCreated(ProjectEvent event) {
    }

    @Override
    public void projectLoaded(ProjectEvent event) {
    }

    @Override
    public void sourceAdded(ProjectEvent event) {
    }

    @Override
    public void sourceModified(ProjectEvent event) {
    }

    @Override
    public void sourceDeleted(ProjectEvent event) {
    }

    @Override
    public void postProcessorsChanged(ProjectEvent event) {
    }
}
