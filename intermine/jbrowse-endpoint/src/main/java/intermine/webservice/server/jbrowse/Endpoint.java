package org.intermine.webservice.server.jbrowse;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 *
 * @author Alex
 *
 */
public class Endpoint extends JSONService
{

    private static final String CMD_RUNNER = "webservice.jbrowse.commandrunner.";
    private Map<String, Object> attrs = new HashMap<String, Object>();

    /**
     *
     * @param im InterMine API
     */
    public Endpoint(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws ServiceException {
        Command cmd = getCommand();

        CommandRunner runner = CommandRunner.getRunner(getProperty(CMD_RUNNER
                + im.getModel().getName()), im);
        runner.addListener(getListener());

        String intro = runner.getIntro(cmd);
        if (intro != null) {
            attrs.put(JSONFormatter.KEY_INTRO, intro);
        }
        String outro = runner.getOutro(cmd);
        if (outro != null) {
            attrs.put(JSONFormatter.KEY_OUTRO, outro);
        }
        runner.run(cmd);
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = super.getHeaderAttributes();
        this.attrs = attributes;
        return attributes;
    }

    // ------------ Helper methods ------------------//

    private MapListener<String, Object> getListener() {
        return new MapListener<String, Object>() {
            @Override
            public void add(Map<String, Object> entry, boolean hasMore) {
                addResultItem(entry, hasMore);
            }

            @Override
            public void add(Entry<String, Object> entry, boolean hasMore) {
                addResultEntries(Collections.singleton(entry), hasMore);
            }
        };
    }

    // Never null
    private Command getCommand() throws ServiceException {
        String pathInfo = request.getPathInfo();
        @SuppressWarnings("unchecked")
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
