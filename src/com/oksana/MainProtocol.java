package com.oksana;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import org.jivesoftware.smack.packet.Packet;

import com.murati.smartdkg.communication.SDKGExtension;
import com.murati.smartdkg.communication.SDKGMessage;
import com.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.murati.smartdkg.dkg.SDKGFeldmanVSS;
import com.murati.smartdkg.dkg.SDKGPedersenVSS;
import com.murati.smartdkg.dkg.SDKGPlayer;
import com.murati.smartdkg.dkg.SDKGPlayerList;
import com.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.murati.smartdkg.dkg.commitments.SDKGCikCommitment;
import com.murati.smartdkg.dkg.commitments.SDKGShare;

public class MainProtocol {
	SDKGPedersenVSS pedersen;
	SDKGFeldmanVSS feldman;
	Decryption decryption;
	
	SDKGElGamalParameters parameters;
	
	SDKGPlayer myself;
	SDKGPlayerList players;
	
	HashMap<SDKGPlayer, SDKGZqElement> viCommitments;
	VotesDecryptionSharesMap decryptionSharesMap;
	
	EncryptedVoteList encryptedVotes; //votes from all players
	ArrayList<SDKGZqElement> decryptedVotes;
	
	BigInteger result;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	void init(){
		//TODO load parameters
	}
	
	void distributeKeys(){
		//TODO run Pedersen
		//TODO run Feldman
	}
	
	void castVote(){
		//TODO encrypt and save encrypted vote
		EncryptedVote myVote = null; //for now, encrypted vote to cast
		encryptedVotes.add(myVote);
	}
		
	
	
	void tallyVotes(){
		result = BigInteger.ZERO;
		for (SDKGZqElement vote : decryptedVotes){
			result.add(vote.getValue());
		}
	}
	
	private EncryptedVoteList getEncryptedVotes(){
		//TODO - also, add protection against players casting multiple votes
		//Add temporary check, then integrate with mixnet
		return null;
	}
	
	static PacketData getDataFromPacket(Packet packet, SDKGPlayerList playersList){
		SDKGExtension extension = (SDKGExtension) packet.getExtension(SDKGExtension.NAMESPACE);
		extension.setFrom(packet.getFrom());
		SDKGMessage msg = new SDKGMessage(extension);

		SDKGPlayer sender = playersList.findPlayer(msg.getFrom());
		SDKGPlayer receiver = playersList.findPlayer(msg.getTo());
		
		return new PacketData(receiver, sender, msg);
	}
	
	public void processPacket(Packet packet) {
		PacketData packetData = MainProtocol.getDataFromPacket(packet, players);

		if (packetData.getMsg().isComplaintMessage()) { //isViCommitment
			SDKGZqElement value = null; //TODO: change
			viCommitments.put(packetData.getFrom(), value);
		}
		
		else if(true) { //TODO clause
			decryption.processPacket(packet);
		}
	}
}
