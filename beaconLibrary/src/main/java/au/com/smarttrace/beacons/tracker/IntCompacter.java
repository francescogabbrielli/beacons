package au.com.smarttrace.beacons.tracker;

public class IntCompacter implements TrackingCompacter<Integer> {

    @Override
    public boolean inThreshold(Integer o1, Integer o2) {
        return o1.equals(o2);
    }

    @Override
    public boolean isTime(long previous, long actual) {
        return false;
    }

}
