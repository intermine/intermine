package org.intermine.api.idresolution;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections.MapUtils;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.bag.BagQueryUpgrade;

public class IDResolver {

    public final Map<UUID, Job> JOBS = new ConcurrentHashMap<UUID, Job>();

    private static IDResolver instance = new IDResolver();

    public static IDResolver getInstance() {
        return instance;
    }

    private ExecutorService threadPool;

    private IDResolver() {
        this.threadPool = Executors.newCachedThreadPool();
    }

    public Job getJobById(UUID id) {
        return JOBS.get(id);
    }

    public Job getJobById(String id) {
        return getJobById(UUID.fromString(id));
    }

    public Job submit(BagQueryRunner runner, JobInput input) {
        UUID id = UUID.randomUUID();
        Job job = new ResolutionJob(id, runner, input);
        return submitJob(id, job);
    }

    public Job submit(BagQueryUpgrade upgrade) {
        UUID id = UUID.randomUUID();
        Job job = new UpgradeJob(id, upgrade);
        return submitJob(id, job);
    }

    private Job submitJob(UUID id, Job job) {
        JOBS.put(id, job);
        threadPool.submit(job);
        return job;
    }

    public Job removeJob(String uid) {
        if (uid == null) {
            return null;
        }
        try {
            return JOBS.remove(UUID.fromString(uid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
