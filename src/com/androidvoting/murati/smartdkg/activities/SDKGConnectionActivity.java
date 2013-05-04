
package com.androidvoting.murati.smartdkg.activities;

import com.androidvoting.R;
import com.androidvoting.oksana.application.UserData;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import java.io.File;

public class SDKGConnectionActivity extends Activity {

	private final String mHost = "talk.google.com";

	private final int mPort = 5222;

	private final String mServiceName = "gmail.com";
	
	private final String mUsername = UserData.getUsername();
	
	private final String mPassword = UserData.getPassword();

	private ConnectionConfiguration mConnConfig;

	private XMPPConnection mConnection;

	private final Context context = this;

	public static XMPPConnection CURRENT_CONNECTION;

	private static final String TAG = "SDKGConnectionActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sdkg_connection);

		init();
	}

	private void init() {
		Connection.DEBUG_ENABLED = true;
		mConnConfig = new ConnectionConfiguration(mHost, mPort, mServiceName);
		// force using TLS
		mConnConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		/*
		 * Truststore workaround for Android 4.x
		 * see https://github.com/Flowdalic/asmack/wiki/Truststore
		 * accessed: 21-03-2013
		 */
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			mConnConfig.setTruststoreType("AndroidCAStore");
			mConnConfig.setTruststorePassword(null);
			mConnConfig.setTruststorePath(null);
		} else {
			mConnConfig.setTruststoreType("BKS");
			String path = System.getProperty("javax.net.ssl.trustStore");
			if (path == null) {
				path = System.getProperty("java.home") + File.separator + "etc"
						+ File.separator + "security" + File.separator + "cacerts.bks";
			}
			mConnConfig.setTruststorePath(path);
		}

		mConnection = new XMPPConnection(mConnConfig);
		SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 0);
		CURRENT_CONNECTION = mConnection;
	}

	public void onButtonClick(View view) {
		switch (view.getId()) {
		case R.id.btn_connect:
			Runnable connectionRunnable = new Runnable() {
				@Override
				public void run() {
					try {
						mConnection.connect();
						if (mConnection != null && mConnection.isConnected()) {
							mConnection.login(mUsername, mPassword);
							if (mConnection.isAuthenticated()) {
								Intent intent = new Intent(context, SDKGInitiationActivity.class);
								startActivity(intent);
								finish();
							}
						}
					} catch (XMPPException e1) {
						Log.e(TAG, "could not connect to the server");
					}
				}
			};

			Thread connectionThread = new Thread(connectionRunnable);
			connectionThread.start();
			break;
		case R.id.btn_close:
			this.finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_connection, menu);
		return true;
	}

}
