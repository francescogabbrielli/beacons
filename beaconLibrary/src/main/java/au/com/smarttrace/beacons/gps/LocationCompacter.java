package au.com.smarttrace.beacons.gps;

import android.location.Location;

import au.com.smarttrace.beacons.tracker.TrackingCompacter;

/**
 *
 */
public class LocationCompacter implements TrackingCompacter<Location> {

    protected final static long MAX_TIME_LAG = 5 * 60000l;
    protected final static double LOCATION_TOLERANCE = 0.001d;

    @Override
    public boolean inThreshold(Location o1, Location o2) {
        return Math.abs(o1.getLatitude()-o2.getLatitude()) < LOCATION_TOLERANCE
                && Math.abs(o1.getLongitude()-o2.getLongitude()) < LOCATION_TOLERANCE;
    }

    @Override
    public boolean isTime(long previous, long actual) {
        return actual - previous > MAX_TIME_LAG;
    }
}
