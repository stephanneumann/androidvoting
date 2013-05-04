package com.androidvoting.oksana.activities;

import java.math.BigInteger;
import java.util.ArrayList;
import org.jivesoftware.smack.XMPPConnection;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.widget.TextView;

import com.androidvoting.R;
import com.androidvoting.murati.smartdkg.activities.SDKGActivity;
import com.androidvoting.murati.smartdkg.activities.SDKGConnectionActivity;
import com.androidvoting.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.androidvoting.murati.smartdkg.dkg.SDKGPlayer;
import com.androidvoting.murati.smartdkg.dkg.SDKGPlayerList;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.androidvoting.oksana.Decryption;
import com.androidvoting.oksana.Decryption.State;
import com.androidvoting.oksana.KeyAndCommitmentsInitilisation;
import com.androidvoting.oksana.EncryptedVote;

public class DecryptionActivity extends Activity {
	private XMPPConnection connection = SDKGConnectionActivity.CURRENT_CONNECTION;
	
	private String sessionId = SDKGActivity.mSessionId; // unique id for the
														// current instance of
														// the dkg protocol

	private Context context = this; // app context

	private Decryption decryption;

	private SDKGElGamalParameters parameters;

	private SDKGPlayerList players = SDKGActivity.mPlayerList;

	private SDKGPlayer myself = SDKGActivity.mMyself;

	public static ArrayList<SDKGZqElement> DECRYPTED_VOTES;

	private static final String TAG = "DecryptionActivity";

	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_decryption);

		initParameters();
		// // //initLoadKeys();
		// //
		BigInteger[] votes = { BigInteger.valueOf(123) };
		SparseArray<EncryptedVote> votesMap = new SparseArray<EncryptedVote>();
		int i = 0;
		for (BigInteger vote : votes) {
//			BigInteger r = new BigInteger(parameters.getQ().bitLength(),
//					new Random());
			BigInteger r = BigInteger.ONE;
			BigInteger a = parameters.getG().modPow(r, parameters.getP());
			BigInteger b = parameters.getH().modPow(r, parameters.getP())
					.multiply(vote).mod(parameters.getP());
		
			EncryptedVote v = new EncryptedVote(new SDKGZqElement(
					parameters.getP(), a), new SDKGZqElement(parameters.getP(),
					b));
			votesMap.put(i, v);
			i++;
		}

		this.decryption = new Decryption(sessionId,
				connection, context, myself, players, parameters, votesMap,
				KeyAndCommitmentsInitilisation.commitmentsMap);
				
		new DecryptionTask().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_decryption, menu);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	private void initParameters() {
		// parameters = new SDKGElGamalParameters(
		// new BigInteger(
		// "111930019642068417438074558300694340156587956555698629470914131330836670314842427722798502002216870944902305931917588433031384121138088787698419389634718106738115794820133084240137242264042332989162493496520794368826169308133135109412816743246243527430440142753378507026369668076190619700799456487551876054207"),
		// new BigInteger(
		// "55965009821034208719037279150347170078293978277849314735457065665418335157421213861399251001108435472451152965958794216515692060569044393849209694817359053369057897410066542120068621132021166494581246748260397184413084654066567554706408371623121763715220071376689253513184834038095309850399728243775938027103"),
		// new BigInteger(
		// "12318509937837929888838834232455081313733322516848738249994989592259774200977125571000223800320587912245829436105757095160635147284621372531144192989814017701233914661663531485996120741252505525438154553361499556894206592144928920270979230704532945448311642936631812488656116464564395748559909980854789757588"),
		// new BigInteger(
		// "61601198452968981934024243593568896584644050084321195545281847260353806930420468138535099784007003507618238635858101694751395783669607650928427616580394080127775693780082110707911448994586005969639583176124058109021976128110393786060838259744480859019692922346626419323542740924919921871260219412702982287239"),
		// (players.size() - 1) / 2);
		parameters = SDKGActivity.mParameters;
	}

	private class DecryptionTask extends AsyncTask<Void, String, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(context);
			progressDialog.setTitle("Running Decryption");
			progressDialog.setMessage("compute decryption shares...");
			progressDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			decryption.execute();
			String state[] = new String[1];

			while (!decryption.executionFinished()) {
				try {
					Thread.sleep(500);
					state[0] = decryption.getCurrentState().toString();
				} catch (InterruptedException e) {
					Log.e(TAG, "error during thread execution");
				}
				publishProgress(state);
			}
			return null;
			// progressDialog.setMessage("Hi, baby!");
			// return null;
		}

		@Override
		protected void onProgressUpdate(String... params) {
			super.onProgressUpdate(params);
			String progress = "starting protocoll...";

			if (params[0].equals("DECRYPTION_COMMITMENT_COMPUTED")) {
				progress = "exchange commitments...";
			} else if (params[0].equals("DECRYPTION_COMMITMENT_EXCHANGED")) {
				progress = "compute decryption shares...";
			} else if (params[0].equals("DECRYPTION_SHARES_COMPUTED")) {
				progress = "exchange decryption shares...";
			} else if (params[0].equals("DECRYPTION_SHARES_EXCHANGED")) {
				progress = "verifying shares and decrypting votes...";
			} else if (params[0].equals("ERROR")) {
				progress = decryption.getErrorMessage();
			}

			progressDialog.setMessage(progress);

			if (!progressDialog.isShowing()) {
				progressDialog.show();
			}
			// progressDialog.setMessage("Hi, baby!");
		}

		@Override
		protected void onPostExecute(Void param) {
			super.onPostExecute(param);
			if (decryption.executionFinished()) {
				progressDialog
						.setMessage("PlayerNr: " + myself.getIndex() + ", protocol execution finished... vote is "
								+ decryption.getDecryptedVotes().get(0)
										.toString());
				// progressDialog.dismiss();
				//
				// DECRYPTED_VOTES = decryption.getDecryptedVotes();
				//
				// Intent intent = new Intent(context, DecryptionResults.class);
				// startActivity(intent);
			}

			else if (decryption.getCurrentState() == State.ERROR) {
				progressDialog.setMessage("There were errors: "
						+ decryption.getErrorMessage());
			}
		}
	}
}
