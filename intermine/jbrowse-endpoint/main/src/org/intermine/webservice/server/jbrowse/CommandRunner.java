package org.intermine.webservice.server.jbrowse;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;

public abstract class CommandRunner {

    private InterMineAPI api;

    private static Map<String, CommandRunner> runners
        = new HashMap<String, CommandRunner>();

    public CommandRunner(InterMineAPI api) {
        this.api = api;
    }

    protected InterMineAPI getAPI() {
        return api;
    }

    public static CommandRunner getRunner(String className, InterMineAPI im) {
        CommandRunner runner;
        try {
            Class<CommandRunner> runnerCls = (Class<CommandRunner>) Class.forName(className);
            Constructor<CommandRunner> ctr = runnerCls.getConstructor(InterMineAPI.class);
            runner = ctr.newInstance(im);
        } catch (ClassCastException e) {
            throw new RuntimeException("Configuration is incorrect.", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find " + className);
        } catch (SecurityException e) {
            throw new RuntimeException("Not allowed to access " + className);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot access constructor for " + className);
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot instantiate runner", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access runner", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot invoke constructor of " + className, e);
        }
        return runner;
    }

    public Map<String, Object> run(Command command) {
        switch (command.getAction()) {
        case STATS:
            return stats(command);
        case REFERENCE:
            return reference(command);
        case FEATURES:
            return features(command);
        case DENSITIES:
            return densities(command);
        default:
            throw new IllegalArgumentException("Unknown action: " + command.getAction());
        }
    }

    public abstract Map<String, Object> stats(Command command);

    public abstract Map<String, Object> reference(Command command);

    public abstract Map<String, Object> features(Command command);

    public abstract Map<String, Object> densities(Command command);
}
