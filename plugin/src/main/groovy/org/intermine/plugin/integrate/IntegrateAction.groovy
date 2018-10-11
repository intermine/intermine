package org.intermine.plugin.integrate

enum IntegrateAction {
    PRE_RETRIEVE("pre-retrieve"),
    RETRIEVE("retrieve"),
    LOAD("load"),
    RETRIEVE_AND_LOAD("")

    private String action;

    IntegrateAction(String action) {
        this.action = action
    }

    protected static getAction = { String action ->
        if (action == null || "".equals(action)) {
            return RETRIEVE_AND_LOAD
        }  else if ("pre-retrieve".equals(action)) {
            return PRE_RETRIEVE
        } else if ("retrieve".equals(action)) {
            return RETRIEVE
        } else if ("load".equals(action)) {
            return LOAD
        } else {
            throw new RuntimeException("Unknown action")
        }
    }
}