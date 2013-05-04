package com.androidvoting.oksana;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;

/*Proofs that dlog(G) = dlog(H), with G = g^s mod q, H = h^s mod q, without disclosing s*/
public class ZKPDlog {
	SDKGZqElement g,h,G,H;
	BigInteger q; 
	
	ZKPDlog(SDKGZqElement g, SDKGZqElement h, SDKGZqElement G, SDKGZqElement H){
		this.G = G;
		this.H = H;
		this.g = g;
		this.h = h;
		this.q = g.getOrder();
	}
	
	ZKPDlogProof prove(BigInteger s) throws NoSuchAlgorithmException{		
		BigInteger r = new BigInteger(q.bitLength(), new Random()).mod(q);
		SDKGZqElement x1 = g.modPow(r);
		SDKGZqElement x2 = h.modPow(r);
		String c = g.toString() + h.toString() + G.toString() + x1.toString() + x2.toString();
		BigInteger e = BigInteger.ONE;
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		sha.update(c.getBytes());
		
		e = new BigInteger(sha.digest()).mod(q);
		BigInteger y = r.add(e.multiply(s)).mod(q.subtract(BigInteger.ONE));
		
		return new ZKPDlogProof(e, y);
	}
	
	boolean verify(ZKPDlogProof proof) throws NoSuchAlgorithmException{
		SDKGZqElement x1 = g.modPow(proof.getY()).mul(G.getMultiplicativeInverse().modPow(proof.getE()));
		SDKGZqElement x2 = h.modPow(proof.getY()).mul(H.getMultiplicativeInverse().modPow(proof.getE()));

		String c = g.toString() + h.toString() + G.toString() + x1.toString() + x2.toString();
		BigInteger e;
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		sha.update(c.getBytes());
		e = new BigInteger(sha.digest()).mod(q);
		
		return e.equals(proof.getE());
	}

}