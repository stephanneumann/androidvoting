package com.oksana.activities;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.jivesoftware.smack.XMPPConnection;

import android.app.Activity;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.murati.smartdkg.R;
import com.murati.smartdkg.activities.SDKGAchievementActivity;
import com.murati.smartdkg.activities.SDKGConnectionActivity;
import com.murati.smartdkg.activities.SDKGInitiationActivity;
import com.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.murati.smartdkg.dkg.SDKGPlayer;
import com.murati.smartdkg.dkg.SDKGPlayerList;
import com.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.oksana.Decryption;
import com.oksana.DecryptionCommitmentsMap;
import com.oksana.DecryptionShare;
import com.oksana.EncryptedVote;
import com.oksana.EncryptedVoteList;

public class DecryptionActivity extends Activity {
	private XMPPConnection mConnection = SDKGConnectionActivity.CURRENT_CONNECTION;

	private Decryption decryption;

	private SDKGElGamalParameters parameters;

	private SDKGPlayerList players;

	private SDKGPlayer myself;

	private ArrayList<SDKGZqElement> decryptedVotes;

	private EncryptedVoteList encryptedVotes;

	private BigInteger myKeyShare;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sdkg_execution);

		Bundle bundle = this.getIntent().getExtras();
		String[] buddies = bundle.getStringArray(SDKGInitiationActivity.PLAYER);

		players = new SDKGPlayerList(buddies.length + 1);
		players.add(null); // there is no player using index 0!

		for (int j = 0; j < buddies.length; j++) {
			players.add(new SDKGPlayer(j + 1, buddies[j]));

			if (buddies[j].equals(mConnection.getUser())) {
				myself = new SDKGPlayer(j + 1, buddies[j]);
			}
		}

		// List<String> buddyList = new
		// LinkedList<String>(Arrays.asList(buddies));
		// buddyList.remove(mConnection.getUser());
		//
		// ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
		// android.R.layout.simple_list_item_1, buddyList);
		// mPlayerListView.setAdapter(arrayAdapter);

		initParameters();

		BigInteger[] votes = { BigInteger.ONE, BigInteger.ZERO };
		SparseArray<EncryptedVote> votesMap = new SparseArray<EncryptedVote>();
		int i = 0;
		for (BigInteger vote : votes) {
			SDKGZqElement e = new SDKGZqElement(parameters.getQ(), vote);
			BigInteger r = new BigInteger(parameters.getQ().bitLength(),
					new Random());
			BigInteger a = parameters.getG().modPow(r, parameters.getP());
			BigInteger b = parameters.getH().modPow(r, parameters.getP())
					.multiply(vote);
			EncryptedVote v = new EncryptedVote(new SDKGZqElement(
					parameters.getP(), a), new SDKGZqElement(parameters.getP(),
					b));
			this.encryptedVotes.add(v);
			votesMap.put(i, v);
			i++;
		}
		SparseArray<SDKGZqElement> commitmentsMap = new SparseArray<SDKGZqElement>();

		this.decryption = new Decryption(myself, players, parameters,
				commitmentsMap, votesMap, myKeyShare);
		
	}

	private void initParameters() {
		parameters = new SDKGElGamalParameters(
				new BigInteger(
						"111930019642068417438074558300694340156587956555698629470914131330836670314842427722798502002216870944902305931917588433031384121138088787698419389634718106738115794820133084240137242264042332989162493496520794368826169308133135109412816743246243527430440142753378507026369668076190619700799456487551876054207"),
				new BigInteger(
						"55965009821034208719037279150347170078293978277849314735457065665418335157421213861399251001108435472451152965958794216515692060569044393849209694817359053369057897410066542120068621132021166494581246748260397184413084654066567554706408371623121763715220071376689253513184834038095309850399728243775938027103"),
				new BigInteger(
						"12318509937837929888838834232455081313733322516848738249994989592259774200977125571000223800320587912245829436105757095160635147284621372531144192989814017701233914661663531485996120741252505525438154553361499556894206592144928920270979230704532945448311642936631812488656116464564395748559909980854789757588"),
				new BigInteger(
						"61601198452968981934024243593568896584644050084321195545281847260353806930420468138535099784007003507618238635858101694751395783669607650928427616580394080127775693780082110707911448994586005969639583176124058109021976128110393786060838259744480859019692922346626419323542740924919921871260219412702982287239"),
				(players.size() - 1) / 2);
	}
}
