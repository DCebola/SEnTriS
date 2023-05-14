package pt.fct.nova.id.srv.application.crypto.dgk;

/*

This is the Java implementation of the C++ NTL Library
Please refer to this site for NTL documentation:
http://www.shoup.net/ntl/doc/tour.html
http://www.shoup.net/ntl/doc/ZZ.txt

Credits to Andrew Quijano for code conversion
and Samet Tonyali for helping on revising the code/debugging it.

 */

import java.math.BigInteger;

public class NTL {
        public static BigInteger POSMOD(BigInteger x, BigInteger n) {
        return x.mod(n).add(n).mod(n);
    }
}