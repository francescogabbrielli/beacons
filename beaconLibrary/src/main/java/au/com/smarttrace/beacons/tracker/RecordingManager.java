package au.com.smarttrace.beacons.tracker;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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

import au.com.smarttrace.beacons.gps.GPSDevice;

/**
 * Manager for the recordings stored on the phone
 */
public class RecordingManager implements JsonDeserializer<Tracking.Data> {

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
                Toast.makeText(context, "Not recorded", Toast.LENGTH_SHORT);
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
        /** Invoke from the UI thread */
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
    public void getById(final long time, final Callback result) {
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
                    Log.e(TAG, "Not loaded "+time+".rec", e);
                    error = e;
                    cancel(false);
                }
                return r;
            }

            @Override
            protected void onCancelled(Recording rec) {
                Toast.makeText(context, "I/O Error", Toast.LENGTH_SHORT);
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

    private Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Tracking.Data.class, this)
                .create();
    }

    @Override
    public Tracking.Data deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject o = json.getAsJsonObject();
        List<Long> timeline = context.deserialize(o.get("timeline"), new TypeToken<List<Long>>(){}.getType());
        List data = null;
        JsonElement first = o.get("data").getAsJsonArray().get(0);
        if (first.isJsonObject() && first.getAsJsonObject().has("lat"))
            data = context.deserialize(o.get("data"), new TypeToken<List<GPSDevice.Sample>>(){}.getType());
        else
            data = context.deserialize(o.get("data"), List.class);
        return new Tracking.Data<>(timeline, data);
    }

    private synchronized void loadHeaders() throws IOException {
        Gson gson = new Gson();
        FileReader r = null;
        try {
            r = new FileReader(new File(dir, HEADERS_FILE));
            Type token = new TypeToken<List<Recording.Header>>(){}.getType();
            Log.i(TAG, "Loaded "+HEADERS_FILE);
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
        Gson gson = buildGson();
        FileReader r = null;
        String filename = timeline+".rec";
        Recording recording = null;
        try {
            r = new FileReader(new File(dir, filename));
            Type token = new TypeToken<Recording>(){}.getType();
            recording = gson.fromJson(r, token);
            Log.i(TAG, "Loaded "+filename);
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
            w = new FileWriter(new File(dir, HEADERS_FILE), false);
            gson.toJson(headers, w);
            Log.i(TAG, "Savied "+HEADERS_FILE);
        } finally {
            try {w.close();} catch(Exception e) {}
        }
    }

    private synchronized void save(Recording recording) throws IOException {
        Gson gson = buildGson();
        FileWriter w = null;
        String filename = recording.begin.getTime()+".rec";
        try {
            w = new FileWriter(new File(dir, filename), false);
            gson.toJson(recording, w);
            Log.i(TAG, "Saved "+filename);
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
