package org.intermine.plugin.integration

enum IntegrationAction {
    RETRIEVE("retrieve"),
    LOAD("load"),
    RETRIEVE_AND_LOAD("")

    private String action;

    IntegrationAction(String action) {
        this.action = action
    }

    protected static getAction = { String action ->
        if (action == null || "".equals(action)) {
            return RETRIEVE_AND_LOAD
        } else if ("retrieve".equals(action)) {
            return RETRIEVE
        } else if ("load".equals(action)) {
            return LOAD
        } else {
            throw new RuntimeException("Unknown action")
        }
    }
}