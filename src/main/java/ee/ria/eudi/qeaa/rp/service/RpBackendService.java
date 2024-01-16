package ee.ria.eudi.qeaa.rp.service;

import com.nimbusds.jwt.SignedJWT;
import ee.ria.eudi.qeaa.rp.configuration.properties.RpProperties;
import ee.ria.eudi.qeaa.rp.model.RequestObjectResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@RequiredArgsConstructor
public class RpBackendService {
    public static final MediaType CONTENT_TYPE_APPLICATION_JWS = new MediaType("application", "jws");
    private final RpProperties.RelyingPartyBackend relyingPartyBackend;
    private final RestClient.Builder restClientBuilder;
    private final RestClientSsl ssl;
    private RestClient restClient;

    @PostConstruct
    private void setupRestClient() {
        restClient = restClientBuilder.apply(ssl.fromBundle("eudi-rp")).build();
    }

    public RequestObjectResponse postRequestObject(SignedJWT requestObject) {
        return restClient.post()
            .uri(relyingPartyBackend.requestEndpointUrl())
            .body(requestObject.serialize())
            .contentType(CONTENT_TYPE_APPLICATION_JWS)
            .accept(APPLICATION_JSON)
            .retrieve()
            .body(RequestObjectResponse.class);
    }
}
