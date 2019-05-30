package com.example.uberclone;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback {

	private GoogleMap mMap;
	private Double driverLat, driverLng, requestLat, requestLng;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_driver_map);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		if (getIntent() != null) {

			driverLat = getIntent().getDoubleExtra("driverLatitude", 0);
			driverLng = getIntent().getDoubleExtra("driverLongitude", 0);
			requestLat = getIntent().getDoubleExtra("requestLatitude", 0);
			requestLng = getIntent().getDoubleExtra("requestLongitude", 0);
			LatLng driverPosition = new LatLng(driverLat, driverLng);
			LatLng requestPosition = new LatLng(requestLat, requestLng);

			mMap.addMarker(new MarkerOptions()
					.position(driverPosition)
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

			mMap.addMarker(new MarkerOptions()
					.position(requestPosition)
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

			final LatLngBounds latLngBounds = LatLngBounds.builder().include(driverPosition).include(requestPosition).build();
			final int padding = 60;


			RelativeLayout containerLayout = findViewById(R.id.container_layout);
			containerLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, padding));
				}
			});

		}

		Button getDirectionsButton = findViewById(R.id.directions_button);
		getDirectionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");

				query.whereEqualTo("username", getIntent().getStringExtra("requestUserName"));

				query.findInBackground(new FindCallback<ParseObject>() {
					@Override
					public void done(List<ParseObject> objects, ParseException e) {
						if (e == null && objects.size() > 0) {
							for (ParseObject parseObject : objects) {
								parseObject.put("driverUserName", ParseUser.getCurrentUser().getUsername());

								parseObject.saveInBackground(new SaveCallback() {
									@Override
									public void done(ParseException e) {
										if (e == null) {
											String uri = "http://maps.google.com/maps?";
											uri += driverLat + "," + driverLng + "&" +
													"daddr=" + requestLat + "," + requestLng;

											Intent intent = new Intent(Intent.ACTION_VIEW,
													//Uri.parse("http://maps.google.com/maps?saddr=20.344,34.34&daddr=20.5666,45.345"));
													Uri.parse(uri));
											startActivity(intent);

										}
									}
								});

							}
						}
					}
				});

			}
		});

		// Add a marker in Sydney and move the camera
		/*LatLng sydney = new LatLng(-34, 151);
		mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
		mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
	}
}
