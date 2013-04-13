
package com.murati.smartdkg.activities;

import com.murati.smartdkg.R;
import com.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.murati.smartdkg.dkg.SDKGFeldmanVSS;
import com.murati.smartdkg.dkg.SDKGPedersenVSS;
import com.murati.smartdkg.dkg.SDKGPlayer;
import com.murati.smartdkg.dkg.SDKGPlayerList;
import com.murati.smartdkg.dkg.commitments.SDKGShare;
import com.murati.smartdkg.util.SDKGKeyFactory;

import org.jivesoftware.smack.XMPPConnection;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

/*
 * represents the dkg protocol by gennaro et al. - it runs an instance of pedersenVSS to compute the players secret share
 * from secret key x and also an instance of feldmanVSS to gain the jointly generated public key y
 * 
 * output: the shares of the jointly generated secret key x - namely xi, also a second share xi' and the public key y
 */
public class SDKGActivity extends Activity {

	private XMPPConnection mConnection = SDKGConnectionActivity.CURRENT_CONNECTION;

	private ListView mPlayerListView;

	//////////////////////////////////////////////////

	private SDKGElGamalParameters mParameters;

	private SDKGPlayerList mPlayerList;

	private SDKGPedersenVSS mPedersenVSS;

	private SDKGFeldmanVSS mFeldmanVSS;

	private SDKGPlayer mMyself;

	//////////////////////////////////////////////////

	private final Context context = this;

	private SecretKey mKey; // AES encryption and decryption key

	private RSAPrivateCrtKeyParameters mPrivateKey; // RSA private key

	private final String mSessionId = UUID.randomUUID().toString();

	private BigInteger mPrivateKeyShare; // xi

	private BigInteger mPrivateShare; // xi'

	private BigInteger mPublicKey; // y

	//////////////////////////////////////////////////

	private static final String TAG = "SDKGActivity";

	private ProgressDialog progressDialog;

	public static final String COMPUTED_VALUES = "com.murati.smartdkg.activities.PUBLICKEY_AND_SHARES";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sdkg_execution);

		mPlayerListView = (ListView)findViewById(R.id.lv_participants);

		Bundle bundle = this.getIntent().getExtras();
		String[] buddies = bundle.getStringArray(SDKGInitiationActivity.PLAYER);

		mPlayerList = new SDKGPlayerList(buddies.length + 1);
		mPlayerList.add(null); // there is no player using index 0!

		for (int j = 0; j < buddies.length; j++) {
			mPlayerList.add(new SDKGPlayer(j + 1, buddies[j]));

			if (buddies[j].equals(mConnection.getUser())) {
				mMyself = new SDKGPlayer(j + 1, buddies[j]);
			}
		}

		List<String> buddyList = new LinkedList<String>(Arrays.asList(buddies));
		buddyList.remove(mConnection.getUser());

		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, buddyList);
		mPlayerListView.setAdapter(arrayAdapter);

		initParameters();
		initLoadKeys();

		new PrivateKeyShareComputationTask().execute();
	}

	private void initLoadKeys() {
		mKey = SDKGKeyFactory.loadAESKey("aeskey");
		try {
			mPrivateKey = SDKGKeyFactory.loadRSAPrivateKey("privkey");
		} catch (IOException ex) {
			Log.e(TAG, "could not load RSA private key from file");
		}
	}

	private void initParameters() {
		mParameters = new SDKGElGamalParameters(
				new BigInteger("111930019642068417438074558300694340156587956555698629470914131330836670314842427722798502002216870944902305931917588433031384121138088787698419389634718106738115794820133084240137242264042332989162493496520794368826169308133135109412816743246243527430440142753378507026369668076190619700799456487551876054207"),
				new BigInteger("55965009821034208719037279150347170078293978277849314735457065665418335157421213861399251001108435472451152965958794216515692060569044393849209694817359053369057897410066542120068621132021166494581246748260397184413084654066567554706408371623121763715220071376689253513184834038095309850399728243775938027103"),
				new BigInteger("12318509937837929888838834232455081313733322516848738249994989592259774200977125571000223800320587912245829436105757095160635147284621372531144192989814017701233914661663531485996120741252505525438154553361499556894206592144928920270979230704532945448311642936631812488656116464564395748559909980854789757588"),
				new BigInteger("61601198452968981934024243593568896584644050084321195545281847260353806930420468138535099784007003507618238635858101694751395783669607650928427616580394080127775693780082110707911448994586005969639583176124058109021976128110393786060838259744480859019692922346626419323542740924919921871260219412702982287239"),
				(mPlayerList.size() - 1) / 2);
	}

	// Step 1: run an instance of pedersenVSS
	public void runPedersenVSS() {
		mPedersenVSS = new SDKGPedersenVSS(
				mParameters,
				mKey,
				mPrivateKey,
				mMyself,
				mPlayerList,
				mSessionId,
				mConnection,
				context);

		mPedersenVSS.execute();
	}

	// Step 2: build a list with all qualified tellers (QUAL := PLAYERLIST \ DISQUALIFIED)
	public void buildQualList() {
		mPlayerList.removeDisqualifiedPlayers();
	}

	// Step 3: compute the shares xi of a secret key x and xi' for each player
	public BigInteger[] computePrivateKeyShares() {
		BigInteger xi = mPedersenVSS.getMyShares()[0].getValue(); // initialize with sii
		BigInteger xi_ = mPedersenVSS.getMyShares()[1].getValue(); // initialize with sii'

		// compute xi = sum(j in QUAL) sji mod q
		for (SDKGPlayer p : mPlayerList) {
			if (p == null || p.equals(mMyself)) {
				continue;
			}

			SDKGShare sji = mPedersenVSS.getReceivedShares().get(p)[0];
			xi = xi.add(sji.getValue()).mod(mParameters.getQ());

			SDKGShare sji_ = mPedersenVSS.getReceivedShares().get(p)[1];
			xi_ = xi_.add(sji_.getValue()).mod(mParameters.getQ());
		}
		xi = xi.mod(mParameters.getQ());
		xi_ = xi_.mod(mParameters.getQ());

		return new BigInteger[] {xi, xi_};
	}

	// Step 4: run an instance of feldmanVSS
	public void runFeldmanVSS() {
		mFeldmanVSS = new SDKGFeldmanVSS(
				mPedersenVSS,
				mParameters,
				mPrivateKey,
				mMyself,
				mPlayerList,
				mSessionId,
				mConnection,
				context);

		mFeldmanVSS.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_key_generation, menu);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	private class PrivateKeyShareComputationTask extends AsyncTask<Void, String, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(context);
			progressDialog.setTitle("Running PedersenVSS");
			progressDialog.setMessage("compute commitments and shares...");
			progressDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			runPedersenVSS();
			String state[] = new String[1];

			while (!mPedersenVSS.executionFinished()) {
				try {
					Thread.sleep(500);
					state[0] = mPedersenVSS.getCurrentState().toString();
				} catch (InterruptedException e) {
					Log.e(TAG, "error during thread execution");
				}
				publishProgress(state);
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... params) {
			super.onProgressUpdate(params);
			String progress = "";

			if (params[0].equals("COMMITMENTS_AND_SHARES_COMPUTED")) {
				progress = "exchange commitments...";
			} else if (params[0].equals("COMMITMENTS_EXCHANGED")) {
				progress = "exchange shares...";
			} else if (params[0].equals("SHARES_EXCHANGED")) {
				progress = "verify shares...";
			} else if (params[0].equals("SHARES_VERIFIED_AND_COMPLAINTS_ISSUED_AND_SENT")) {
				progress = "check for complaints...";
			} else if (params[0].equals("RETRIEVED_COMPLAINTS_AND_SENT_RESPONSES")) {
				progress = "check for complaint responses...";
			}

			progressDialog.setMessage(progress);

			if (!progressDialog.isShowing()) {
				progressDialog.show();
			}
		}

		@Override
		protected void onPostExecute(Void param) {
			super.onPostExecute(param);
			if (mPedersenVSS.executionFinished()) {
				progressDialog.setMessage("protocol execution finished...");
				progressDialog.dismiss();

				buildQualList();

				BigInteger[] shares = computePrivateKeyShares();
				mPrivateKeyShare = shares[0];
				mPrivateShare = shares[1];

				new PublicKeyComputationTask().execute();
			}
		}
	}

	private class PublicKeyComputationTask extends AsyncTask<Void, String, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(context);
			progressDialog.setTitle("Running FeldmanVSS");
			progressDialog.setMessage("compute commitments...");
			progressDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			runFeldmanVSS();
			String state[] = new String[1];

			while (!mFeldmanVSS.executionFinished()) {
				try {
					Thread.sleep(500);
					state[0] = mFeldmanVSS.getCurrentState().toString();
				} catch (InterruptedException e) {
					Log.e(TAG, "error during thread execution");
				}
				publishProgress(state);
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... params) {
			super.onProgressUpdate(params);
			String progress = "";

			if (params[0].equals("COMMITMENTS_COMPUTED")) {
				progress = "exchange commitments...";
			} else if (params[0].equals("COMMITMENTS_EXCHANGED")) {
				progress = "verify commitments...";
			} else if (params[0].equals("COMMITMENTS_VERIFIED_AND_COMPLAINTS_ISSUED")) {
				progress = "check for complaints...";
			} else if (params[0].equals("COMPLAINTS_SENT")) {
				progress = "check for complaint responses...";
			} else if (params[0].equals("COMPLAINTS_VERIFIED")) {
				progress = "check for reconstruction...";
			} else if (params[0].equals("RECONSTRUCTION_REQUEST_SENT")) {
				progress = "reconstruct...";
			} else if (params[0].equals("MISSING_PUBLICKEY_SHARES_RECONSTRUCTED")) {
				progress = "compute public key...";
			}

			progressDialog.setMessage(progress);

			if (!progressDialog.isShowing()) {
				progressDialog.show();
			}
		}

		@Override
		protected void onPostExecute(Void param) {
			super.onPostExecute(param);
			if (mFeldmanVSS.executionFinished()) {
				progressDialog.setMessage("protocol execution finished...");
				progressDialog.dismiss();

				mPublicKey = mFeldmanVSS.getPublicKey();

				String[] data = new String[] {
						mPrivateKeyShare.toString(),
						mPrivateShare.toString(),
						mPublicKey.toString()
				};

				Bundle bundle = new Bundle();
				bundle.putStringArray(COMPUTED_VALUES, data);
				Intent intent = new Intent(context, SDKGAchievementActivity.class);
				intent.putExtras(bundle);
				startActivity(intent);
			}
		}
	}

}
