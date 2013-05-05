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
		//setContentView(R.layout.activity_decryption_results);
		TextView tx = new TextView(this);
		String msg = "Votes: \n";
		for (SDKGZqElement vote : DecryptionActivity.DECRYPTED_VOTES) {
			msg += vote.toString() + "\n";
		}
		tx.setText(msg);
		setContentView(tx);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_decryption_results, menu);
		return true;
	}

}
