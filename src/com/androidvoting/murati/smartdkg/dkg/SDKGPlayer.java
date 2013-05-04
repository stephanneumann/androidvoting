
package com.androidvoting.murati.smartdkg.dkg;

/*
 * represents a player in the dkg protocol
 */
public class SDKGPlayer {

	private int index; // index of a player in range 1...n

	private String name; // email address

	private int numComplaints = 0;

	private boolean isDisqualified = false;

	public SDKGPlayer(int index, String name) {
		this.index = index;
		this.name = name;
	}

	public SDKGPlayer(int index, String name, int numComplaints) {
		this(index, name);
		this.numComplaints = numComplaints;
	}

	public int getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}

	public int getNumComplaints() {
		return numComplaints;
	}

	public boolean isDisqualified() {
		return isDisqualified;
	}

	public void markDisqualified() {
		isDisqualified = true;
	}

	public void markComplained() {
		numComplaints++;
	}

	public boolean equals(SDKGPlayer p) {
		if (name.equals(p.getName()) && index == p.getIndex()) {
			return true;
		}
		return false;
	}

}
