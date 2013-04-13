
package com.murati.smartdkg.communication;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import java.util.Collection;

public class SDKGBuddyList {

	private XMPPConnection mConnection;

	public SDKGBuddyList(XMPPConnection connection) {
		mConnection = connection;
	}

	public boolean isAvailable(String contact) {
		Roster roster = mConnection.getRoster();
		Presence presence;
		int i = 0;

		// need to ask many times for user presence -> asmack doesnt work properly
		// for this purpose
		do {
			presence = roster.getPresence(contact);
			i++;
		} while (i < 100);

		return presence.isAvailable();
	}

	public String getNameFromEmail(String email) {
		String[] name = email.split("@");
		return name[0];
	}

	public String[] getBuddyList() {
		Roster roster = mConnection.getRoster();
		Collection<RosterEntry> rosterEntries;
		int i = 0;

		do {
			rosterEntries = roster.getEntries();
			i++;
		} while (i < 100);

		String[] contacts = new String[rosterEntries.size()];
		int j = 0;

		for (RosterEntry entry : rosterEntries) {
			contacts[j] = entry.getUser();
			j++;
		}

		return contacts;
	}
}
