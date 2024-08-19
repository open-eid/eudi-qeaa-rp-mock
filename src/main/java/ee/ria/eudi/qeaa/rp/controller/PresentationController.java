package ee.ria.eudi.qeaa.rp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
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
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ee.ria.eudi.qeaa.rp.controller.CredentialDoctype.ORG_ISO_18013_5_1_MDL;

@Controller
@RequiredArgsConstructor
public class PresentationController {
    public static final String REQUEST_CREDENTIAL_PRESENTATION_REQUEST_MAPPING = "request-presentation";
    private final RpBackendService rpBackendService;
    private final TransactionRepository transactionRepository;
    private final PresentationRequestObjectFactory presentationRequestObjectFactory;
    private final ObjectMapper objectMapper;

    @Value("${eudi.wallet.authorization-url}")
    private String walletAuthorizationUrl;

    @GetMapping("/")
    public ModelAndView preparePresentationView(@RequestParam(name = "doc_type", required = false) String docType) throws JsonProcessingException, JOSEException {
        CredentialDoctype credentialDoctype = CredentialDoctype.valueOf(StringUtils.defaultIfBlank(docType, ORG_ISO_18013_5_1_MDL.name()));
        List<CredentialAttribute> attributes = CredentialAttribute.getAttributes(credentialDoctype);
        ECKey responseEncryptionKey = new ECKeyGenerator(Curve.P_256)
            .keyUse(KeyUse.ENCRYPTION)
            .algorithm(JWEAlgorithm.ECDH_ES)
            .keyID(UUID.randomUUID().toString())
            .generate();
        PresentationRequestObject requestObject = presentationRequestObjectFactory.create(credentialDoctype, attributes, responseEncryptionKey);
        ModelAndView modelAndView = new ModelAndView("prepare-presentation");
        modelAndView.addObject("doc_types", CredentialDoctype.values());
        modelAndView.addObject("request_object", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestObject));
        modelAndView.addObject("response_encryption_key", responseEncryptionKey.toJSONObject());
        return modelAndView;
    }

    @PostMapping("/presentation")
    public ModelAndView presentationView(@ModelAttribute("request_object") String requestObject, @ModelAttribute("response_encryption_key") String responseEncryptionKey) throws JOSEException, ParseException, IOException, WriterException {
        SignedJWT presentationRequest = presentationRequestObjectFactory.create(requestObject);
        RequestObjectResponse response = rpBackendService.postRequestObject(presentationRequest);
        startTransaction(presentationRequest, response, ECKey.parse(responseEncryptionKey));

        String redirectUrl = UriComponentsBuilder
            .fromUriString(walletAuthorizationUrl)
            .queryParam("request_uri", response.requestUri())
            .queryParam("client_id", presentationRequest.getJWTClaimsSet().getStringClaim("client_id"))
            .toUriString();

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(redirectUrl, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        String qrCodeBase64 = Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());

        ModelAndView modelAndView = new ModelAndView("presentation");
        modelAndView.addObject("qrCodeImage", "data:image/png;base64," + qrCodeBase64);
        modelAndView.addObject("redirectUrl", redirectUrl);
        return modelAndView;
    }

    @PostMapping(value = REQUEST_CREDENTIAL_PRESENTATION_REQUEST_MAPPING)
    public RedirectView requestPresentation(@ModelAttribute("request_object") String requestObject, @ModelAttribute("response_encryption_key") String responseEncryptionKey) throws JOSEException, ParseException {
        SignedJWT presentationRequest = presentationRequestObjectFactory.create(requestObject);
        RequestObjectResponse response = rpBackendService.postRequestObject(presentationRequest);
        startTransaction(presentationRequest, response, ECKey.parse(responseEncryptionKey));
        return new RedirectView(UriComponentsBuilder
            .fromUriString(walletAuthorizationUrl)
            .queryParam("request_uri", response.requestUri())
            .queryParam("client_id", presentationRequest.getJWTClaimsSet().getStringClaim("client_id"))
            .toUriString());
    }

    private void startTransaction(SignedJWT signedRequestObject, RequestObjectResponse requestObjectResponse, ECKey responseEncryptionKey) throws ParseException {
        JWTClaimsSet roClaims = signedRequestObject.getJWTClaimsSet();
        Map<String, Object> presentationDefinition = roClaims.getJSONObjectClaim("presentation_definition");
        transactionRepository.save(Transaction.builder()
            .nonce(roClaims.getStringClaim("nonce"))
            .state(roClaims.getStringClaim("state"))
            .presentationDefinition(presentationDefinition)
            .transactionId(requestObjectResponse.transactionId())
            .responseCode(requestObjectResponse.responseCode())
            .responseEncryptionKey(responseEncryptionKey)
            .build());
    }
}
