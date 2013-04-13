package main;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/*Proofs that dlog(G) = dlog(H), with G = g^s mod q, H = h^s mod q, without disclosing s*/
public class ZPKDlog {
	BigInteger g,h,G,H,q;
	
	ZPKDlog(BigInteger g, BigInteger h, BigInteger G, BigInteger H, BigInteger q){
		this.G = G;
		this.H = H;
		this.q = q;
		this.g = g;
		this.h = h;
	}
	
	BigInteger[] prove(BigInteger s){		
		BigInteger r = new BigInteger(q.bitLength(), new Random()).mod(q);
		BigInteger x1 = g.modPow(r,q);
		BigInteger x2 = h.modPow(r,q);
		String c = g.toString() + h.toString() + G.toString() + x1.toString() + x2.toString();
		BigInteger e = BigInteger.ONE;
		try{
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		sha.update(c.getBytes());
		
		e = new BigInteger(sha.digest()).mod(q);
		}
		catch(NoSuchAlgorithmException ex){
			e = BigInteger.valueOf((long)c.hashCode()).mod(q); //just in case, should never happen as long as standard crypto libraries are used 
		}
		BigInteger y = r.add(e.multiply(s)).mod(q.subtract(BigInteger.ONE));
		
		BigInteger[] proof = {e,y};
		return proof;
	}
	
	boolean verify(BigInteger[] proof){
		BigInteger x1 = g.modPow(proof[1], q).multiply(G.modInverse(q).modPow(proof[0], q)).mod(q);
		BigInteger x2 = h.modPow(proof[1], q).multiply(H.modInverse(q).modPow(proof[0], q)).mod(q);

		String c = g.toString() + h.toString() + G.toString() + x1.toString() + x2.toString();
		BigInteger e;
		try{
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		sha.update(c.getBytes());
		e = new BigInteger(sha.digest()).mod(q);
		}
		catch(NoSuchAlgorithmException ex){
			e = BigInteger.valueOf((long)c.hashCode()).mod(q);  //just in case, should never happen
		}
		
		return e.equals(proof[0]);
	}

}