package org.intermine.webservice.server.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.intermine.api.results.ResultElement;
import org.intermine.webservice.server.output.MemoryOutput;
import org.junit.Test;

public class ResultProcessorTest {

    @Test
    public void nullElements() {
        List<ResultElement> row = new ArrayList<ResultElement>();
        row.add(null);
        row.add(null);
        row.add(null);
        row.add(null);
        @SuppressWarnings("unchecked")
        List<List<ResultElement>> table = Arrays.asList(row);
        MemoryOutput output = new MemoryOutput();
        ResultProcessor processor = new ResultProcessor();
        processor.write(table.iterator(), output);
        assertEquals("Every null element should now be an empty string",
                Arrays.asList("", "", "", ""),
                output.getResults().get(0));
    }

}
