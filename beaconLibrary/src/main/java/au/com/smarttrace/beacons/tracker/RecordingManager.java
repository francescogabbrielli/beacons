package au.com.smarttrace.beacons.tracker;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manager for the recordings stored on the phone
 */
public class RecordingManager {

    private final static String TAG = RecordingManager.class.getSimpleName();

    private final static String HEADERS_FILE = "recordings.lst";

    private static RecordingManager instance;

    private Context context;

    private File dir;

    private List<Recording.Header> headers;

    private Map<Long, Recording> recordings;

    private List<RecordingListener> listenersList;


    private RecordingManager() {
        headers = new ArrayList<>();
        recordings = new TreeMap<>();
        listenersList = new LinkedList<>();
    }

    public static RecordingManager getInstance() {
        if (instance==null)
            instance = new RecordingManager();
        return instance;
    }

    public void init(Context context) {
        this.context = context;
        this.dir = context.getFilesDir();
        //TODO: headersAdapter = new SimpleCursorAdapter(context, ...);
        new AsyncTask<Void, Void, Exception>() {
            private long time;
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    time = SystemClock.currentThreadTimeMillis();
                    loadHeaders();
                } catch (Exception e) {
                    return e;
                } finally {
                    time = SystemClock.currentThreadTimeMillis()-time;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception e) {
                if (e!=null) {
                    Log.e(TAG, "Error loading recording headers", e);
                    Toast.makeText(RecordingManager.this.context,
                            "Error loading recording data:" + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG);
                } else
                    Log.d(TAG, "Recording headers loaded in "+time+"ms");
            }
        }.execute();
    }

    /**
     * Add a recording (in a separate thread)
     *
     * @param newRecording
     *              the new recording
     */
    public void add(final Recording newRecording) {

        new AsyncTask<Void, Void, Exception>() {
            long time;
            Recording.Header h;

            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    time = SystemClock.currentThreadTimeMillis();
                    h = new Recording.Header(newRecording);
                    if (h.readings==0) {
                        cancel(false);
                        return null;
                    }
                    headers.add(h);
                    save(newRecording);
                    saveHeaders();
                } catch(Exception e) {
                    return e;
                } finally {
                    time = SystemClock.currentThreadTimeMillis()-time;
                }
                return null;
            }

            @Override
            protected void onCancelled() {
                //TODO
            }

            @Override
            protected void onPostExecute(Exception e) {
                if (e!=null) {
                    headers.remove(h);
                    Log.e(TAG, "Error saving recording headers", e);
                    Toast.makeText(RecordingManager.this.context,
                            "Error saving recording data:" + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG);
                } else {
                    recordings.put(h.begin, newRecording);
                    RecordingManager.getInstance().fireEvent(
                            new RecordingEvent(newRecording));
                    Log.d(TAG, "Recording headers saved in " + time + "ms");
                }
            }
        }.execute();
    }

    /**
     * Get a recording started at the given time
     *
     * @param time
     *              time in ms
     * @return
     *          the recording
     */
    public Recording getById(long time) {
        return recordings.get(time);
    }

    public interface Callback {
        void onReceive(Recording rec);
    }

    /**
     * Get a recording started at the given time through a callback
     *
     * @param time
     *              time in ms
     * @param result
     *              the callback to pass the result
     */
    public void getById(long time, final Callback result) {
        Recording ret = getById(time);
        if (ret!=null) {
            result.onReceive(ret);
            return;
        }
        new AsyncTask<Long, Void, Recording>() {

            private Exception error;

            @Override
            protected Recording doInBackground(Long... params) {
                Recording r = null;
                try {
                    r = load(params[0]);
                } catch(IOException e) {
                    error = e;
                    cancel(false);
                }
                return r;
            }

            @Override
            protected void onCancelled() {
                Toast.makeText(context, error.getLocalizedMessage(), Toast.LENGTH_SHORT);
            }

            @Override
            protected void onPostExecute(Recording recording) {
                result.onReceive(recording);
            }

        }.execute(time);
    }

    public synchronized Recording.Header getHeader(int position) {
        return headers.get(position);
    }

    public synchronized int count() {
        return headers.size();
    }

    private synchronized void loadHeaders() throws IOException {
        Gson gson = new Gson();
        FileReader r = null;
        try {
            r = new FileReader(new File(dir, HEADERS_FILE));
            Type token = new TypeToken<List<Recording.Header>>(){}.getType();
            headers = gson.fromJson(r, token);
        } finally {
            try {r.close();} catch(Exception e) {}
        }
    }

    /**
     * Load a recording from the storage
     *
     * @param timeline
     *              time of recording
     * @return
     *          the recording
     * @throws IOException
     *          for I/O problems
     */
    public synchronized Recording load(long timeline) throws IOException {
        Gson gson = new Gson();
        FileReader r = null;
        String filename = timeline+".rec";
        Recording recording = null;
        try {
            r = new FileReader(new File(dir, filename));
            recording = gson.fromJson(r, Recording.class);
            recordings.put(timeline, recording);
        } finally {
            try {r.close();} catch(Exception e) {}
        }
        return recording;
    }

    private synchronized void saveHeaders() throws IOException {
        Gson gson = new Gson();
        FileWriter w = null;
        try {
            Log.i(TAG, "FILE: "+new File(dir, HEADERS_FILE));
            w = new FileWriter(new File(dir, HEADERS_FILE), false);
            gson.toJson(headers, w);
        } finally {
            try {w.close();} catch(Exception e) {}
        }
    }

    private synchronized void save(Recording recording) throws IOException {
        Gson gson = new Gson();
        FileWriter w = null;
        String filename = recording.begin.getTime()+".rec";
        try {
            Log.i(TAG, "FILE: "+new File(dir, filename));
            w = new FileWriter(new File(dir, filename), false);
            gson.toJson(recording, w);
        } finally {
            try {w.close();} catch(Exception e) {}
        }
    }

    /**
     * Add a new {@link RecordingListener}, replacing an eventual old one
     *
     * @param listener
     * 				the listener
     */
    public synchronized void addRecordingListener(RecordingListener listener) {
        int pos = listenersList.indexOf(listener);
        if (pos>=0)
            listenersList.set(pos, listener);
        else
            listenersList.add(listener);
    }

    /**
     * Remove a {@link RecordingListener}
     *
     * @param listener
     * 				the listener
     */
    public synchronized void removeRecordingListener(RecordingListener listener) {
        listenersList.remove(listener);
    }

    protected synchronized void fireEvent(RecordingEvent event) {
        for (RecordingListener l : listenersList)
            l.onRecordingChange(event);
    }

}
