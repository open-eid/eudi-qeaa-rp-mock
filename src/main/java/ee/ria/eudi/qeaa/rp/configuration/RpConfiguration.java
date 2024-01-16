package ee.ria.eudi.qeaa.rp.configuration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import ee.ria.eudi.qeaa.rp.util.JwtUtil;
import ee.ria.eudi.qeaa.rp.util.X509CertUtil;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

@Configuration
@ConfigurationPropertiesScan
public class RpConfiguration {

    @Bean
    public String rpClientId(X509Certificate rpCert) {
        return X509CertUtil.getSubjectAlternativeNameDNSName(rpCert);
    }

    @Bean
    public X509Certificate rpCert(SslBundles sslBundles) throws KeyStoreException {
        SslBundle bundle = sslBundles.getBundle("eudi-rp");
        KeyStore keyStore = bundle.getStores().getKeyStore();
        return (X509Certificate) keyStore.getCertificate(bundle.getKey().getAlias());
    }

    @Bean
    public ECKey rpKey(SslBundles sslBundles) throws KeyStoreException, JOSEException {
        SslBundle bundle = sslBundles.getBundle("eudi-rp");
        KeyStore keyStore = bundle.getStores().getKeyStore();
        return ECKey.load(keyStore, bundle.getKey().getAlias(), null);
    }

    @Bean
    public JWSAlgorithm rpKeyJwsAlg(ECKey rpKey) {
        return JwtUtil.getJwsAlgorithm(rpKey.getCurve());
    }

    @Bean
    public ECDSASigner rpKeySigner(ECKey rpKey) throws JOSEException {
        return new ECDSASigner(rpKey);
    }
}
