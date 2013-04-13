package main;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.util.BigIntegers;

public class TestDrive {
	BigInteger g,p,x,h;
	public static void main(String[] args) throws NoSuchAlgorithmException{
		testBatchProof();
	}
	
	static void testReconstruction(){
		TestDrive test = new TestDrive();
		test.getKeys(100);
		String message = "abcd";
		Party party = new Party(test.g,test.p);
		BigInteger[] c = test.encrypt(message);
		party.setSecret(c[0], c[1]);
		party.setPrivateKey(test.x);
		party.getPartialDecryption();
		party.getValidPD(new Party[0]);
		BigInteger y = party.reconstruct();
		BigInteger m = test.decryptFromPD(y,c[1]);
		System.out.println(message.equals(new String(m.toByteArray())));
	}
	
	static void testVerification(){
		TestDrive test = new TestDrive();
		test.getKeys(10);
		int n = 10;
		BigInteger[] c = test.encrypt("abcd");
		Party[] parties = new Party[n];
		for(int i = 0; i<n;i++){
			parties[i] = new Party(i+1,test.g,test.p);
			parties[i].setPrivateKey();
			parties[i].setSecret(c[0], c[1]);
		}
		Party p = parties[0];
		p.getValidPD(parties);
		System.out.println(p.getValidParties().size() == n);
	}
	
	static void testBatchProof() throws NoSuchAlgorithmException {
		BatchProofTester bpt = new BatchProofTester();
		bpt.generate();
		bpt.test();
	}
	
	static void testDlog(){
		TestDrive test = new TestDrive();
		test.getKeys(10);
		Party p = new Party(1,test.g,test.p);
		BigInteger[] c = test.encrypt("abcd");
		p.setSecret(c[0], c[1]);
		p.setPrivateKey(test.x);
		
		BigInteger[] proof = p.getProof();
		System.out.print(p.verifyProof(p.getPK(), p.getPartialDecryption(), proof));
	}
	
	void getKeys(int bitLength){

	    SecureRandom random = new SecureRandom();

	    ElGamalParametersGenerator parGen = new ElGamalParametersGenerator();
	    parGen.init(bitLength, 80, random);
	    ElGamalParameters param = parGen.generateParameters();
		this.g = param.getG();
		this.p = param.getP();
		this.x = BigIntegers.createRandomInRange(BigInteger.ONE, p.subtract(BigInteger.ONE), random);
		this.h = g.modPow(x, p);
	}
	
	BigInteger[] encrypt(BigInteger m){
		BigInteger r = BigIntegers.createRandomInRange(BigInteger.ZERO, p.subtract(BigInteger.ONE), new SecureRandom());
		BigInteger a = g.modPow(r, p);
		
		BigInteger b = m.multiply(h.modPow(r, p)).mod(p);
		return new BigInteger[]{a,b};
	}
	
	BigInteger decrypt(BigInteger[] c){
		return c[1].multiply(c[0].modInverse(this.p).modPow(this.x, p)).mod(p);
	}
	
	BigInteger decryptFromPD(BigInteger y, BigInteger b){
		return b.multiply(y.modInverse(p)).mod(p);
	}
	
	BigInteger[] encrypt(String message){
		return this.encrypt(new BigInteger(message.getBytes()));
	}
}
