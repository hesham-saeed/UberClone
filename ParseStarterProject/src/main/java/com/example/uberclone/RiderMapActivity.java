package com.example.uberclone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class RiderMapActivity extends FragmentActivity implements OnMapReadyCallback {

	private static final String TAG = "RiderMapActivity";
	private GoogleMap mMap;
	private Button callUberButton, logoutButton;
	private FusedLocationProviderClient fusedLocationProviderClient;
	private LocationRequest locationRequest;
	private LocationCallback locationCallback;

	private double latitude;
	private double longitude;

	private Handler handler = new Handler();

	private boolean rideInProgress = false;

	private Boolean driverActive = false;

	private TextView uberStatusTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rider_map);

		callUberButton = findViewById(R.id.call_uber_button);
		logoutButton = findViewById(R.id.logout_button);

		buildLocationRequest();
		buildLocationCallback();

		uberStatusTextView = findViewById(R.id.status_text_view);

		ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
		query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null) {
					if (objects.size() > 0) {
						rideInProgress = true;
						callUberButton.setText("CANCEL UBER");

						checkRequestUpdates();

					}
				}
			}
		});

		logoutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ParseUser.logOut();
				Intent intent = new Intent(RiderMapActivity.this, UberHomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});


		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);

		mapFragment.getMapAsync(this);
	}

	private boolean lastLocationExists() {

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

			fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

			if (fusedLocationProviderClient.getLastLocation() != null)
				return true;

		}
		return false;
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		callUberButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (!rideInProgress) {

					if (!lastLocationExists()) {
						Toast.makeText(RiderMapActivity.this, "Could not find a location, Please try again later", Toast.LENGTH_SHORT).show();
						return;
					}
					ParseObject request = new ParseObject("Request");
					request.put("geopoint", new ParseGeoPoint(latitude, longitude));
					request.put("username", ParseUser.getCurrentUser().getUsername());
					request.saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException e) {
							if (e == null) {
								rideInProgress = true;
								callUberButton.setText("CANCEL UBER");
								Log.d("Request", "Success");

								checkRequestUpdates();

							} else {
								Log.d("Request", "Failed");
							}
						}
					});

				} else {

					ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
					query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

					query.findInBackground(new FindCallback<ParseObject>() {
						@Override
						public void done(List<ParseObject> objects, ParseException e) {
							if (e == null && objects.size() > 0) {
								for (ParseObject object : objects) {
									object.deleteInBackground();
								}

								rideInProgress = false;
								callUberButton.setText("CALL UBER");
							} else {
								Log.d("RideInProgress", "No previous requests found");
							}
						}
					});

				}

			}
		});

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

		requestUserLocation();

	}

	private void checkRequestUpdates() {

		ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
		query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
		query.whereExists("driverUserName");

		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null && objects.size() > 0) {

					driverActive = true;


					String driverUserName = objects.get(0).getString("driverUserName");
					Log.d("driverUserName", driverUserName);

					ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
					userQuery.whereEqualTo("username", driverUserName);

					userQuery.findInBackground(new FindCallback<ParseUser>() {
						@Override
						public void done(List<ParseUser> objects, ParseException e) {
							if (e == null && objects.size() > 0) {
								uberStatusTextView.setText("Your ride is on the way");

								ParseGeoPoint driverGeoPoint = objects.get(0).getParseGeoPoint("location");
								Log.d(TAG, "driver location " + driverGeoPoint.getLatitude() + "," + driverGeoPoint.getLongitude());
								LatLng driverLatLng = new LatLng(driverGeoPoint.getLatitude(), driverGeoPoint.getLongitude());
								LatLng userLatLng = new LatLng(latitude, longitude);;

								ParseGeoPoint userGeoPoint = new ParseGeoPoint(latitude, longitude);

								Double distance = driverGeoPoint.distanceInKilometersTo(userGeoPoint);
								Double roundedDistance = (double) Math.round(distance * 10) / 10;

								if (distance < 0.01){
									handler.postDelayed(new Runnable() {
										@Override
										public void run() {
											rideInProgress = false;
											driverActive = false;
											ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
											query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

											query.findInBackground(new FindCallback<ParseObject>() {
												@Override
												public void done(List<ParseObject> objects, ParseException e) {
													if (e == null){
														for (ParseObject object:objects)
															object.deleteInBackground();
													}
												}
											});
											uberStatusTextView.setText("Your ride is here");

											callUberButton.setText("CALL UBER");
										}
									}, 5000);
								} else {

									final LatLngBounds latLngBounds = LatLngBounds.builder().include(driverLatLng).include(userLatLng).build();
									final int padding = 70;

									userLatLng = new LatLng(latitude, longitude);

									mMap.clear();

									mMap.addMarker(new MarkerOptions()
											.position(driverLatLng)
											.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

									mMap.addMarker(new MarkerOptions()
											.position(userLatLng)
											.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

									mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, padding));

									handler.postDelayed(new Runnable() {
										@Override
										public void run() {
											Log.d(TAG, "Running checkRequestUpdates() every 2 sec");
											checkRequestUpdates();
										}
									}, 4000);
								}

							}
						}
					});

				}



			}
		});

	}

	private void requestUserLocation() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
			} else {
				if (fusedLocationProviderClient != null)
					fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
			}
		} else {
			if (fusedLocationProviderClient != null)
				fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 1000 && grantResults.length > 0) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					if (fusedLocationProviderClient != null)
						fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
				}
			}
		}
	}

	private void buildLocationRequest() {
		locationRequest = new LocationRequest();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setInterval(0);
		locationRequest.setFastestInterval(1000);
	}

	private void buildLocationCallback() {
		locationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				if (locationResult != null) {
					Location location = locationResult.getLastLocation();
					udpateMap(location);
				}
			}
		};
	}

	private void udpateMap(Location location) {
		if (location != null && driverActive == false) {
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			LatLng position = new LatLng(latitude, longitude);
			mMap.clear();
			mMap.addMarker(new MarkerOptions().position(position).title("My Location"));
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (fusedLocationProviderClient != null)
			fusedLocationProviderClient.removeLocationUpdates(locationCallback);
	}

	@Override
	protected void onResume() {
		super.onResume();
		requestUserLocation();
	}
}
