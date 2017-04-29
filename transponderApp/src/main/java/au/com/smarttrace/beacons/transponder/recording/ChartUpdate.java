package au.com.smarttrace.beacons.transponder.recording;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.com.smarttrace.beacons.gps.GPSDevice;
import au.com.smarttrace.beacons.tracker.Recording;
import au.com.smarttrace.beacons.tracker.Tracking;
import au.com.smarttrace.beacons.transponder.R;

public class ChartUpdate extends Update implements Runnable {

    private boolean groupingByKey;

    private LineChart chart;

    private Spinner spinner;

    private Map<String, LineDataSet> dataMap;

    private ArrayList<Map<String, ?>> spinnerList;

    private long begin, end;

    /** Selected dataset */
    private String label;

    private final static String FROM_LABEL = "label";
    private final static String FROM_COUNT = "count";

    public ChartUpdate(Context context) {
        super(context);
    }

    /**
     * @param rootView
     *              the detail fragment rootView
     */
    public void init(View rootView) {
        chart = (LineChart) rootView.findViewById(R.id.recording_chart);
        chart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return begin>0
                        ? DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                            .format(new Date(begin + (long) value))
                        : "-";
            }
        });
        chart.setNoDataText("No data");
        spinner = (Spinner) rootView.findViewById(R.id.recording_dataset);
    }

    /**
     * Load new recording data
     *
     * @param rec
     *              the recording
     */
    @Override
    public synchronized Update load(Recording rec) {

        dataMap = new LinkedHashMap<>();
        begin = rec.getBegin().getTime();
        end = rec.getEnd().getTime();

        Map<String, List<Entry>> entryMap = new LinkedHashMap<>();

        // browse samples
        for (String id : rec.getDeviceIds()) {

            Tracking t = rec.getTracking(id);
            if (t == null || t.size() == 0 || GPSDevice.IDENTIFIER.equals(id))
                continue;

            Log.i(Recording.TAG, "SAMPLES of " + id);

            for (String key : t.getKeys()) {

                String dataKey = groupingByKey ? key : id + "/" + key;
                Tracking.Data samples = t.getSamples(key);
                Log.i(Recording.TAG, "- " + key + ": " + samples.size() + " readings");

                List<Entry> entries = entryMap.get(dataKey);
                if (entries == null) {
                    entries = new ArrayList<>();
                    entryMap.put(dataKey, entries);
                }

                for (Object obj : samples) {
                    Tracking.Sample s = (Tracking.Sample) obj;
                    Log.i("Sample", "  " + (s.time - begin) + "=" + s.data.toString());
                    try {
                        float x = s.time - begin;
                        float y = Float.parseFloat(s.data.toString());
                        entries.add(new Entry(x, y));
                    } catch (NumberFormatException e) {
                        Log.e(Recording.TAG, "Unexpected sample: " + s.data, e);
                    }
                }


            }

        }

        createDatasets(entryMap);
        initSpinner();
        return this;
    }

    private void createDatasets(Map<String, List<Entry>> entryMap) {
        for (String key: entryMap.keySet()) {
            List<Entry> entries = entryMap.get(key);
            Collections.sort(entries, new Comparator<Entry>() {
                @Override
                public int compare(Entry o1, Entry o2) {
                    return (int) (o1.getX() - o2.getX());
                }
            });

            LineDataSet dataSet = new LineDataSet(entries, key);

            int i=key.indexOf('/');
            String colorKey = i<0 ? key : key.substring(i+1);
            int c = getResources().getIdentifier(colorKey, "color", R.class.getPackage().getName());
            int color = getResources().getColor(c);

            dataSet.setColor(color);
            dataSet.setCircleColor(color);
            dataSet.setLineWidth(2);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setCubicIntensity(0.2f);
            dataSet.setDrawCircles(true);
            dataSet.setDrawCircleHole(true);
            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
//                    dataSet.calcMinMax();
//                    dataSet.setDrawValues(true);
//                        dataSet.setDrawFilled(true);
//                        dataSet.setDrawValues(true);
//                        dataSet.calcMinMax();
//                        dataSet.setDrawHighlightIndicators(true);
//                        dataSet.setValueFormatter(s.data.formatter);
            dataMap.put(key, dataSet);
        }
    }

    /**
     * Init the datasets spinner
     */
    private void initSpinner() {
        //label = null; ??? reset label
        spinnerList = new ArrayList<>();
        for (Map.Entry<String, LineDataSet> e : dataMap.entrySet()) {
            String k = e.getKey();
            LineDataSet s = e.getValue();
            if (label==null)
                label = k;
            Map<String, String> item = new HashMap<>();
            item.put(FROM_LABEL, k);
            item.put(FROM_COUNT, String.valueOf(s.getEntryCount()));
            spinnerList.add(item);
            Entry last = s.getEntryForIndex(s.getEntryCount()-1);
            if (last.getX()<end)
                s.addEntry(new Entry(end-begin, last.getY()));
        }
    }

    public synchronized boolean isGroupingByKey() {
        return groupingByKey;
    }

    public synchronized ChartUpdate setGroupingByKey(boolean enabled) {
        groupingByKey = enabled;
        return this;
    }

    //TODO
    public synchronized ChartUpdate selectDevice(String deviceId) {
//        this.deviceId = deviceId;
        return this;
    }

    public synchronized ChartUpdate selectDataset(int position) {
        Map<String, String> item = (Map<String, String>)
                spinner.getItemAtPosition(position);
        this.label = item.get(FROM_LABEL);
        return this;
    }

    public synchronized ChartUpdate removeDataset() {
        this.label = null;
        return this;
    }

    @Override
    public synchronized void run() {
        Log.d("CHART", "Updated");
        if (spinnerList!=null)
            spinner.setAdapter(new SimpleAdapter(
                    getContext(), spinnerList, R.layout.dataset,
                    new String[]{FROM_LABEL, FROM_COUNT},
                    new int[]{R.id.dataset_label, R.id.dataset_count}));
        spinnerList = null;
        chart.setData(label!=null ? new LineData(dataMap.get(label)) : null);
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

}
