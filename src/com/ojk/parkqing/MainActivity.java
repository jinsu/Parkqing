package com.ojk.parkqing;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
	public final static String LOCATION_NAME = "com.ojk.parkqing.LOC_NAME";
	public final static String LATITUDE = "com.ojk.parkqing.LAT";
	public final static String LONGITUDE = "com.ojk.parkqing.LON";
	public final static String ADDRESS = "com.ojk.parkqing.ADDRESS";
	public final static String VIEW_ONLY = "com.ojk.parkqing.VIEW_ONLY";

	private TextView mLatLng;
	private TextView mAddress;
	private TextView mLocName;
	private LocationManager mLocationManager;
	private Handler mHandler;
	private boolean mGeocoderAvailable;

	// UI handler codes.
	private static final int UPDATE_ADDRESS = 1;
	private static final int UPDATE_LATLNG = 2;

	private static final int TWO_METERS = 2;
	private static final int TEN_SECONDS = 10000;
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	// temporary solution
	private double mLatitude = 0;
	private double mLongitude = 0;
	private final int MULTIPLIER = 1000000;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mLatLng = (TextView) findViewById(R.id.latlng);
		mAddress = (TextView) findViewById(R.id.address);
		mLocName = (EditText) findViewById(R.id.edit_loc_name);

		// The isPresent() helper method is only available on Gingerbread or
		// above.
		mGeocoderAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& Geocoder.isPresent();

		// Handler for updating text fields on the UI like the lat/long and
		// address
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case UPDATE_ADDRESS:
					mAddress.setText((String) msg.obj);
					break;
				case UPDATE_LATLNG:
					mLatLng.setText((String) msg.obj);
					break;
				}
			}
		};

		// Get a reference to the locationManager object
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	// Skipped onSaveInstanceState() function that restores UI state after
	// rotation

	@Override
	protected void onResume() {
		super.onResume();
		setup();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Check if the GPS setting is currently enabled on the device.
		// This verification should be done during onStart() because the system
		// calls this method
		// when the user returns to the activity, which ensures the desired
		// location provider is
		// enabled each time the activity resumes from the stopped state.

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final boolean gpsEnabled = locationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		if (!gpsEnabled) {
			// Build an alert dialog here that requests that the user enable
			// the location services, then when the user clicks the "Ok" button,
			// call enableLocationSettings()
			new EnableGpsDialogFragment().show(getSupportFragmentManager(),
					"enableGpsDialog");
		}
	}

	// Method to launch settings
	private void enableLocationSettings() {
		Intent settingsIntent = new Intent(
				Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(settingsIntent);
	}

	// Stop receiving location updates whenever the Activity becomes invisible.
	@Override
	protected void onStop() {
		super.onStop();
		mLocationManager.removeUpdates(listener);
	}

	// Save the ParKQing location information
	// Will be called via the onClick attribute
	// of the buttons in activity_main.xml
	/** Called when the user clicks the Park button */
	public void sendParkMessage(View view) {
		Intent intent = new Intent(this, RecentLocationActivity.class);
		String loc = mLocName.getText().toString();
		intent.putExtra(LOCATION_NAME, loc);
		double lat = mLatitude;
		intent.putExtra(LATITUDE, lat);
		double lon = mLongitude;
		intent.putExtra(LONGITUDE, lon);
		String addr = mAddress.getText().toString();
		intent.putExtra(ADDRESS, addr);
		switch(view.getId()) {
		case R.id.save_location:
			intent.putExtra(VIEW_ONLY, false);
			break;
		case R.id.view_recent_button:
			intent.putExtra(VIEW_ONLY, true);
		}		
		startActivity(intent);
	}

	// Set up both fine and coarse location providers for use.
	private void setup() {
		Location gpsLocation = null;
		mLocationManager.removeUpdates(listener);
		mLatLng.setText(R.string.unknown);
		mAddress.setText(R.string.unknown);
		// Request updates from both coarse and fine providers
		gpsLocation = requestUpdatesFromProvider(LocationManager.GPS_PROVIDER,
				R.string.not_support_gps);
		// networkLocation = requestUpdatesFromProvider(
		// LocationManager.NETWORK_PROVIDER, R.string.not_support_network);

		// If both providers return last known locations, compare the two and
		// use the better
		// one to update the UI. If only one provider returns a location, use
		// it.
		/*
		 * if(gpsLocation !=null && networkLocation != null) {
		 * updateUILocation(getBetterLocation(gpsLocation, networkLocation)); }
		 * else if (gpsLocation != null) { updateUILocation(gpsLocation); } else
		 * if (networkLocation != null) { updateUILocation(networkLocation); }
		 */
		if (gpsLocation != null) {
			updateUILocation(gpsLocation);
		}
	}

	/**
	 * Method to register location updates with a desired location provider. If
	 * the requested provider is not available on the device, the app displays a
	 * Toast with a message referenced by a resource id.
	 * 
	 * @param provider
	 *            Name of the requested provider.
	 * @param errorResId
	 *            Resouce id for the string message to be displayed if the
	 *            provider does not exist on the device
	 * @return A previously returned {@link android.location.Location} from the
	 *         requested provider, if exists.
	 */
	private Location requestUpdatesFromProvider(final String provider,
			final int errorResId) {
		Location location = null;
		if (mLocationManager.isProviderEnabled(provider)) {
			mLocationManager.requestLocationUpdates(provider, TEN_SECONDS,
					TWO_METERS, listener);
			location = mLocationManager.getLastKnownLocation(provider);
		} else {
			Toast.makeText(this, errorResId, Toast.LENGTH_LONG).show();
		}
		return location;
	}

	private void doReverseGeocoding(Location location) {
		// since the geocoding API is synchronous and may take a while, you
		// don't want to
		// lock up the UI thread. Invoking reverse geocoding in an AsyncTask.
		(new ReverseGeocodingTask(this)).execute(new Location[] { location });
	}

	private void updateUILocation(Location location) {
		// temporary
		// TODO: a better way
		mLatitude = location.getLatitude();
		mLongitude = location.getLongitude();

		// We're sending the update to a handler which then updates the UI with
		// the new
		// location.
		Message.obtain(mHandler, UPDATE_LATLNG,
				location.getLatitude() + ", " + location.getLongitude())
				.sendToTarget();

		// Do reverse-geocoding only if the Geocoder service is available on the
		// device.
		if (mGeocoderAvailable) {
			doReverseGeocoding(location);
		} else {
			Message.obtain(mHandler, UPDATE_ADDRESS,
					"Sorry, your device doesn't support this yet. :(")
					.sendToTarget();
		}
	}

	private final LocationListener listener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			// A new location update is received. Do something useful with it.
			// Update the UI with the location update.
			updateUILocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	/**
	 * Determine whether one location reading is better than the current
	 * location fix. Code taken from
	 * http://developer.android.com/guide/topics/location
	 * /obtaining-user-location.html
	 * 
	 * @param newLocaiton
	 *            The new location that you want to evaluate
	 * @param currentBestLocation
	 *            The current location fix, to which you want to compare the new
	 *            one.
	 * @return The better location object based on recency and accuracy.
	 */
	protected Location getBetterLocation(Location newLocation,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return newLocation;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved.
		if (isSignificantlyNewer) {
			return newLocation;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return currentBestLocation;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return newLocation;
		} else if (isNewer && !isLessAccurate) {
			return newLocation;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return newLocation;
		}
		return currentBestLocation;
	}

	// Checks whether two providers are the same
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	// AsyncTask encapsulating the reverse-geocoding API. Since the geocoder API
	// is blocked,
	// we do not want to invoke it from the UI thread.
	private class ReverseGeocodingTask extends AsyncTask<Location, Void, Void> {
		Context mContext;

		public ReverseGeocodingTask(Context context) {
			super();
			mContext = context;
		}

		@Override
		protected Void doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());

			Location loc = params[0];
			List<Address> addresses = null;
			try {
				addresses = geocoder.getFromLocation(loc.getLatitude(),
						loc.getLongitude(), 1);
			} catch (IOException e) {
				e.printStackTrace();
				// Update address field with the exception.
				//Message.obtain(mHandler, UPDATE_ADDRESS, e.toString()).sendToTarget();
				Message.obtain(mHandler, UPDATE_ADDRESS, "Having Trouble...are you connected to a network?").sendToTarget();
			}
			if (addresses != null && addresses.size() > 0) {
				Address address = addresses.get(0);
				// Format the first line of address (if available), city, and
				// country name.
				String addressText = String.format(
						"%s, %s, %s",
						address.getMaxAddressLineIndex() > 0 ? address
								.getAddressLine(0) : "", address.getLocality(),
						address.getCountryName());
				// Update address field on UI.
				Message.obtain(mHandler, UPDATE_ADDRESS, addressText)
						.sendToTarget();
			}
			return null;
		}
	}

	/**
	 * Dialog to prompt users to enable GPS on the device.
	 */
	private class EnableGpsDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getActivity())
					.setTitle(R.string.enable_gps)
					.setMessage(R.string.enable_gps_dialog)
					.setPositiveButton(R.string.enable_gps,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									enableLocationSettings();
								}
							}).create();
		}
	}
}
