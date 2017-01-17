package au.com.smarttrace.beacons.tracker;

import java.util.Date;
import java.util.List;

import au.com.smarttrace.beacons.Device;

/**
 * Created by dstech2013 on 17/01/2017.
 */

public class Recording {

    /**
     * Start time of tracking
     */
    protected Date begin;

    /**
     * End time of tracking
     */
    protected Date end;


    private List<Tracking> trackings;


    public void addTracking(Device device) {
        trackings.add(new Tracking(device));
    }

    public synchronized void start() {
        if (begin==null && end==null)
            begin = new Date();
    }

    public synchronized void stop() {
        if (begin!=null && end==null)
            end = new Date();
    }

    public synchronized boolean isTracking() {
        return begin!=null && end==null;
    }


}
