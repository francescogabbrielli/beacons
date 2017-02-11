package au.com.smarttrace.beacons.tracker;

import android.content.ComponentCallbacks;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.smarttrace.beacons.gps.GPSDevice;

/**
 *
 */
public class Tracking {

    public static class Sample<T> {
        public long time;
        public T data;
        public Sample(long time, T data) {
            this.time = time;
            this.data = data;
        }

    }

    /**
     *
     * @param <T>
     */
    public static class Data<T> implements Iterable<Sample<T>> {
        List<Long> timeline;
        List<T> data;
        public int size() {
            return data.size();
        }
        public Data() {
            timeline = new ArrayList<>();
            data = new ArrayList<>();
        }
        public Data(List<Long> t, List<T> s) {
            timeline = t;
            data = s;
        }

        public T get(int i) {
            return data.get(i);
        }

        public Date getDate(int i) {
            return new Date(timeline.get(i));
        }

        @Override
        public String toString() {
            return data.toString();
        }

        @Override
        public Iterator<Sample<T>> iterator() {
            return new Iterator<Sample<T>>() {
                Iterator<Long> i1 = timeline.iterator();
                Iterator<T> i2 = data.iterator();
                @Override
                public boolean hasNext() {
                    return i1.hasNext();
                }
                @Override
                public Sample<T> next() {
                    return new Sample<>(i1.next(), i2.next());
                }
            };
        }
    }

    /** Internal data */
    private class TrackingComponent<T> extends Data<T>{
        transient Class<T> dataClass;
        transient TrackingCompacter<T> compacter;
        TrackingComponent() {}
        TrackingComponent(String key, Class<T> dataClass) {
            this.dataClass = dataClass;
            if (context!=null)
                compacter = dataClass==GPSDevice.Sample.class
                        ? (TrackingCompacter<T>) GPSDevice.COMPACTER
                        : TrackingCompacterFactory.getInstance().getCompacter(context, key, dataClass);
        }
        public boolean add(long time, T o) {
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
        Log.i("Tracking", String.format("adding %s=%s", sampleKey, value));
        return e.add(time, value);
    }

    /** Read only */
    public Data getSamples(String sampleKey) {
        return getSamples(sampleKey, null);
    }

    /**
     * Return all the samples for the current device trackingOn
     *
     * @param sampleKey
     *              the key of the sample
     * @param sampleClass
     *              the class of the sample
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

    @Override
    public String toString() {
        return components.toString();
    }
}
