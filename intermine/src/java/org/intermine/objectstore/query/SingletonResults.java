package org.flymine.objectstore.query;

/**
 * This class is equivalent to a Result object with ResultRows consisting only of single items
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class SingletonResults extends Results 
{
    /**
     * @param index of the object required
     * @return the revelant object
     */
    public Object get(int index) {
        return null;
    }
}
