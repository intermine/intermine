package org.intermine.webservice.server.idresolution;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.time.DateUtils;
import org.intermine.api.idresolution.IDResolver;
import org.intermine.api.idresolution.Job;

/**
 * Wake up once a minute and evict old completed jobs.
 * @author Alex Kalderimis
 *
 */
public class JobJanitor implements Runnable
{

    private static final long PERIOD = 60 * 1000;
    private volatile boolean canContinue = true;

    @Override
    public void run() {
        IDResolver idresolver = IDResolver.getInstance();
        while (canContinue) {
            Iterator<Job> jobs = idresolver.getJobs().values().iterator();
            Date cutOff = DateUtils.addHours(new Date(), -3);
            while (jobs.hasNext()) {
                if (Thread.interrupted()) {
                    return;
                }
                Job job = jobs.next();
                switch (job.getStatus()) {
                    case ERROR:
                    case SUCCESS:
                        Date startedAt = job.getStartedAt();
                        if (startedAt != null && startedAt.before(cutOff)) {
                            jobs.remove();
                        }
                        break;
                    case PENDING:
                    case RUNNING:
                    default:
                        continue;
                }
            }

            try {
                Thread.sleep(PERIOD);
            } catch (InterruptedException e) {
                return; // Server going down - abandon ship.
            }
        }

    }

    /** Let others tell us to stop. **/
    public void stop() {
        canContinue = false;
    }

}
