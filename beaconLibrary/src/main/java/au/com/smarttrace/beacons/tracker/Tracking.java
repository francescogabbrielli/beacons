package au.com.smarttrace.beacons.tracker;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Tracking {

    class TrackingComponent<T> {
        List<Long> timeline;
        List<T> data;
        transient Class<T> dataClass;
        transient TrackingCompacter<T> compacter;
        TrackingComponent(String key, Class<T> dataClass) {
            this.dataClass = dataClass;
            timeline = new ArrayList<>();
            data = new ArrayList<>();
            compacter = TrackingCompacterFactory.getInstance().getCompacter(context, key, dataClass);
        }
        boolean add(long time, T o) {
            int index = Collections.binarySearch(timeline, time);
            if (index<0) {
                if (index==-1) {
                    data.add(0, o);
                    timeline.add(0, time);
                    return true;
                } else {
                    T prev = data.get(-index-2);
                    long t = timeline.get(-index-2);
                    if (compacter==null || !compacter.inThreshold(o, prev) || compacter.isTime(t, time)) {
                        data.add(o);
                        timeline.add(time);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    Map<String, TrackingComponent> components;

    private Context context;

    public Tracking(Context context) {
        this.context = context;
        components = new LinkedHashMap<>();
    }

    private <T> TrackingComponent<T> getComponent(String sampleKey, Class<T> sampleClass) {
        TrackingComponent<T> c = components.get(sampleKey);
        if (c==null) {
            c = new TrackingComponent<>(sampleKey, sampleClass);
            components.put(sampleKey, c);
        }
        return c;
    }

    public boolean addSample(String sampleKey, Object value) {
        return addSample(System.currentTimeMillis(), sampleKey, value);
    }

    public boolean addSample(long time, String sampleKey, Object value) {
        if (value==null)
            return false;
        TrackingComponent e = getComponent(sampleKey, value.getClass());
        return e.add(time, value);
    }

}
