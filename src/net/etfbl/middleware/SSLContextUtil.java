
package net.etfbl.middleware;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import net.etfbl.config.Configuration;

public class SSLContextUtil {

    public static SSLContext getSSLContext() throws Exception {
        String keystorePath = String.format("%s/%s", Configuration.PROJECT_ROOT, Configuration.projectProperties.getProperty("keystorePath"));
        String keystorePassword = Configuration.projectProperties.getProperty("keystorePass");
        String truststorePath = String.format("%s/%s", Configuration.PROJECT_ROOT, Configuration.projectProperties.getProperty("truststorePath"));
        String truststorePassword = Configuration.projectProperties.getProperty("keystorePass");
        return createSSLContext(keystorePath, keystorePassword, truststorePath, truststorePassword);
    }

    private static SSLContext createSSLContext(String keystorePath, String keystorePassword,
                                               String truststorePath, String truststorePassword) throws Exception {
        // Load KeyStore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyStoreInputStream = new FileInputStream(keystorePath)) {
            keyStore.load(keyStoreInputStream, keystorePassword.toCharArray());
        }

        // Initialize KeyManagerFactory
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keystorePassword.toCharArray());

        // Load TrustStore
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream trustStoreInputStream = new FileInputStream(truststorePath)) {
            trustStore.load(trustStoreInputStream, truststorePassword.toCharArray());
        }

        // Initialize TrustManagerFactory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // Initialize SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }
}
