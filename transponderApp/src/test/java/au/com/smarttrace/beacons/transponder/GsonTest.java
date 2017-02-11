package au.com.smarttrace.beacons.transponder;

import android.location.Location;
import android.os.SystemClock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;


import au.com.smarttrace.beacons.DeviceManager;
import au.com.smarttrace.beacons.gps.GPSDevice;
import au.com.smarttrace.beacons.tracker.RecordingManager;
import au.com.smarttrace.beacons.tracker.Tracking;

public class GsonTest {

    @Test
    public void testSerialize() {

        Tracking t = new Tracking();
        t.addSample("location", new GPSDevice.Sample(10,10,10));
        try {Thread.sleep(100);} catch(InterruptedException e) {}
        t.addSample("location", new GPSDevice.Sample(12,13,1));
        t.addSample("temperature", 22f);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Tracking.Data.class, new JsonDeserializer<Tracking.Data>() {
                    @Override
                    public Tracking.Data<GPSDevice.Sample> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
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
                })
                .create();

        String json = gson.toJson(t);
        System.out.println(json);
        Object deserialzed = gson.fromJson(json, Tracking.class);
        System.out.println(deserialzed);
        assertEquals("{location=[(10.0,10.0), (12.0,13.0)], temperature=[22.0]}", deserialzed.toString());

    }

}
