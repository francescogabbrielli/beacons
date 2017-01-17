package au.com.smarttrace.beacons.tracker;

import android.location.Location;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import au.com.smarttrace.beacons.Device;


public class Tracking {

    protected final static String KEY_TIME = "time";
    protected final static String KEY_LOCATION = "location";

    protected final static long MAX_TIME_LAG = 5 * 60000l;
    protected final static double LOCATION_TOLERANCE = 0.001;


    /**
     * The device that is tracking
     */
    protected Device tracker;

    class TrackingElement<T> {
        List<Long> times;
        List<T> objects;
        transient Trackator<T> trackator;
        boolean add(long time, T o) {
            int index = Collections.binarySearch(times, time);
            if (index<0) {
                if (index==-1) {
                    objects.add(0, o);
                    times.add(0, time);
                    return true;
                } else {
                    T prev = objects.get(-index-2);
                    if (!trackator.inThreshold(o, prev)) {
                        objects.add(o);
                        times.add(time);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    Map<String, TrackingElement> tracking;

    public boolean addSample(String sampleKey, Object value) {
        TrackingElement e = tracking.get(sampleKey);
        return e.add(System.currentTimeMillis(), value);
    }

    public boolean addSample(long time, String sampleKey, Object value) {
        TrackingElement e = tracking.get(sampleKey);
        return e.add(time, value);
    }


}
