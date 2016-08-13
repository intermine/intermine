package org.intermine.api.idresolution;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.bag.BagQueryUpgrade;

/**
 *
 * @author Alex
 *
 */
public final class IDResolver
{

    /**
     * list of jobs
     */
    private final Map<UUID, Job> jobs = new ConcurrentHashMap<UUID, Job>();

    private static IDResolver instance = new IDResolver();

    /**
     *
     * @return ID resolver
     */
    public static IDResolver getInstance() {
        return instance;
    }

    private ExecutorService threadPool;

    private IDResolver() {
        this.threadPool = Executors.newCachedThreadPool();
    }

    /**
     *
     * @return all current jobs
     */
    public Map<UUID, Job> getJobs() {
        return jobs;
    }

    /**
     *
     * @param id job ID
     * @return job
     */
    public Job getJobById(UUID id) {
        return jobs.get(id);
    }

    /**
     *
     * @param id job id
     * @return job
     */
    public Job getJobById(String id) {
        return getJobById(UUID.fromString(id));
    }

    /**
     *
     * @param runner bag query runner
     * @param input input
     * @return job
     */
    public Job submit(BagQueryRunner runner, JobInput input) {
        UUID id = UUID.randomUUID();
        Job job = new ResolutionJob(id, runner, input);
        return submitJob(id, job);
    }

    /**
     *
     * @param upgrade upgrade
     * @return job
     */
    public Job submit(BagQueryUpgrade upgrade) {
        UUID id = UUID.randomUUID();
        Job job = new UpgradeJob(id, upgrade);
        return submitJob(id, job);
    }

    private Job submitJob(UUID id, Job job) {
        jobs.put(id, job);
        threadPool.submit(job);
        return job;
    }

    /**
     *
     * @param uid id
     * @return job
     */
    public Job removeJob(String uid) {
        if (uid == null) {
            return null;
        }
        try {
            return jobs.remove(UUID.fromString(uid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
