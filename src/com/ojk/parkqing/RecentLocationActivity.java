package com.ojk.parkqing;

import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class RecentLocationActivity extends ListActivity {
	private PLocationDataSource datasource;
	private ListView rList;

	private double rLat;
	private double rLon;
	private String rLocName;
	private String rAddr;
	private String curLocString;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recent_location);

		datasource = new PLocationDataSource(this);
		datasource.open();
		
		// set a localized string for "Current Location"
		curLocString = "Current%20Location";
		
		rList = getListView();
		List<PLocation> values = datasource.getAllPLocations();

		// Use the SimpleCursorAdapter to show the
		// elements in a ListView
		ArrayAdapter<PLocation> adapter = new ArrayAdapter<PLocation>(this,
				android.R.layout.simple_list_item_1, values);
		setListAdapter(adapter);
	}
	
	// Will be called via the list item in activity_recent_location.xml
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Object o = rList.getItemAtPosition(position);
		PLocation loc = (PLocation) o;
		//String msg = loc.toString();
		/* Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
				Uri.parse("http://maps.google.com/maps?saddr=" + curLocString
						+ "&daddr=" + loc.getCoordString() + "&sensor=true")); */
		/*TODO: Cross-platform solution for launching maps application.
		 * Leaving saddr= as blank gives current location for android but it won't work for iphone.
		 */
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
				Uri.parse("http://maps.google.com/maps?saddr=&daddr=" + loc.getCoordString() + "&sensor=true"));
		startActivity(intent);
		/* Toast.makeText(getApplicationContext(), "You have chosen: " + msg, Toast.LENGTH_LONG).show();*/
	}
	@Override
	protected void onStart() {
		// Get the Message
		saveLocation(getIntent());
		super.onStart();
	}

	// Will be called via the onClick attribute
	// of the buttons in activity_main.xml
	public void saveLocation(Intent intent) {
		@SuppressWarnings("unchecked")
		ArrayAdapter<PLocation> adapter = (ArrayAdapter<PLocation>) getListAdapter();
		retrieveIntentMsg(intent);

		// Save the new PLocation to the database
		PLocation ploc = datasource.createLocation(rLocName, rLat, rLon);
		adapter.add(ploc);
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onResume() {
		datasource.open();
		retrieveIntentMsg(getIntent());
		super.onResume();
	}

	@Override
	protected void onPause() {
		datasource.close();
		super.onPause();
	}

	private void retrieveIntentMsg(Intent intent) {
		rLocName = intent.getStringExtra(MainActivity.LOCATION_NAME);
		rLon = intent.getDoubleExtra(MainActivity.LONGITUDE, 0);
		rLat = intent.getDoubleExtra(MainActivity.LATITUDE, 0);
		rAddr = intent.getStringExtra(MainActivity.ADDRESS);
	}
}
