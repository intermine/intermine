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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents an interbase segment.
 *
 * @author Alex
 */
public final class Segment
{

    private final String section;
    private final Integer start, end;

    private Segment(String section, Integer start, Integer end) {
        this.section = section;
        this.start = start;
        this.end = end;
    }

    /**
     * negative segment
     */
    public static final Segment NEGATIVE_SEGMENT = new Segment(null, null, null);
    /**
     * global segment
     */
    public static final Segment GLOBAL_SEGMENT = new Segment(null, null, null);

    /**
     * @param ref segment type, e.g. global
     * @param s start
     * @param e end
     * @return segment
     */
    public static Segment makeSegment(String ref, Integer s, Integer e) {
        if (("global").equals(ref)) {
            return GLOBAL_SEGMENT;
        }
        if (s != null && e != null && s < 0 && e < 0) {
            return NEGATIVE_SEGMENT; // Represents all out of band segments
        }
        return new Segment(ref, ((s == null) ? null : Math.max(0, s)), e);
    }

    /**
     * @return section
     */
    public String getSection() {
        return this.section;
    }

    /**
     * @return start
     */
    public Integer getStart() {
        return this.start;
    }

    /**
     * @return end
     */
    public Integer getEnd() {
        return this.end;
    }

    /**
     * @return width
     */
    public Integer getWidth() {
        if (this.end == null || this.start == null) {
            return null;
        }
        return this.end - this.start;
    }

    /**
     * @return range in string format
     */
    public String toRangeString() {
        if (start == null && end == null) {
            return section;
        } else if (start == null || end == null) {
            throw new RuntimeException("Not implemented"); // TODO
        } else {
            // Convert Interbase -> Base coördinates: start + 1
            return String.format("%s:%d..%d", section, start + 1, end);
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

    /**
     * @param i start
     * @param j end
     * @return subsegment
     */
    public Segment subsegment(int i, int j) {
        if (start != null && i < start) {
            throw new IllegalArgumentException("i is less than start");
        }
        if (end != null && j > end) {
            throw new IllegalArgumentException(
                    String.format("j (%d) is greater than end (%d)", j, end));
        }
        return new Segment(section, i, j);
    }
}
