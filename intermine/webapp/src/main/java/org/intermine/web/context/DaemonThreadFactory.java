package org.intermine.web.context;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.concurrent.ThreadFactory;

/**
 * A thread factory that automatically sets threads it creates to be
 * daemonic.
 * @author Alex Kalderimis
 *
 */
class DaemonThreadFactory implements ThreadFactory
{
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    }
}
