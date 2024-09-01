package ee.ria.eudi.qeaa.rp.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.Nonce;
import ee.ria.eudi.qeaa.rp.configuration.properties.RpProperties;
import ee.ria.eudi.qeaa.rp.controller.PresentationDefinition.Constraints;
import ee.ria.eudi.qeaa.rp.controller.PresentationDefinition.Field;
import ee.ria.eudi.qeaa.rp.controller.PresentationDefinition.InputDescriptor;
import ee.ria.eudi.qeaa.rp.controller.VerifierMetadata.VpFormats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static ee.ria.eudi.qeaa.rp.controller.PresentationRequestObject.ClientIdScheme.X509_SAN_DNS;
import static ee.ria.eudi.qeaa.rp.controller.PresentationRequestObject.ResponseMode.DIRECT_POST;
import static ee.ria.eudi.qeaa.rp.controller.PresentationRequestObject.ResponseType.VP_TOKEN;

@Component
@RequiredArgsConstructor
public class PresentationRequestObjectFactory {
    public static final JOSEObjectType OAUTH_AUTHZ_REQ_JWT = new JOSEObjectType("oauth-authz-req+jwt");
    private final String rpClientId;
    private final RpProperties rpProperties;
    private final ECDSASigner rpKeySigner;
    private final ECKey rpKey;
    private final JWSAlgorithm rpKeyJwsAlg;

    public PresentationRequestObject create(CredentialDoctype doctype, List<CredentialAttribute> attributes) {
        return PresentationRequestObject.builder()
            .clientId(rpClientId)
            .clientIdScheme(X509_SAN_DNS)
            .responseType(VP_TOKEN)
            .responseMode(DIRECT_POST)
            .responseUri(rpProperties.rpBackend().responseEndpointUrl())
            .clientMetadata(getClientMetadata())
            .presentationDefinition(getPresentationDefinition(doctype, attributes))
            .nonce(new Nonce().getValue())
            .state(new State().getValue())
            .build();
    }

    public SignedJWT create(String requestObjectClaims) throws JOSEException, ParseException {
        JWSHeader jwsHeader = new JWSHeader.Builder(rpKeyJwsAlg)
            .type(OAUTH_AUTHZ_REQ_JWT)
            .x509CertChain(rpKey.getX509CertChain())
            .x509CertSHA256Thumbprint(rpKey.getX509CertSHA256Thumbprint())
            .build();
        SignedJWT requestObjectJwt = new SignedJWT(jwsHeader, JWTClaimsSet.parse(requestObjectClaims));
        requestObjectJwt.sign(rpKeySigner);
        return requestObjectJwt;
    }

    private VerifierMetadata getClientMetadata() {
        VerifierMetadata.MsoMdoc msoMdoc = VerifierMetadata.MsoMdoc.builder()
            .alg(List.of("ES256", "ES384", "ES512"))
            .build();
        VpFormats vpFormats = VpFormats.builder()
            .msoMdoc(msoMdoc)
            .build();
        return VerifierMetadata.builder()
            .clientName("Relying Party")
            .clientUri(rpProperties.rp().baseUrl() + "/info")
            .logoUri(rpProperties.rp().baseUrl() + "/rp_logo.png")
            .vpFormats(vpFormats)
            .build();
    }

    private PresentationDefinition getPresentationDefinition(CredentialDoctype doctype, List<CredentialAttribute> attributes) {
        Field documentTypeFilter = getDocumentTypeFilter(doctype);
        List<Field> fields = attributes.stream().map(attribute -> Field.builder()
                .path(List.of(attribute.getPresentationDefinitionPath()))
                .intentToRetain(true)
                .build())
            .collect(Collectors.toList());
        fields.add(documentTypeFilter);
        Constraints constraints = Constraints.builder()
            .limitDisclosure("required")
            .fields(fields)
            .build();
        InputDescriptor inputDescriptor = InputDescriptor.builder()
            .id(UUID.randomUUID().toString())
            .format(Map.of("mso_mdoc", Map.of("alg", List.of("ES256", "ES384", "ES512"))))
            .constraints(constraints)
            .build();
        return PresentationDefinition.builder()
            .id(UUID.randomUUID().toString())
            .name("Credential presentation request")
            .purpose("Claims validation")
            .inputDescriptors(List.of(inputDescriptor))
            .build();
    }

    private Field getDocumentTypeFilter(CredentialDoctype doctype) {
        return Field.builder()
            .path(List.of("$.type"))
            .filter(PresentationDefinition.Filter.builder()
                .type("string")
                .pattern(doctype.getUri())
                .build())
            .build();
    }
}
