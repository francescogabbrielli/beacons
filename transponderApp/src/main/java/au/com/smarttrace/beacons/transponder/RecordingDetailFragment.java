package au.com.smarttrace.beacons.transponder;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

import au.com.smarttrace.beacons.tracker.Recording;
import au.com.smarttrace.beacons.tracker.RecordingManager;
import au.com.smarttrace.beacons.tracker.Tracking;

/**
 * A fragment representing a single Recording detail screen.
 * This fragment is either contained in a {@link RecordingListActivity}
 * in two-pane mode (on tablets) or a {@link RecordingDetailActivity}
 * on handsets.
 */
public class RecordingDetailFragment extends Fragment implements RecordingManager.Callback {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private Recording item;

    private View rootView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecordingDetailFragment() {
    }

    @Override
    public synchronized void onReceive(Recording r) {
        item = r;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            final long t = getArguments().getLong(ARG_ITEM_ID);
            RecordingManager.getInstance().getById(t, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recording_detail, container, false);
        this.rootView = rootView;
        updateUI();
        return rootView;
    }

    private synchronized void updateUI() {
        if (item == null || rootView==null)
            return;

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);

        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout)
                getActivity().findViewById(R.id.toolbar_layout);
        if (appBarLayout != null)
            appBarLayout.setTitle(item.toString());

        TextView tv = (TextView) rootView.findViewById(R.id.recording_begin);
        tv.setText("Started: ");
        tv.append(df.format(item.getBegin()));
        tv = (TextView) rootView.findViewById(R.id.recording_end);
        tv.setText("Ended: ");
        tv.append(df.format(item.getEnd()));
        for (String id : item.getDeviceIds()) {
            Log.i(BeaconTransponder.TAG, "DEVICE " + id);
            Tracking t = item.getTracking(id);
            for (String k : t.getKeys())
                Log.i(BeaconTransponder.TAG, "- " + k + ": " + t.getSamples(k).size() + " readings");
//                ((TextView) rootView.findViewById(R.id.recording_detail)).setText(item.getTracking(id));
        }

    }
}
