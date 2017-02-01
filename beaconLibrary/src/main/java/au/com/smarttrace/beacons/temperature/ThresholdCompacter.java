package au.com.smarttrace.beacons.temperature;

import au.com.smarttrace.beacons.tracker.TrackingCompacter;

public class ThresholdCompacter implements TrackingCompacter<Float> {

    public final static float THRESHOLD = 0.015f;

    public final static long TIME_THRESHOLD = 5 * 60000l;

    @Override
    public boolean inThreshold(Float o1, Float o2) {
        return Math.abs(o1-o2) < THRESHOLD;
    }

    @Override
    public boolean isTime(long previous, long actual) {
        return actual - previous > TIME_THRESHOLD;
    }
}
