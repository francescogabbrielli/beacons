package au.com.smarttrace.beacons.tracker;

import java.util.Date;
import java.util.TreeMap;

import au.com.smarttrace.beacons.Device;
import au.com.smarttrace.beacons.DeviceEvent;
import au.com.smarttrace.beacons.DeviceListener;

/**
 *
 */
public class Recording implements DeviceListener {

    /**
     * Start time of components
     */
    protected Date begin;

    /**
     * End time of components
     */
    protected Date end;

    /**
     * Current tracked trackings
     */
    private TreeMap<Device, Tracking> trackings;

    public Recording() {
        begin = new Date();
        trackings = new TreeMap<>();
    }

    public Tracking getTracking(Device device) {
        return trackings.get(device);
    }

    public synchronized void addDevice(Device device) {
        Tracking old = trackings.get(device);
        Tracking t = old==null ? new Tracking(device.getContext()) : old;
        device.onTrackingStart(t);
    }

    private synchronized void removeDevice(Device device) {
        device.onTrackingStop();
    }

    public synchronized void stop() {
        if (begin!=null && end==null) {
            for (Device d : trackings.keySet())
                d.onTrackingStop();
            end = new Date();
        }
    }

    public synchronized boolean isActive() {
        return begin!=null && end==null;
    }

    @Override
    public void onChange(DeviceEvent event) {
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
}
