package com.androidvoting.murati.smartdkg.dkg;

import com.androidvoting.murati.smartdkg.exceptions.SDKGPlayerNotFoundException;

import java.util.ArrayList;

public class SDKGPlayerList extends ArrayList<SDKGPlayer> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SDKGPlayerList() {
		super();
	}

	public SDKGPlayerList(int capacity) {
		super(capacity);
	}

	public void addPlayer(SDKGPlayer player) {
		add(player);
	}

	public void addPlayer(int index, String playerName) {
		add(new SDKGPlayer(index, playerName));
	}

	public void addPlayer(int index, String playerName, int numComplaints) {
		add(new SDKGPlayer(index, playerName, numComplaints));
	}

	public void removePlayer(SDKGPlayer player) {
		remove(player);
	}

	public boolean containsPlayer(SDKGPlayer player) {
		return contains(player);
	}

	public SDKGPlayer findPlayer(String playerName) {
		if (playerName.isEmpty()) {
			throw new SDKGPlayerNotFoundException("playername cannot be empty");
		}
		for (SDKGPlayer p : this) {
			if (p == null) {
				continue;
			}
			if (p.getName().equals(playerName)) {
				return p;
			}
		}
		throw new SDKGPlayerNotFoundException("player " + playerName + "not found");
	}

	public SDKGPlayer findPlayer(int index) {
		if (index < 0 || index >= this.getSize()) {
			throw new SDKGPlayerNotFoundException("player index must be in range 0...n");
		}
		return get(index);
	}

	public SDKGPlayer findPlayer(SDKGPlayer player) {
		if (player == null) {
			throw new SDKGPlayerNotFoundException("player cannot be null");
		}
		for (SDKGPlayer p : this) {
			if (p == null) {
				continue;
			}
			if (p.getIndex() == player.getIndex()
					&& p.getName().equals(player.getName())
					&& p.getNumComplaints() == player.getNumComplaints()) {
				return p;
			}
		}
		throw new SDKGPlayerNotFoundException("the requested player is not in the playerlist");
	}

	/*
	 * the object on the specified position will be set to null. actually no player is removed from list due to retain the player indices
	 */
	public boolean removeAll(SDKGPlayerList secondList) {
		if (!this.containsAll(secondList)) {
			return false;
		}
		for (int j = 0; j < secondList.size(); j++) {
			if (secondList.get(j) != null) {
				this.set(j, null);
			}
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return isEmpty();
	}

	public int getSize() {
		return size();
	}

	@Override
	public void clear() {
		clear();
	}

	public SDKGPlayerList filterComplainedPlayers(int maxAllowed) {
		SDKGPlayerList filteredList = new SDKGPlayerList();

		for (SDKGPlayer p : this) {
			if (p != null && p.getNumComplaints() > maxAllowed) {
				filteredList.add(p);
			}
		}
		return filteredList;
	}

	public void removeOftenComplainedPlayers(int maxAllowed) {
		SDKGPlayerList blacklist = filterComplainedPlayers(maxAllowed);

		for (SDKGPlayer p : blacklist) {
			this.set(p.getIndex(), null);
		}
	}

	/*
	 * "removes" a disqualified player from playerList
	 */
	public void removeDisqualifiedPlayers() {
		for (int j = 1; j < this.size(); j++) {
			if (this.get(j) != null && this.get(j).isDisqualified()) {
				/*
				 *  sets the object to null on the specified position
				 *  this is helpful to retain the indices of the players
				 */
				this.set(j, null);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getSize());

		for (SDKGPlayer p : this) {
			if (p == null) {
				continue;
			}
			sb.append("[").append("P_").append(p.getIndex()).append(", ").append(p.getName())
			.append(", ").append(p.getNumComplaints()).append("], ");
		}
		return sb.toString();
	}

}
