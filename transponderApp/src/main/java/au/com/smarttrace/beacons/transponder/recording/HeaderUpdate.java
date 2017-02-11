package au.com.smarttrace.beacons.transponder.recording;

import android.content.Context;
import android.support.design.widget.CollapsingToolbarLayout;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;

import au.com.smarttrace.beacons.tracker.Recording;
import au.com.smarttrace.beacons.transponder.R;

public class HeaderUpdate extends Update implements Runnable {

    private CollapsingToolbarLayout appBarLayout;

    private Recording recording;

    private View rootView;

    public HeaderUpdate(Context context) {
        super(context);
    }

    @Override
    public Update load(Recording recording) {
        this.recording = recording;
        return this;
    }

    public void init(CollapsingToolbarLayout appBarLayout, View rootView) {
        this.appBarLayout = appBarLayout;
        this.rootView = rootView;
    }

    @Override
    public void run() {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
        if (appBarLayout != null)
            appBarLayout.setTitle(recording.toString());
        TextView tv = (TextView) rootView.findViewById(R.id.recording_begin);
        tv.setText("Started: ");
        tv.append(df.format(recording.getBegin()));
        tv = (TextView) rootView.findViewById(R.id.recording_end);
        tv.setText("Ended: ");
        tv.append(df.format(recording.getEnd()));
    }

}
