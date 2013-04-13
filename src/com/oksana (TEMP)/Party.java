package main;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;

import org.bouncycastle.util.BigIntegers;

//Party in election protocoll

public class Party {
	SDKGPlayer player; //player data here
	SDKGPedersenVSS shares;
	
	SDKGShare[] myShares;

	private BigInteger di;
	private BigInteger g, q;
	private BigInteger vi, zi;
	private BigInteger secret_a, secret_b;
	private int id;
	private ZPKDlog zpk;
	private BigInteger[] zpkProof;
	private HashMap<Integer, BigInteger> validDecryptions = new HashMap<>();
	private int t;
	
	Party(int i, BigInteger g, BigInteger q){
		this.setParameters(g, q);
		this.id = i;
	}
	
	Party(BigInteger g, BigInteger q){
		this.setParameters(g, q);
	}
	
	void setParameters(BigInteger g, BigInteger q){
		this.g = g;
		this.q = q;
	}
	
	void setPrivateKey(BigInteger di){
		this.di = di;
	}
	
	//Creates random secret key.
	void setPrivateKey(){
		SecureRandom rnd = new SecureRandom();
		this.di = BigIntegers.createRandomInRange(BigInteger.ONE, this.p.subtract(BigInteger.ONE), rnd);
	}
	
	void setSecret(BigInteger a, BigInteger b){
		this.secret_a = a;
		this.secret_b = b;
	}
	
	//Gets a commitment vi = g^di mod p for future decryption verification
	BigInteger getPK() {
		if (vi == null)
			this.PK();
		return this.vi;
	}

	private void PK() {
		this.vi = g.modPow(di, q);
	}

	// Provides proof for correct decryption (discrete logs equality)
	private void prove() {
		zpk = new ZPKDlog(g, secret_a, this.getPK(),
				this.getPartialDecryption(), q);
		zpkProof = zpk.prove(di);
	}

	public BigInteger[] getProof() {
		if (zpkProof == null)
			this.prove();
		return this.zpkProof;
	}

	BigInteger getPartialDecryption() {
		if (zi == null)
			this.decrypt();
		return this.zi;
	}
	
	HashMap<Integer, BigInteger> getValidParties(){
		return this.validDecryptions;
	}

	private void decrypt() {
		this.zi = secret_a.modPow(di, q);
	}

	// Verifies proof provided by party j
	public boolean verifyProof(BigInteger vj, BigInteger yj,
			BigInteger[] proof_j) {
		ZPKDlog zpk_j = new ZPKDlog(g, secret_a, vj, yj, q);
		return zpk_j.verify(proof_j);
	}

	//Gets a set of trusted parties with verified proofs
	void getValidPD(Party[] parties) {
		for (Party p : parties) {
			if (this.verifyProof(p.getPK(), p.getPartialDecryption(),
					p.getProof()))
				this.validDecryptions.put(p.id, p.getPartialDecryption());
		}
		this.validDecryptions.put(this.id, this.zi);
	}

	//Reconstructs a message from a set of partial decryptions of trusted parties
	//Not needed for now, adjust later for Shamir's reconstruction in general
	BigInteger reconstructShamirs() {
		if (this.validDecryptions.size() >= t) {
			BigInteger mi;
			BigInteger m = BigInteger.ONE;
			BigInteger mi_n, mi_d;
			mi_n = BigInteger.ONE;
			mi_d = BigInteger.ONE;
			for (int i : this.validDecryptions.keySet()) {
				for (int j : this.validDecryptions.keySet()) {
					if (j != i) {
						mi_n = mi_n.multiply(BigInteger.valueOf((long) j)).mod(
								q.subtract(BigInteger.ONE));
						mi_d = mi_d.multiply(BigInteger.valueOf((long) j - i));
					}
					mi = mi_n.multiply(mi_d.modInverse(q
							.subtract(BigInteger.ONE)));
					m = m.multiply(validDecryptions.get(i).modPow(mi, q))
							.mod(q);
				}
			}
			BigInteger s = secret_b.multiply(m.modInverse(q));
			return s;
		}
		return null;
	}
	
	public BigInteger reconstruct(){
		BigInteger y = BigInteger.ONE;
		for(BigInteger zi: this.validDecryptions.values()){
			y = y.multiply(zi).mod(q);
		}
		//TODO: reconstruction of missing key shares (need to know how they are stored by other users
		return y;
	}

}
