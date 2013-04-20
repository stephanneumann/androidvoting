package com.oksana;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import com.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.murati.smartdkg.dkg.SDKGFeldmanVSS;
import com.murati.smartdkg.dkg.SDKGPedersenVSS;
import com.murati.smartdkg.dkg.SDKGPlayer;
import com.murati.smartdkg.dkg.SDKGPlayerList;
import com.murati.smartdkg.dkg.arithm.SDKGZqElement;

public class MainProtocol {
	SDKGPedersenVSS pedersen;
	SDKGFeldmanVSS feldman;
	Decryption decryption;
	
	SDKGElGamalParameters parameters;
	
	SDKGPlayer myself;
	SDKGPlayerList players;
	
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
	}
		
	private SDKGZqElement decryptVote(EncryptedVote vote) throws NoSuchAlgorithmException{
		decryption = new Decryption(myself, players, parameters, pedersen);
		DecryptionShareList allShares = this.getOtherDecryptionShares(vote);
		ArrayList<SDKGZqElement> validShares = decryption.getValidPD(allShares);
		return decryption.reconstruct(validShares);
	}
	
	private DecryptionShareList getOtherDecryptionShares(EncryptedVote vote) throws NoSuchAlgorithmException{
		DecryptionShareList decryptionShares = new DecryptionShareList(vote, new ArrayList<DecryptionShare>());
		decryptionShares.addDecryptionShare(decryption.getDecryptionShare(vote));
		for (SDKGPlayer player : players){
			decryptionShares.addDecryptionShare(this.getDecryptionShare(vote, player));
		}
		return decryptionShares;
	}
	
	private DecryptionShare getDecryptionShare(EncryptedVote vote, SDKGPlayer player){
		return new DecryptionShare();
	}
	
	private void getDecryptedVotes() throws NoSuchAlgorithmException{
		decryptedVotes = new ArrayList<>();
		EncryptedVoteList encVotes = this.getEncryptedVotes();
		for(EncryptedVote encVote : encVotes){
			decryptedVotes.add(this.decryptVote(encVote));
		}
	}
	
	void tallyVotes(){
		result = BigInteger.ZERO;
		for (SDKGZqElement vote : decryptedVotes){
			result.add(vote.getValue());
		}
	}
	
	private EncryptedVoteList getEncryptedVotes(){
		//TODO
		return null;
	}
}
