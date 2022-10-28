package pt.fct.nova.id.srv.application.crypto.dgk.misc;

/*

This is the Java implementation of the C++ NTL Library
Please refer to this site for NTL documentation:
http://www.shoup.net/ntl/doc/tour.html
http://www.shoup.net/ntl/doc/ZZ.txt

Credits to Andrew Quijano for code conversion 
and Samet Tonyali for helping on revising the code/debugging it.

Feel free to use this code as you like.
 */

import java.math.BigInteger;
import java.security.SecureRandom;

public class NTL implements pt.fct.nova.id.srv.application.crypto.dgk.misc.CipherConstants
{
	private static final SecureRandom rnd = new SecureRandom();

	public static BigInteger POSMOD(BigInteger x, BigInteger n)
	{
		return x.mod(n).add(n).mod(n);
	}

	// Ensure it is n-bit Large number and positive as well
	public static BigInteger generateXBitRandom (int bits)
	{
		BigInteger r = new BigInteger(bits, rnd);
		r = r.setBit(bits - 1);
		return r;
	}

}