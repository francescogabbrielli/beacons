package au.com.smarttrace.beacons.gps;

import android.location.Location;

import au.com.smarttrace.beacons.tracker.TrackingCompacter;

/**
 *
 */
public class LocationCompacter implements TrackingCompacter<GPSDevice.Sample> {

    protected final static long MAX_TIME_LAG = 5 * 60000l;
    protected final static double LOCATION_TOLERANCE = 0.0001d;

    @Override
    public boolean inThreshold(GPSDevice.Sample o1, GPSDevice.Sample o2) {
        return Math.abs(o1.lat-o2.lat) < LOCATION_TOLERANCE
                && Math.abs(o1.lng-o2.lng) < LOCATION_TOLERANCE;
    }

    @Override
    public boolean isTime(long previous, long actual) {
        return actual - previous > MAX_TIME_LAG;
    }
}
