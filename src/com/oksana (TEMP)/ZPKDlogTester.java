package main;

import java.math.BigInteger;
import java.util.Random;

public class ZPKDlogTester {
	BigInteger q,g,h,G,H,s;
	int n = 10;
	Random rnd = new Random();
	
	ZPKDlog zkp1, zkp2;
	
	void generate(){
		q = new BigInteger(n, 100, rnd);
		g = new BigInteger(n, 100, rnd).mod(q);
		h = new BigInteger(n, 100, rnd).mod(q);
		s = new BigInteger(n, rnd).mod(q.subtract(BigInteger.ONE));
		G = g.modPow(s, q);
		H = h.modPow(s, q);
		
		zkp1 = new ZPKDlog(g,h,G,H,q);
		
	}
	
	void test(){
		BigInteger[] proof = zkp1.prove(s);
		zkp2 = new ZPKDlog(g,h,G,H,q);
		boolean v = zkp2.verify(proof);
		System.out.println(v);
	}
	
	void runTests(){
		this.generate();
		this.test();		//should output "true"
		H = new BigInteger(n, rnd).mod(q);
		this.test();		//should output "false"
	}
	
}
