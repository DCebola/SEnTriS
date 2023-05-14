package pt.fct.nova.id.srv.application.crypto.dgk;

import java.math.BigInteger;

// This interface collects constants used by a lot of
// 1- KeyPairGenerators
public interface CipherConstants {
    // controls the error probability of the primality testing algorithm
     int CERTAINTY = 40;
    // This variable has been needed a lot, but I want to keep it a Java 8 library
    // So it can be used in Android apps with NO issues
    BigInteger TWO = new BigInteger("2");

}
