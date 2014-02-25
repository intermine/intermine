package org.intermine.webservice.server.jbrowse;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

public class Endpoint extends JSONService {

    private static final String CMD_RUNNER = "webservice.jbrowse.commandrunner.";

    public Endpoint(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws ServiceException {
        Command cmd = getCommand();
        CommandRunner runner = CommandRunner.getRunner(getProperty(CMD_RUNNER + im.getModel().getName()), im);
        Map<String, Object> result = runner.run(cmd);
        addResultEntries(result.entrySet());
    }

    // ------------ Helper methods ------------------//

    // Never null
    private Command getCommand() throws ServiceException {
        String pathInfo = request.getPathInfo();
        Map<String, String[]> params = request.getParameterMap();
        Command cmd = Commands.getCommand(pathInfo, singlefyMap(params));
        if (cmd == null) {
            throw new ResourceNotFoundException(request.getPathInfo());
        }
        return cmd;
    }

    private static <K, V> Map<K, V> singlefyMap(Map<K, V[]> input) {
        Map<K, V> output = new HashMap<K, V>();
        for (Entry<K, V[]> entry: input.entrySet()) {
            V[] values = entry.getValue();
            if (values.length == 1) {
                output.put(entry.getKey(), values[0]);
            }
        }
        return output;
    }

}
