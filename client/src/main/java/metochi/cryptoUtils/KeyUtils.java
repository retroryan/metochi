package metochi.cryptoUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

public class KeyUtils {

    public static KeyPair generateRSAKeyPair()
            throws Exception {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(1024, new SecureRandom());
        return kpGen.generateKeyPair();
    }
}
