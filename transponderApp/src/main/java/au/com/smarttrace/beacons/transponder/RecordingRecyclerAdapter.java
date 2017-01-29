package au.com.smarttrace.beacons.transponder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import au.com.smarttrace.beacons.tracker.RecordingEvent;
import au.com.smarttrace.beacons.tracker.RecordingListener;
import au.com.smarttrace.beacons.tracker.Recording;
import au.com.smarttrace.beacons.tracker.RecordingManager;

/**
 *
 */
public class RecordingRecyclerAdapter extends RecyclerView.Adapter<RecordingRecyclerAdapter.ViewHolder>
                            implements RecordingListener {

    private AppCompatActivity context;

    private boolean twoPane;

    public RecordingRecyclerAdapter(AppCompatActivity context, boolean twoPane) {
        this.context = context;
        this.twoPane = twoPane;
        RecordingManager.getInstance().addRecordingListener(this);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public final TextView titleView;
        public final TextView contentView;
        public Recording.Header item;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            titleView = (TextView) view.findViewById(R.id.recording_title);
            contentView = (TextView) view.findViewById(R.id.recording_content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + titleView.getText() + "'";
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recording_list_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.item = RecordingManager.getInstance().getHeader(position);
        holder.titleView.setText(holder.item.toString());
        holder.contentView.setText(holder.item.getReadings()+" readings");
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (twoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putLong(RecordingDetailFragment.ARG_ITEM_ID, holder.item.getBegin());
                    RecordingDetailFragment fragment = new RecordingDetailFragment();
                    fragment.setArguments(arguments);
                    context.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.recording_detail_container, fragment)
                            .commit();
                } else {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, RecordingDetailActivity.class);
                    intent.putExtra(RecordingDetailFragment.ARG_ITEM_ID, holder.item.getBegin());
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return RecordingManager.getInstance().count();
    }

    @Override
    public void onRecordingChange(RecordingEvent event) {
        notifyDataSetChanged();
    }

    public void dispose() {
        RecordingManager.getInstance().removeRecordingListener(this);
    }

}
