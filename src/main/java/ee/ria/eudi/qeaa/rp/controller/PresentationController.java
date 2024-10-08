package ee.ria.eudi.qeaa.rp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import ee.ria.eudi.qeaa.rp.model.Transaction;
import ee.ria.eudi.qeaa.rp.repository.TransactionRepository;
import ee.ria.eudi.qeaa.rp.service.RequestObjectResponse;
import ee.ria.eudi.qeaa.rp.service.RpBackendService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static ee.ria.eudi.qeaa.rp.controller.CredentialDoctype.ORG_ISO_18013_5_1_MDL;

@Controller
@RequiredArgsConstructor
public class PresentationController {
    public static final String REQUEST_CREDENTIAL_PRESENTATION_REQUEST_MAPPING = "request-credential-presentation";
    private final String rpClientId;
    private final RpBackendService rpBackendService;
    private final TransactionRepository transactionRepository;
    private final PresentationRequestObjectFactory presentationRequestObjectFactory;
    private final ObjectMapper objectMapper;

    @Value("${eudi.wallet.authorization-url}")
    private String walletAuthorizationUrl;

    @GetMapping("/")
    public ModelAndView presentationView(@RequestParam(name = "doc_type", required = false) String docType) throws JsonProcessingException {
        CredentialDoctype credentialDoctype = CredentialDoctype.valueOf(StringUtils.defaultIfBlank(docType, ORG_ISO_18013_5_1_MDL.name()));
        List<CredentialAttribute> attributes = CredentialAttribute.getAttributes(credentialDoctype);
        PresentationRequestObject requestObject = presentationRequestObjectFactory.create(credentialDoctype, attributes);
        String requestObjectForTesting = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestObject);
        ModelAndView modelAndView = new ModelAndView("presentation");
        modelAndView.addObject("doc_types", CredentialDoctype.values());
        modelAndView.addObject("request_object", requestObjectForTesting);
        return modelAndView;
    }

    @PostMapping(value = REQUEST_CREDENTIAL_PRESENTATION_REQUEST_MAPPING)
    public RedirectView requestCredentialPresentation(@ModelAttribute("request_object") String requestObject) throws JOSEException, ParseException {
        SignedJWT presentationRequest = presentationRequestObjectFactory.create(requestObject);
        RequestObjectResponse response = rpBackendService.postRequestObject(presentationRequest);
        startTransaction(presentationRequest, response);
        return new RedirectView(UriComponentsBuilder
            .fromUriString(walletAuthorizationUrl)
            .queryParam("request_uri", response.requestUri())
            .queryParam("client_id", rpClientId)
            .toUriString());
    }

    private void startTransaction(SignedJWT signedRequestObject, RequestObjectResponse requestObjectResponse) throws ParseException {
        JWTClaimsSet roClaims = signedRequestObject.getJWTClaimsSet();
        Map<String, Object> presentationDefinition = roClaims.getJSONObjectClaim("presentation_definition");
        transactionRepository.save(Transaction.builder()
            .nonce(roClaims.getStringClaim("nonce"))
            .state(roClaims.getStringClaim("state"))
            .presentationId(presentationDefinition != null ? (String) presentationDefinition.get("id") : null)
            .transactionId(requestObjectResponse.transactionId())
            .responseCode(requestObjectResponse.responseCode())
            .build());
    }
}
