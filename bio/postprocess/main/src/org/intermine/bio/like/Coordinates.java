package org.intermine.bio.like;

import java.io.Serializable;
import java.util.Map.Entry;

/**
 * Builds a pair of coordinates. So, a matrix can be simulated in a HashMap. Where these
 * Coordinates are the key.
 *
 * @author selma
 *
 */
public final class Coordinates implements Entry<Integer, Integer>, Serializable
{

    /**
    * Needed for Serializable
    */
    private static final long serialVersionUID = 3072845275407842459L;

    private final int x, y;

    /**
     * Construct a pair of coordinates.
     *
     * @param x coordinate
     * @param y coordinate
     */
    public Coordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public Integer getKey() {
        return x;
    }

    @Override
    public Integer getValue() {
        return y;
    }

    @Override
    public Integer setValue(Integer arg0) {
        throw new RuntimeException("This class is final");
    }

    @Override
    public String toString() {
        return String.format("Pair(%s,%s)", x, y);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Coordinates other = (Coordinates) obj;
        if (x != other.x) {
            return false;
        }
        if (y != other.y) {
            return false;
        }
        return true;
    }

}
