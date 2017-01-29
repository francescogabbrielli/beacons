package au.com.smarttrace.beacons.tracker;

import java.util.EventObject;

import au.com.smarttrace.beacons.tracker.Recording;

/**
 *
 */
public class RecordingEvent extends EventObject {

    public RecordingEvent(Recording source) {
        super(source);
    }

    public Recording getRecording() {
        return (Recording) getSource();
    }
}
