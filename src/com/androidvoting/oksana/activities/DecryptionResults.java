package com.androidvoting.oksana.activities;

import com.androidvoting.R;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class DecryptionResults extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_decryption_results);
		for (SDKGZqElement vote : DecryptionActivity.DECRYPTED_VOTES) {
			TextView tx = new TextView(this);
			tx.setText(vote.toString());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_decryption_results, menu);
		return true;
	}

}
