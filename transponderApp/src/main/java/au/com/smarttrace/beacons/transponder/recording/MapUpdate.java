package au.com.smarttrace.beacons.transponder.recording;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.LinkedList;
import java.util.List;

import au.com.smarttrace.beacons.gps.GPSDevice;
import au.com.smarttrace.beacons.tracker.Recording;
import au.com.smarttrace.beacons.tracker.Tracking;
import au.com.smarttrace.beacons.transponder.R;

public class MapUpdate extends Update implements Runnable {

    private final static int MAP_PADDING = 30;

    private GoogleMap map;

    private PolylineOptions po;
    private List<CircleOptions> cos;
    private LatLngBounds.Builder bb;

    public MapUpdate(Context context) {
        super(context);
    }

    public Update setMap(GoogleMap map) {
        this.map = map;
        return this;
    }

    @Override
    public synchronized Update load(Recording rec) {

        int gpsColor = getResources().getColor(R.color.gps);

        po = new PolylineOptions();
        cos = new LinkedList<>();
        bb = new LatLngBounds.Builder();

        // browse samples
        for (String id : rec.getDeviceIds()) {

            Tracking t = rec.getTracking(id);
            if (t==null || t.size()==0)
                continue;

            //map update
            if (GPSDevice.IDENTIFIER.equals(id)) {

                Tracking.Data<GPSDevice.Sample> samples =
                        t.getSamples(GPSDevice.KEY_LOCATION, GPSDevice.Sample.class);

                Log.i(Recording.TAG, "SAMPLES of "+GPSDevice.IDENTIFIER);
                Log.i(Recording.TAG, "- " + GPSDevice.KEY_LOCATION +
                        ": " + samples.size() + " readings");
                for (Tracking.Sample<GPSDevice.Sample> s : samples) {
                    LatLng ll = new LatLng(s.data.lat, s.data.lng);
                    po.add(ll);
                    bb.include(ll);
                    cos.add(new CircleOptions()
                            .center(ll)
                            .clickable(true)
                            .strokeWidth(0)
                            .fillColor(gpsColor)
                            .radius(s.data.acc)
                            .zIndex(2));
                }

                po.color(getResources().getColor(R.color.gps_line));
                po.width(3);
                po.zIndex(1);

            }

        }

        return this;

    }

    @Override
    public synchronized void run() {
        Log.d("MAP", "Updated");
        if (map!=null && !cos.isEmpty()) {
            map.addPolyline(po);
            for (CircleOptions co : cos)
                map.addCircle(co);
            map.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
                @Override
                public void onCircleClick(Circle circle) {
                    Toast.makeText(getContext(), circle.getId(), Toast.LENGTH_SHORT).show();
                }
            });
            map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                            bb.build(), MAP_PADDING));
        }
    }

}
