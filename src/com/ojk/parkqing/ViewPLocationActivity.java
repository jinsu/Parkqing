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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ViewPLocationActivity extends FragmentActivity {
	private TextView mLatLng;
	private TextView mAddr;
	private TextView mDist;
	private String mLocName;
	private String mCoordStr;
	private Double mLon;
	private Double mLat;
	private long mId;
	
	private final static int earthRadius = 6371; //km
	
	private LocationManager mLocationManager;
	private Handler mHandler;
	private boolean mGeocoderAvailable;

	// UI handler codes.
	private static final int UPDATE_ADDRESS = 1;
	private static final int UPDATE_LATLNG = 2;
	private static final int UPDATE_DISTANCE = 3;

	private static final int TWO_METERS = 2;
	private static final int TEN_SECONDS = 10000;
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	// temporary solution
	public static double mLatitude = 0;
	public static double mLongitude = 0;

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    //hookup the layout
	    setContentView(R.layout.activity_view_plocation);
	    
	    mLatLng = (TextView) findViewById(R.id.view_ploc_latlng);
	    mAddr = (TextView) findViewById(R.id.view_ploc_addr);
	    mDist = (TextView) findViewById(R.id.view_ploc_distance);
	    
		// Handler for updating text fields on the UI like the lat/long and
		// address
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case UPDATE_LATLNG:
					mLatLng.setText((String) msg.obj);
					break;
				case UPDATE_DISTANCE:
					mDist.setText((String) msg.obj);
				}
			}
		};

		// Get a reference to the locationManager object
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    
	    //bring in the msg from intent
	    Intent intent = getIntent();
		retrieveIntentMsg(intent);	    
	}
	
	@Override
	public void onResume() {
		super.onResume();
		setup();
	}

	@Override
	public void onStart() {
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
		super.onStart();
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
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	private String calculateDistance() {
		/**
		 * var dLat = (lat2-lat1).toRad();
var dLon = (lon2-lon1).toRad();
var lat1 = lat1.toRad();
var lat2 = lat2.toRad();
var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
var d = R * c;
		 **/
		//mLat
		//double dLat = (m)
		//MainActivity.mLatitude;
		//MainActivity.mLongitude;
		return "-5 mile(s)";
	}
	
	private void retrieveIntentMsg(Intent intent) {
		mLocName = intent.getStringExtra(PLocation.LOCATION_NAME);
		mLon = intent.getDoubleExtra(PLocation.LONGITUDE, 0);
		mLat = intent.getDoubleExtra(PLocation.LATITUDE, 0);
		mId = intent.getLongExtra(PLocation.LOCATION_ID, 0);
		mCoordStr = intent.getStringExtra(PLocation.COORD_STR);
	}
	
	public void sendToNavigation(View view) {
		Intent intentGmap = new Intent(android.content.Intent.ACTION_VIEW,
		Uri.parse("http://maps.google.com/maps?saddr=&daddr=" + mCoordStr + "&sensor=true"));
		startActivity(intentGmap);
	}
	
	// Set up both fine and coarse location providers for use.
	private void setup() {
		Location gpsLocation = null;
		mLocationManager.removeUpdates(listener);
		mLatLng.setText(R.string.unknown);
		mAddr.setText(R.string.unknown);
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
		Message.obtain(mHandler, UPDATE_DISTANCE, calculateDistance()).sendToTarget();

		// Do reverse-geocoding only if the Geocoder service is available on the
		// device.
		if (mGeocoderAvailable) {
			doReverseGeocoding(location);
		} else {
			Message.obtain(mHandler, UPDATE_ADDRESS,
					"Sorry, your device doesn't support this yet. :(")
					.sendToTarget();
		}
		
		//TODO: change distance
		//TODO: change gps coordinate
		//TODO: change direction arrow
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
