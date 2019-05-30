package com.example.uberclone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestListActivity extends AppCompatActivity {

	private ListView requestsListView;
	private ArrayAdapter<String> requestsArrayAdapter;
	private List<String> requestsList = Collections.synchronizedList(new ArrayList<String>());
	private LocationRequest locationRequest;
	private LocationCallback locationCallback;
	private ArrayList<Double> requestLatitude = new ArrayList<>(), requestLongitude = new ArrayList<>();
	private ArrayList<String> usernameList = new ArrayList<>();
	private Double driverLat, driverLng;

	private FusedLocationProviderClient fusedLocationProviderClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_list);

		if (getSupportActionBar() != null)
			getSupportActionBar().setTitle("Nearby Requests");

		requestsList.clear();
		requestsList.add("Getting nearby requests...");

		requestsListView = findViewById(R.id.requests_list_view);
		requestsArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requestsList);
		requestsListView.setAdapter(requestsArrayAdapter);


		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
		buildLocationRequest();
		buildLocationCallback();
		requestUserLocation();

		requestsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (!(requestsList.get(0).equals("No nearby requests") && requestsList.get(0).equals("Getting nearby requests..."))) {
					Intent driverLocationIntent = new Intent(RequestListActivity.this, DriverMapActivity.class);

					driverLocationIntent.putExtra("requestLatitude", requestLatitude.get(position));
					driverLocationIntent.putExtra("requestLongitude", requestLongitude.get(position));
					driverLocationIntent.putExtra("driverLatitude", driverLat);
					driverLocationIntent.putExtra("driverLongitude", driverLng);
					driverLocationIntent.putExtra("requestUserName", usernameList.get(position));
					startActivity(driverLocationIntent);

				}

			}
		});

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 333 && grantResults.length > 0) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				requestUserLocation();
			}
		}
	}

	private void requestUserLocation() {
		Log.d("RequestListActivity", "requestUserLocation()");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 333);
			} else {
				fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
			}
		} else {
			fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
		}
	}

	private void buildLocationRequest() {
		locationRequest = new LocationRequest();
		locationRequest.setInterval(5000);
		locationRequest.setFastestInterval(5000);
		locationRequest.setSmallestDisplacement(1);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	private void buildLocationCallback() {
		locationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				if (locationResult != null) {
					Log.d("RequestListActivity", "onLocationResult()");
					Location location = locationResult.getLastLocation();
					updateRequestsList(location);
					updateUserLocationOnServer(location);
				}
			}
		};
	}

	private void updateUserLocationOnServer(final Location location) {
		Log.d("RequestListActivity", "Entering updateUserLcationOnServer()");

		ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(), location
				.getLongitude()));

		ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
			@Override
			public void done(ParseException e) {
				if (e == null)
					Log.d("RequestListActivity", "Driver Location Updated on Parse");
				else
					Log.d("RequestListActivity", "Failed to Update Driver location on Parse");
			}
		});

	}

	private void updateRequestsList(final Location location) {
		if (location != null) {

			requestsList.clear();
			requestLatitude.clear();
			requestLongitude.clear();

			ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
			Log.d("RequestListActivity", "Querying nearby requests for " + location.toString());

			final ParseGeoPoint driverGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

			query.whereNear("geopoint", driverGeoPoint);
			query.whereDoesNotExist("driverUserName");
			query.setLimit(10);

			query.findInBackground(new FindCallback<ParseObject>() {
				@Override
				public void done(List<ParseObject> objects, ParseException e) {
					if (e == null && objects.size() > 0) {

						for (ParseObject object : objects) {

							Double distance = driverGeoPoint.distanceInKilometersTo(object.getParseGeoPoint("geopoint"));

							Double roundedDistance = (double) Math.round(distance * 10) / 10;

							requestsList.add(roundedDistance.toString() + " KM");
							requestLatitude.add(object.getParseGeoPoint("geopoint").getLatitude());
							requestLongitude.add(object.getParseGeoPoint("geopoint").getLongitude());

							usernameList.add((String) object.get("username"));

						}
						driverLat = location.getLatitude();
						driverLng = location.getLongitude();

					} else {
						requestsList.add("No nearby requests");
					}
					/*requestsListView.setAdapter(
							new ArrayAdapter<String>(RequestListActivity.this,
									android.R.layout.simple_list_item_1,
									requestsList));*/
					requestsArrayAdapter.notifyDataSetChanged();
				}
			});

		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		fusedLocationProviderClient.removeLocationUpdates(locationCallback);
	}

	@Override
	protected void onResume() {
		super.onResume();
		requestUserLocation();
	}
}
