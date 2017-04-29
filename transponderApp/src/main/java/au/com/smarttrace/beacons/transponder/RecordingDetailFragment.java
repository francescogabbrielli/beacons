package au.com.smarttrace.beacons.transponder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Arrays;
import java.util.LinkedList;

import au.com.smarttrace.beacons.tracker.Recording;
import au.com.smarttrace.beacons.tracker.RecordingManager;
import au.com.smarttrace.beacons.transponder.recording.ChartUpdate;
import au.com.smarttrace.beacons.transponder.recording.HeaderUpdate;
import au.com.smarttrace.beacons.transponder.recording.MapUpdate;

/**
 * A fragment representing a single Recording detail screen.
 * This fragment is either contained in a {@link RecordingListActivity}
 * in two-pane mode (on tablets) or a {@link RecordingDetailActivity}
 * on handsets.
 */
public class RecordingDetailFragment extends Fragment
        implements RecordingManager.Callback, OnMapReadyCallback, GoogleMap.OnMapLoadedCallback,
        AdapterView.OnItemSelectedListener {

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private static final LatLngBounds AUSTRALIA = new LatLngBounds(
                new LatLng(-44, 113), new LatLng(-10, 154));

    private final static int READY_MAP = 1;
    private final static int READY_FRAGMENT = 2;
    private final static int READY_ALL = READY_MAP|READY_FRAGMENT;

    private Recording item;

    private View rootView;

    private MapView mapView;
    private GoogleMap map;

    private HeaderUpdate headerUpdate;
    private ChartUpdate chartUpdate;
    private MapUpdate mapUpdate;

    private int readyState = 0;

    private AsyncTask<Recording, Void, Void> updateTask;
    private LinkedList<Runnable> updates = new LinkedList<>();


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecordingDetailFragment() {
    }

    @Override
    public void onReceive(Recording r) {
        item = r;
        if (updateTask!=null && updateTask.getStatus()== AsyncTask.Status.RUNNING)
            updateTask.cancel(true);
        updateTask = new AsyncTask<Recording, Void, Void>() {
            @Override
            protected Void doInBackground(Recording... params) {
                Recording rec = params[0];
                Log.d(Recording.TAG, "display: "+rec);
                mapUpdate.load(rec);
                startUpdates(
                        headerUpdate.load(rec),
                        chartUpdate.load(rec));
                return null;
            }
        }.execute(r);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            final long t = getArguments().getLong(ARG_ITEM_ID);
            RecordingManager.getInstance().getById(t, this);
        }
        headerUpdate = new HeaderUpdate(getContext());
        chartUpdate = new ChartUpdate(getContext());
        mapUpdate = new MapUpdate(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recording_detail, container, false);
        readyState = 0;
        this.rootView = rootView;
        initMap(savedInstanceState);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                square(R.id.recording_map);
                square(R.id.recording_chart);
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout)
                getActivity().findViewById(R.id.toolbar_layout);
        headerUpdate.init(appBarLayout, rootView);
        chartUpdate.init(rootView);
        ((Spinner) rootView.findViewById(R.id.recording_dataset)).setOnItemSelectedListener(this);
        startUpdates(READY_FRAGMENT);
    }

    private void square(int id) {
        View v = rootView.findViewById(id);
        ViewGroup.LayoutParams p = v.getLayoutParams();
        int max = Math.max(v.getWidth(), v.getHeight());
        p.width = max;
        p.height = max*3/4;
        v.setLayoutParams(p);
    }

    private void initMap(Bundle savedInstanceState) {
        Bundle mapViewBundle = null;
        if (savedInstanceState != null)
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        mapView = (MapView) rootView.findViewById(R.id.recording_map);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("MAP", "Ready");
        map = googleMap;
        map.setOnMapLoadedCallback(this);
    }

    @Override
    public void onMapLoaded() {
        Log.d("MAP", "Loaded");
        startUpdates(mapUpdate.setMap(map));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    private void startUpdates(Runnable... updatesList) {
        startUpdates(0, updatesList);
    }

    private void startUpdates(int flag, Runnable... updatesList) {
        boolean ok;
        synchronized (this) {
            updates.addAll(Arrays.asList(updatesList));
            readyState |= flag;
            ok = (readyState & READY_FRAGMENT) != 0;
        }
        while (ok) {
            synchronized (this) {
                if(!updates.isEmpty())
                    getActivity().runOnUiThread(updates.poll());
                else
                    break;
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        startUpdates(chartUpdate.selectDataset(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        startUpdates(chartUpdate.removeDataset());
    }

}
