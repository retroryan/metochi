package metochi.cryptoUtils;

import metochi.cryptoUtils.KeyUtils;
import metochi.cryptoUtils.X509V1CreateExample;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertUtils {

    public static X509Certificate createCert() throws Exception {
        // create the keys
        KeyPair pair = KeyUtils.generateRSAKeyPair();

        // create the input stream
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        bOut.write(X509V1CreateExample.generateV1Certificate(pair).getEncoded());

        bOut.close();

        InputStream in = new ByteArrayInputStream(bOut.toByteArray());

        // create the certificate factory
        CertificateFactory fact = CertificateFactory.getInstance("X.509", "BC");

        // read the certificate
        X509Certificate x509Cert = (X509Certificate) fact.generateCertificate(in);
        return x509Cert;
    }
}
