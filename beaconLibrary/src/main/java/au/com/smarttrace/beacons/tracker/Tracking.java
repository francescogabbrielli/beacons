package au.com.smarttrace.beacons.tracker;

import android.content.ComponentCallbacks;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Tracking {

    /**
     *
     * @param <T>
     */
    public class Data<T> {
        List<Long> timeline;
        List<T> data;
        public int size() {
            return data.size();
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    /** Internal data */
    private class TrackingComponent<T> extends Data<T>{
        transient Class<T> dataClass;
        transient TrackingCompacter<T> compacter;
        TrackingComponent() {}
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

    /** Exposed data */
    private Map<String, Data> components;

    /** Application context, needed to read resource files */
    private transient Context context;

    /**
     * Used for serialization purposes
     */
    public Tracking() {
        components = new LinkedHashMap<>();
    }

    /**
     * Used for active recording
     */
    public Tracking(Context context) {
        this();
        this.context = context;
    }

    /**
     * Add a freshly read sample to this device trackingOn
     *
     * @param sampleKey
     *              the sample key
     * @param value
     *              the sample value
     * @return
     *          true if the data was actually added
     */
    public boolean addSample(String sampleKey, Object value) {
        return addSample(System.currentTimeMillis(), sampleKey, value);
    }

    /**
     * Add a sample, read at a specified time, to this device trackingOn
     *
     * @param time
     *              the time of the reading
     * @param sampleKey
     *              the sample key
     * @param value
     *              the sample value
     * @return
     *          true if the data was actually added
     */
    public boolean addSample(long time, String sampleKey, Object value) {
        if (value==null)
            return false;
        TrackingComponent e = (TrackingComponent) getSamples(sampleKey, value.getClass());
        return e.add(time, value);
    }

    /** Read only */
    public <T> Data<T> getSamples(String sampleKey) {
        return getSamples(sampleKey, null);
    }

    /**
     * Return all the samples for the current device trackingOn
     *
     * @param sampleKey
     *              the key of the sample
     * @param sampleClass
     *              the class of the sample
     * @param <T>
     *              the data type of the sample
     * @return
     *          the data
     */
    public <T> Data<T> getSamples(String sampleKey, Class<T> sampleClass) {
        Data<T> c = components.get(sampleKey);
        if (c==null && sampleClass!=null) {
            c = new TrackingComponent<>(sampleKey, sampleClass);
            components.put(sampleKey, c);
        }
        return c;
    }

    public Set<String> getKeys() {
        return components.keySet();
    }

    public int size() {
        int ret = 0;
        for (Data d : components.values())
            ret += d.size();
        return ret;
    }

}
