package org.intermine.task;

import org.apache.catalina.ant.AbstractCatalinaTask;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class SetDeployURL extends AbstractCatalinaTask {

    private String var;

    public void setVar(String name) {
        var = name;
    }

    @Override
    public boolean isFailOnError() {
        return false;
    }

    public void execute() throws BuildException {
        super.execute();
        log("Determining version of " + getUrl(), Project.MSG_DEBUG);
        execute("/manager/text/list");
    }

    @Override
    protected void handleErrorOutput(String output) {
        if (output == null || !output.equals(getUrl() + "/manager/text/list")) {
            throw new BuildException(output);
        }
        log(getUrl() + " is version 6", Project.MSG_DEBUG);
        setDeployURL(getUrl() + "/manager");
    }

    @Override
    protected void handleOutput(String output, int priority) {
        handleOutput(output);
    }

    @Override
    protected void handleOutput(String output) {
        log(getUrl() + " is version 7", Project.MSG_DEBUG);
        setDeployURL(getUrl() + "/manager/text");
    }

    private void setDeployURL(String value) {
        log("Setting deploy url " + var + " = " + value);
        getProject().setNewProperty(var, value);
    }

}
