package ee.ria.eudi.qeaa.rp.configuration;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Configuration
public class CredentialIssuerConfiguration {

    @Bean
    public List<X509Certificate> issuerTrustedRootCAs(SslBundles sslBundles) throws KeyStoreException {
        SslBundle bundle = sslBundles.getBundle("eudi-issuer-ca");
        KeyStore trustStore = bundle.getStores().getTrustStore();
        List<X509Certificate> issuerTrustedRootCAs = new ArrayList<>();
        Enumeration<String> enumeration = trustStore.aliases();
        while (enumeration.hasMoreElements()) {
            String alias = enumeration.nextElement();
            if (trustStore.getCertificate(alias) instanceof X509Certificate x509Certificate) {
                issuerTrustedRootCAs.add(x509Certificate);
            }
        }
        return issuerTrustedRootCAs;
    }
}
