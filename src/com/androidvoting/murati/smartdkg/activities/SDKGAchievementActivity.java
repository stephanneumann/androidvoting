package com.androidvoting.murati.smartdkg.activities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;

import org.spongycastle.openssl.PEMWriter;

import com.androidvoting.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class SDKGAchievementActivity extends Activity {
	
	private TextView mPrivateKeyShareView;

	private TextView mPrivateShareView;

	private TextView mPublicKeyView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sdkg_achievement);

		mPrivateKeyShareView = (TextView) findViewById(R.id.id_tv_privatekey_share_xi);
		mPrivateShareView = (TextView) findViewById(R.id.id_tv_private_share_xi_);
		mPublicKeyView = (TextView) findViewById(R.id.id_tv_publickey);

		String[] values = getIntent().getExtras().getStringArray(SDKGActivity.COMPUTED_VALUES);

		mPrivateKeyShareView.setText(values[0]);
		mPrivateShareView.setText(values[1]);
		mPublicKeyView.setText(values[2]);
		//this.storePublicKey("public_key", values[2]);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_achievement, menu);
		return true;
	}
	
	public void storePublicKey(String filename, String publicKey) {
		File dir =  getExternalFilesDir(null);
		File file = new File(dir, filename + ".pem");
		BufferedWriter bufWriter = null;
		PEMWriter pemWriter = null;

		try {
			bufWriter = new BufferedWriter(new FileWriter(file));
			pemWriter = new PEMWriter(bufWriter);
			pemWriter.flush();
			pemWriter.writeObject(publicKey);
		} catch (IOException e) {
			Log.e("xx", "could not create new bufferedwriter, maybe the file with the specified filename could not be found");
		} finally {
			try {
				pemWriter.close();
				bufWriter.close();
			} catch (IOException e) {
				Log.e("xx", "could not close bufferedwriter");
			}
		}
	}

}
