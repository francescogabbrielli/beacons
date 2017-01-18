package au.com.smarttrace.beacons.tracker;

import android.content.Context;
import android.util.Log;

/**
 *
 */
public class TrackingCompacterFactory {

    private final static TrackingCompacterFactory instance = null;

    public static TrackingCompacterFactory getInstance() {
        if (instance==null)
            return new TrackingCompacterFactory();
        return instance;
    }

    private TrackingCompacterFactory() {

    }

    public <T> TrackingCompacter<T> getCompacter(Context context, String trackingKey, Class<T> trackingClass) {
        int id = context.getResources().getIdentifier("compacter." + trackingKey, "string", context.getPackageName());
        if (id!=0)
            try {
                return (TrackingCompacter<T>) Class.forName(context.getResources().getString(id)).newInstance();
            } catch (Exception e) {
                Log.e("Compacter("+trackingKey+")", e.toString(), e);
            }
        return null;
    }
}
