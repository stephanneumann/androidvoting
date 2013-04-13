
package com.murati.smartdkg.activities;

import com.murati.smartdkg.R;
import com.murati.smartdkg.communication.SDKGBuddyList;
import com.murati.smartdkg.communication.SDKGExtension;
import com.murati.smartdkg.communication.SDKGMessage;
import com.murati.smartdkg.communication.SDKGProvider;
import com.murati.smartdkg.util.SDKGKeyFactory;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class SDKGInitiationActivity extends Activity implements PacketListener {

	private XMPPConnection mConnection = SDKGConnectionActivity.CURRENT_CONNECTION;

	private ListView mBuddyListView;

	//////////////////////////////////////////////////

	private SDKGBuddyList mBuddyList;

	private SDKGMessage mMessage;

	//////////////////////////////////////////////////

	private ArrayList<String> mParticipants;

	private String mSessionId = UUID.randomUUID().toString();

	private RSAPrivateCrtKeyParameters mPrivateKey; // my RSA private key

	private boolean mTimerStarted = false;

	//////////////////////////////////////////////////

	public static final long TIME_TO_DKG_PROTOCOL_EXECUTION = 5000; // in ms

	public static final String PLAYER = "com.example.xmppapp.PARTICIPANTS";

	private static final String SENDER = SDKGConnectionActivity.CURRENT_CONNECTION.getUser();

	private static final String TAG = "SDKGInitiationActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sdkg_initiation);

		mBuddyListView = (ListView)findViewById(R.id.lv_contacts);
		mBuddyList = new SDKGBuddyList(mConnection);

		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, mBuddyList.getBuddyList());

		mBuddyListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mBuddyListView.setAdapter(arrayAdapter);

		mConnection.addPacketListener(this, null);
		ProviderManager.getInstance().addExtensionProvider(SDKGExtension.ELEMENT_NAME,
				SDKGExtension.NAMESPACE, new SDKGProvider(this));

		mParticipants = new ArrayList<String>();

		try {
			mPrivateKey = SDKGKeyFactory.loadRSAPrivateKey("privkey");
		} catch (IOException e) {
			Log.e(TAG, "could not load rsa private key from file");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mConnection.removePacketListener(this);
		ProviderManager.getInstance().removeExtensionProvider(SDKGExtension.ELEMENT_NAME,
				SDKGExtension.NAMESPACE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mConnection.addPacketListener(this, null);
		ProviderManager.getInstance().addExtensionProvider(SDKGExtension.ELEMENT_NAME,
				SDKGExtension.NAMESPACE, new SDKGProvider(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_initiation, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_select_all:
			selectBuddy(true);
			return true;
		case R.id.menu_deselect_all:
			selectBuddy(false);
			return true;
		case R.id.menu_invite:
			String[] selectedBuddies = getSelectedBuddies();
			sendInitationMessage(selectedBuddies);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void selectBuddy(boolean mode) {
		for (int i = 0; i < mBuddyListView.getCount(); i++) {
			mBuddyListView.setItemChecked(i, mode);
		}
	}

	private String[] getSelectedBuddies() {
		SparseBooleanArray checkedItems = mBuddyListView.getCheckedItemPositions();
		String[] buddies = mBuddyList.getBuddyList();
		String[] selectedBuddies = new String[checkedItems.size()];

		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i)) {
				selectedBuddies[i] = buddies[checkedItems.keyAt(i)];
			}
		}
		return selectedBuddies;
	}

	private void sendInitationMessage(String[] receiverlist) {
		String sessionId = UUID.randomUUID().toString();
		mSessionId = sessionId;

		for (String receiver : receiverlist) {
			if (receiver != null) {
				SDKGMessage initiation = new SDKGMessage(
						mPrivateKey,
						SDKGMessage.Type.INITIATION,
						sessionId,
						receiverlist,
						this);

				initiation.setFrom(SENDER);
				initiation.setTo(receiver);
				mConnection.sendPacket(initiation);
			}
		}

	}

	private void sendJoinMessage(String receiver, String sessionId) {
		SDKGMessage joinMessage = new SDKGMessage(
				mPrivateKey,
				SDKGMessage.Type.JOIN,
				mSessionId,
				null,
				this);

		joinMessage.setFrom(SENDER);
		joinMessage.setTo(receiver);
		mConnection.sendPacket(joinMessage);
	}

	private void sendDkgProtocolExecutionMessage(String receiver, String sessionId, String[] participants) {
		SDKGMessage readyMessage = new SDKGMessage(
				mPrivateKey,
				SDKGMessage.Type.DKG_PROTOCOL_EXECUTION,
				sessionId,
				participants,
				this);

		readyMessage.setFrom(SENDER);
		readyMessage.setTo(receiver);
		mConnection.sendPacket(readyMessage);
	}

	private void checkMessage(final SDKGMessage message) {
		mMessage = message; // use parcel to add this to showDialog instead

		if (message.isInitiationMessage()) {
			if (message.getSessionId().equals(mSessionId)) {
				return;
			}
			mHandler.sendEmptyMessage(0);
		}

		else if (message.isJoinMessage()) {
			mParticipants.add(message.getFrom());

			if (!mTimerStarted) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						startDkgProtocolExecution(message.getSessionId());
					}
				};
				// wait some time before starting protocol execution
				mHandler.postDelayed(runnable, TIME_TO_DKG_PROTOCOL_EXECUTION);
				mTimerStarted = true;
			}
		}

		else if (message.isDkgProtocolExecutionMessage()) {
			String[] invitationList = message.getValues();
			String[] participants = new String[invitationList.length + 1];

			for (int i = 0; i < invitationList.length; i++) {
				participants[i] = invitationList[i];
			}

			participants[participants.length - 1] = message.getFrom();

			Intent intent = new Intent(this, SDKGActivity.class);
			Bundle b = new Bundle();
			b.putStringArray(PLAYER, participants);
			intent.putExtras(b);
			startActivity(intent);
			finish();
			return;
		}
	}

	private void startDkgProtocolExecution(String sessionId) {
		String[] participans = null;

		synchronized (mParticipants) {
			participans = new String[mParticipants.size() + 1];
			mParticipants.toArray(participans);

			for (String participant : participans) {
				sendDkgProtocolExecutionMessage(participant, sessionId, participans);
			}
			participans[participans.length - 1] = SENDER;
		}

		Intent intent = new Intent(this, SDKGActivity.class);
		Bundle b = new Bundle();
		b.putStringArray(PLAYER, participans);
		intent.putExtras(b);
		startActivity(intent);
		finish();
		return;
	}

	@Override
	public void processPacket(Packet packet) {
		if (packet instanceof Presence) {
			return;
		}
		SDKGExtension extension = (SDKGExtension) packet.getExtension(SDKGExtension.NAMESPACE);
		extension.setFrom(packet.getFrom());
		SDKGMessage msg = new SDKGMessage(extension);
		checkMessage(msg);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// use parcable for dkg message here...
			showDialog();
		};
	};

	private DialogInterface.OnClickListener addClickListener() {
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which != DialogInterface.BUTTON_POSITIVE) {
					return;
				}
				sendJoinMessage(mMessage.getFrom(), mMessage.getSessionId());
			}
		};
		return listener;
	}

	private void showDialog() {
		DialogInterface.OnClickListener listener = addClickListener();
		StringBuilder sb = new StringBuilder();
		sb.append("Invitation from:\n");
		sb.append(mMessage.getFrom());
		sb.append("\n\nInvitation list:\n");
		sb.append(Arrays.toString(mMessage.getValues()));
		sb.append("\n\nJoin?");

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("Invitation");
		dialog.setMessage(sb.toString());
		dialog.setPositiveButton("Yes", listener);
		dialog.setNegativeButton("No", null);
		dialog.create();
		dialog.show();
	}

}
