package au.com.smarttrace.beacons.gps;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reverse geocoding
 */
public class AddressService extends IntentService {

    private static final String TAG = AddressService.class.getSimpleName();

    public static final String EXTRA_LOCATION = "extra_location";
    public static final String EXTRA_RECEIVER = "extra_receiver";
    public static final String EXTRA_RESULT = "extra_result";

    public static final int RESULT_OK = 1;
    public static final int RESULT_KO = 2;

    public AddressService() {
        super(TAG);
    }

    public static void start(Context context, Location location, ResultReceiver receiver) {
        Intent addressService = new Intent(context, AddressService.class);
        addressService.putExtra(AddressService.EXTRA_LOCATION, location);
        addressService.putExtra(AddressService.EXTRA_RECEIVER, receiver);
        context.startService(addressService);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null)
            return;

        Location location = intent.getParcelableExtra(EXTRA_LOCATION);
        ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RECEIVER);

        if (location == null)
            return;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;
        String error = "No address found";

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
        } catch (IOException ioException) {
            error = ioException.getLocalizedMessage();
            Log.e(TAG, "Network error", ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            error = illegalArgumentException.getLocalizedMessage();
            Log.e(TAG, "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(),
                    illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size() == 0) {
            Log.w(TAG, "No address found");
            deliverResult(receiver, RESULT_KO, error);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++)
                addressFragments.add(address.getAddressLine(i));
            String a = TextUtils.join(" ", addressFragments).replaceAll("\\n","");
            Log.i(TAG, "Address: " + a);
            deliverResult(receiver, RESULT_OK, a);
        }
    }

    private void deliverResult(ResultReceiver receiver, int status, String result) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_RESULT, result);
        receiver.send(status, bundle);
    }

}
