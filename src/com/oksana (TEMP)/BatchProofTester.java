package main;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.util.BigIntegers;

public class BatchProofTester {
	BigInteger p, g, v, d;
	BatchProof bp;
	BigInteger[] alphas;
	BigInteger[] z;
	BigInteger[] tj;

	void generate() {
		int n = 10;
		int m = 5;
		alphas = new BigInteger[m];
		z = new BigInteger[m];
		tj = new BigInteger[m];
		SecureRandom random = new SecureRandom();

		ElGamalParametersGenerator parGen = new ElGamalParametersGenerator();
		parGen.init(n, 80, random);
		ElGamalParameters param = parGen.generateParameters();
		this.g = param.getG();
		this.p = param.getP();
		this.d = BigIntegers.createRandomInRange(BigInteger.ONE,
				p.subtract(BigInteger.ONE), random);
		this.v = g.modPow(d, p);

		for (int j = 0; j < m; j++) {
			alphas[j] = g.modPow(
					BigIntegers.createRandomInRange(BigInteger.ONE,
							p.subtract(BigInteger.ONE), random), p);
			z[j] = alphas[j].modPow(d, p);
			tj[j] = BigIntegers.createRandomInRange(BigInteger.ONE,
					p.subtract(BigInteger.ONE), random);
		}

		bp = new BatchProof(p, g, v, z, alphas);
	}

	void test() throws NoSuchAlgorithmException {
		generate();
		BigInteger[] proof = bp.prove(d, tj);
		System.out.println(bp.verify(proof, v, z, tj));
	}
}
