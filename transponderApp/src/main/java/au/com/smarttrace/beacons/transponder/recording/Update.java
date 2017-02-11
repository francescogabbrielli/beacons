package au.com.smarttrace.beacons.transponder.recording;

import android.content.Context;
import android.content.res.Resources;

import au.com.smarttrace.beacons.tracker.Recording;

public abstract class Update implements Runnable {

    protected Context context;

    /**
     * <p>
     * Manage updates for the Chart in the detail fragment.
     * <p>
     * Load data async in a separate thread, run sync in UI thread.
     *
     * @param context
     *              the detail fragment context
     */
    public Update(Context context) {
        this.context = context;
    }

    protected Resources getResources() {
        return context.getResources();
    }

    protected Context getContext() {
        return context;
    }

    /**
     * Load data. Call from separate async thread
     *
     * @param recording
     *
     * @return
     */
    public abstract Update load(Recording recording);

}
