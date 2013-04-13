package com.murati.smartdkg.activities;

import com.murati.smartdkg.R;

import android.app.Activity;
import android.os.Bundle;
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_achievement, menu);
		return true;
	}

}
