package org.intermine.webservice.server.idresolution;

import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.lang.time.DateUtils;
import org.intermine.api.idresolution.IDResolver;
import org.intermine.api.idresolution.Job;

/**
 * Wake up once a minute and evict old completed jobs.
 * @author Alex Kalderimis
 *
 */
public class JobJanitor implements Runnable {

    private static final long PERIOD = 60 * 1000;
    private volatile boolean canContinue = true;

    @Override
    public void run() {
        IDResolver idresolver = IDResolver.getInstance();
        while (canContinue) {
            Iterator<Job> jobs = idresolver.JOBS.values().iterator();
            Date cutOff = DateUtils.addHours(new Date(), -3);
            while (jobs.hasNext()) {
                if (Thread.interrupted()) {
                    return;
                }
                Job job = jobs.next();
                switch (job.getStatus()) {
                    case ERROR:
                    case SUCCESS:
                        Date startedAt = job.getStatedAt();
                        if (startedAt != null && startedAt.before(cutOff)) {
                            jobs.remove();
                        }
                    case PENDING:
                    case RUNNING:
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

    public void stop() {
        canContinue = false;
    }

}
