package org.intermine.tracker;

public interface Tracker
{
    void createTrackerTable() throws Exception ;

    void storeTrack(TrackerInput track);

    Object getTrack();

    String getName();
}
