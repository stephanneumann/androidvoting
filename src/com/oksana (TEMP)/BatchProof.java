package main;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.bouncycastle.util.BigIntegers;

/**
 * Proof and verification of dlog(v) = dlog(zj) for each partial decryption zj
 * with g^d = v mod p, and alpha_j ^ d = zj mod p for all j = 1,...,m
 * 
 * @author oksana
 * 
 */
public class BatchProof {
	private static final BigInteger ONE = BigInteger.ONE;

	private BigInteger p, g, q, vi;
	int m, l;
	private final BigInteger[] alphas, zij;

	/**
	 * It is assumed, that the class variables q, g, l satisfy proper ElGamal
	 * parameters: p is a large prime q = (p - 1)/2 is also prime g is a
	 * generator of Zq* (a subgroup of Zp) and a quadratic rest modulo p l is a
	 * secure parameter of batched proof, with 2^l < q
	 */

	public BatchProof(BigInteger _p, BigInteger _g, BigInteger _vi,
			BigInteger[] _zij, BigInteger[] _alphas) {
		q = _p.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2));
		p = _p;
		g = _g;
		m = _zij.length;
		alphas = _alphas;
		vi = _vi;
		zij = _zij;
	}

	public BigInteger[] prove(BigInteger d, BigInteger[] tj)
			throws NoSuchAlgorithmException {
		SecureRandom rnd = new SecureRandom();
		BigInteger r = BigIntegers.createRandomInRange(ONE, q.subtract(ONE),
				rnd);
		BigInteger c1 = g.modPow(r, p);
		BigInteger c2 = ONE;
		for (int j = 0; j < m; j++) {
			c2 = c2.multiply(alphas[j].modPow(tj[j], p));
		}
		c2 = c2.modPow(r, p);
		int j = rnd.nextInt(m);
		String c = c1.toString() + c2.toString() + g.toString() + vi.toString()
				+ alphas[j].toString() + zij[j].toString();
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		sha.update(c.getBytes());
		BigInteger u = new BigInteger(sha.digest()).mod(q);
		BigInteger w = r.subtract(u.multiply(d).mod(q));
		return new BigInteger[] { c1, c2, w, BigInteger.valueOf(j) };
	}

	public boolean verify(BigInteger[] proof, BigInteger _vi, BigInteger[] _zi,
			BigInteger[] tj) throws NoSuchAlgorithmException {
		int j = proof[3].intValue();
		
		String c = proof[0].toString() + proof[1].toString() + g.toString()
				+ _vi.toString() + alphas[j].toString()
				+ _zi[j].toString();
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		sha.update(c.getBytes());
		BigInteger u = new BigInteger(sha.digest()).mod(q);
		BigInteger c1 = g.modPow(proof[2], p).multiply(_vi.modPow(u, p)).mod(p);
		
		BigInteger prod1 = ONE;
		BigInteger prod2 = ONE;
		for (int j1 = 0; j1 < m; j1++) {
			prod1 = prod1.multiply(alphas[j1].modPow(tj[j1], p));
			prod2 = prod2.multiply(_zi[j1].modPow(tj[j1], p));
		}
		BigInteger c2 = prod1.modPow(proof[2], p).multiply(prod2.modPow(u, p)).mod(p);

		return c1.equals(proof[0]) && c2.equals(proof[1]);
	}
}
