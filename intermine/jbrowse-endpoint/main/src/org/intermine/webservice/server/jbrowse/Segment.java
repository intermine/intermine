package org.intermine.webservice.server.jbrowse;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Segment {

    public final String section;
    public final Integer start, end;

    public Segment(String section, Integer start, Integer end) {
        this.section = section;
        this.start = start;
        this.end = end;
    }

    public final static Segment GLOBAL_SEGMENT = new Segment(null, null, null);

    public String getSection() {
        return this.section;
    }

    public Integer getStart() {
        return this.start;
    }

    public Integer getEnd() {
        return this.end;
    }

    public String toRangeString() {
        if (start == null && end == null) {
            return section;
        } else if (start == null || end == null) {
            throw new RuntimeException("Not implemented"); // TODO
        } else {
            return String.format("%s:%d..%d", section, start, end);
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(section).append(start).append(end).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
}
