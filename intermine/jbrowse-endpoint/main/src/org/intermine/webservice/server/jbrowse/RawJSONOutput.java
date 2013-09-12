package org.intermine.webservice.server.jbrowse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.webservice.server.output.Output;
import org.json.JSONException;
import org.json.JSONObject;

public class RawJSONOutput extends Output {

    private static final Logger LOG = Logger.getLogger(RawJSONOutput.class);
    private final Writer writer;

    public RawJSONOutput(Writer w) {
        this.writer = w;
    }

    public void addResultItem(JSONObject obj) {
        try {
            obj.write(writer);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addResultItem(List<String> item) {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            LOG.error("Could not flush output.", e);
        }
    }

    @Override
    protected int getResultsCount() {
        return 0;
    }

}
