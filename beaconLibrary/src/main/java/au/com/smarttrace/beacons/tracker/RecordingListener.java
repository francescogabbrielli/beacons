package au.com.smarttrace.beacons.tracker;

import java.util.EventListener;

/**
 * Created by dstech2013 on 28/01/2017.
 */

public interface RecordingListener extends EventListener {

    void onRecordingChange(RecordingEvent event);

}
