package au.com.smarttrace.beacons.tracker;

/**
 * Created by dstech2013 on 17/01/2017.
 */

public interface Trackator<T> {

    public boolean inThreshold(T o1, T o2);

}
