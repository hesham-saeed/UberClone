/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.example.uberclone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class UberHomeActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_uber_home);

		if (getSupportActionBar() != null)
			getSupportActionBar().hide();


		if (ParseUser.getCurrentUser() == null) {
			ParseAnonymousUtils.logIn(new LogInCallback() {
				@Override
				public void done(ParseUser user, ParseException e) {
					if (e != null) {
						Log.d("Login", "Anonymous login failed");
					} else {
						Log.d("Login", "Anonymous login success");
						Log.d("Login", user.getUsername());
					}
				}
			});
		} else {
			if (ParseUser.getCurrentUser().get("riderOrDriver") != null) {
				redirectActivity();
				Log.d("Info", "Redirecting as " + ParseUser.getCurrentUser().get("riderOrDriver"));
			}
		}


		Button getStartedButton = findViewById(R.id.get_started_button);

		getStartedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Switch userTypeSwitch = findViewById(R.id.user_type_switch);

				String userType = "rider";
				if (userTypeSwitch.isChecked())
					userType = "driver";

				ParseUser.getCurrentUser().put("riderOrDriver", userType);

				ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
					@Override
					public void done(ParseException e) {
						if (e == null) {
							Log.d("User type", "saved");
							redirectActivity();
						} else {
							Log.d("User type", "Failed " + e.getMessage());
						}

					}
				});


				Log.d("Switch value", String.valueOf(userTypeSwitch.isChecked()));
				Log.d("Info", "Redirecting as " + userType);
			}
		});


		ParseAnalytics.trackAppOpenedInBackground(getIntent());
	}

	private void redirectActivity() {
		if (ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")) {
			Intent intent = new Intent(UberHomeActivity.this, RiderMapActivity.class);
			startActivity(intent);
		} else {
			Intent intent = new Intent(UberHomeActivity.this, RequestListActivity.class);
			startActivity(intent);
		}
	}

}