package au.com.smarttrace.beacons.tracker;

import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import au.com.smarttrace.beacons.Device;
import au.com.smarttrace.beacons.DeviceEvent;
import au.com.smarttrace.beacons.DeviceListener;
import au.com.smarttrace.beacons.DeviceManager;
import au.com.smarttrace.beacons.NoSuchDeviceException;

/**
 *
 */
public class Recording implements DeviceListener {


    public static class Header implements Comparable<Header> {
        Long begin;
        Long end;
        Integer readings;
        Header() {}
        Header(Recording recording) {
            begin = recording.begin.getTime();
            end = recording.end.getTime();
            readings = recording.size();
        }

        public long getBegin() {
            return begin;
        }

        public long getEnd() {
            return end;
        }

        public int getReadings() {
            return readings;
        }

        @Override
        public int compareTo(Header h) {
            return h==null ? 1 : begin.compareTo(h.begin);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Header && begin.equals(((Header)obj).begin);
        }

        @Override
        public int hashCode() {
            return new Long(begin).hashCode();
        }

        @Override
        public String toString() {
            return DateFormat.getDateTimeInstance().format(new Date(begin));
        }
    }

    /**
     * Start time of components
     */
    protected Date begin;

    /**
     * End time of components
     */
    protected Date end;

    /**
     * Current trackings
     */
    private Map<String, Tracking> trackings;

    public Recording() {
        begin = new Date();
        trackings = new TreeMap<>();
    }

    public Date getBegin() {
        return begin;
    }

    public Date getEnd() {
        return end;
    }

    public int size() {
        int ret = 0;
        for (Tracking t : trackings.values())
            ret += t.size();
        return ret;
    }

    public Tracking getTracking(String deviceId) {
        return trackings.get(deviceId);
    }

    public synchronized void addDevice(Device device) {
        Tracking t = trackings.get(device.getId());
        if (t==null) {
            t = new Tracking(device.getContext());
            trackings.put(device.getId(), t);
        }
        device.onTrackingStart(t);
    }

    private synchronized void removeDevice(Device device) {
        device.onTrackingStop();
    }

    public synchronized void stop() {
        if (begin!=null && end==null) {
            for (String id : trackings.keySet())
                //stop only the devices that are actually present; the others must
                //already have been stopped when removed from the manager
                try {
                    DeviceManager.getInstance().getDevice(id).onTrackingStop();
                } catch(NoSuchDeviceException e) {
//                    DeviceManager.getInstance().stopTracking(id);
                }
            end = new Date();
            RecordingManager.getInstance().add(this);
        }
    }

    public Set<String> getDeviceIds() {
        return trackings.keySet();
    }

    public synchronized boolean isActive() {
        return begin!=null && end==null;
    }

    @Override
    public void onDeviceChange(DeviceEvent event) {
        Device device = event.getDevice();
        switch (event.getType()) {
            case DeviceEvent.TYPE_DEVICE_ADDED:
                addDevice(device);
                break;
            case DeviceEvent.TYPE_DEVICE_REMOVED:
                removeDevice(device);
                break;
        }
    }

    @Override
    public String toString() {
        return DateFormat.getDateTimeInstance().format(begin);
    }
}
