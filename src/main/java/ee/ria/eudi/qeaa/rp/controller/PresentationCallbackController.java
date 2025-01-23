package ee.ria.eudi.qeaa.rp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import ee.ria.eudi.qeaa.rp.error.ServiceException;
import ee.ria.eudi.qeaa.rp.model.Transaction;
import ee.ria.eudi.qeaa.rp.repository.TransactionRepository;
import ee.ria.eudi.qeaa.rp.service.PresentationSubmission;
import ee.ria.eudi.qeaa.rp.service.ResponseObjectResponse;
import ee.ria.eudi.qeaa.rp.service.RpBackendService;
import ee.ria.eudi.qeaa.rp.validation.VpTokenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.text.ParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ee.ria.eudi.qeaa.rp.controller.CredentialAttribute.ORG_ISO_18013_5_1_PORTRAIT;
import static ee.ria.eudi.qeaa.rp.controller.CredentialNamespace.ORG_ISO_18013_5_1;
import static ee.ria.eudi.qeaa.rp.service.PresentationSubmission.InputDescriptor.CREDENTIAL_FORMAT_MSO_MDOC;
import static ee.ria.eudi.qeaa.rp.service.PresentationSubmission.InputDescriptor.CREDENTIAL_PATH_AS_DIRECT_VP_TOKEN_VALUE;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PresentationCallbackController {
    public static final String PRESENTATION_CALLBACK_REQUEST_MAPPING = "/presentation-callback";

    private final RpBackendService rpBackendService;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final VpTokenValidator vpTokenValidator;

    @GetMapping(PRESENTATION_CALLBACK_REQUEST_MAPPING)
    public ModelAndView presentationRequestCallback(@RequestParam(name = "response_code") String responseCode) throws ParseException, JOSEException {
        log.debug("Received presentation callback with response code: {}", responseCode);
        Transaction transaction = getAndInvalidateTransaction(responseCode);
        log.debug("Transaction found for response code: {}", transaction.getTransactionId());
        ResponseObjectResponse responseObject = rpBackendService.getResponseObject(transaction.getTransactionId(), responseCode);
        log.debug("Response object received with state: {}", responseObject.state());
        if (!transaction.getState().equals(responseObject.state())) {
            throw new ServiceException("Invalid state");
        }

        EncryptedJWT jwe = decryptResponseObject(responseObject, transaction);
        JWEHeader header = jwe.getHeader();
        JWTClaimsSet claimsSet = jwe.getJWTClaimsSet();
        Map<String, Object> presentationSubmission = claimsSet.getJSONObjectClaim("presentation_submission");
        validatePresentationSubmission(transaction.getPresentationDefinition(), presentationSubmission);
        String vpToken = claimsSet.getStringClaim("vp_token");
        String mdocNonce = header.getAgreementPartyUInfo().decodeToString();
        Map<CredentialNamespace, Map<String, Object>> vpTokenClaims = vpTokenValidator.validateMsoMDoc(vpToken, transaction.getNonce(), mdocNonce);

        ModelAndView modelAndView = new ModelAndView("credential");
        modelAndView.addObject("vp_token", vpToken);
        modelAndView.addObject("presentation_submission", presentationSubmission);
        Map<String, Object> mdlClaims = vpTokenClaims.getOrDefault(ORG_ISO_18013_5_1, Collections.emptyMap());
        if (mdlClaims.containsKey("portrait")) {
            String encodedPortrait = Base64.getEncoder().encodeToString((byte[]) mdlClaims.get(ORG_ISO_18013_5_1_PORTRAIT.getUri()));
            mdlClaims.replace("portrait", encodedPortrait);
        }
        modelAndView.addObject("claims", vpTokenClaims);
        log.debug("Returning credential view");
        return modelAndView;
    }

    private void validatePresentationSubmission(Map<String, Object> presentationDefinition, Map<String, Object> presentationSubmission) {
        PresentationDefinition pd = objectMapper.convertValue(presentationDefinition, PresentationDefinition.class);
        PresentationSubmission ps = objectMapper.convertValue(presentationSubmission, PresentationSubmission.class);
        if (!pd.id().equals(ps.definitionId())) {
            throw new ServiceException("Invalid presentation submission definition id");
        }
        List<PresentationSubmission.InputDescriptor> psInputDescriptors = ps.descriptorMap();
        if (psInputDescriptors == null || pd.inputDescriptors().size() != psInputDescriptors.size()) {
            throw new ServiceException("Invalid presentation submission. Invalid input descriptors.");
        }

        PresentationSubmission.InputDescriptor inputDescriptor = psInputDescriptors.getFirst();
        if (CREDENTIAL_FORMAT_MSO_MDOC.equals(inputDescriptor.format())) {
            if (!CREDENTIAL_PATH_AS_DIRECT_VP_TOKEN_VALUE.equals(inputDescriptor.path())) {
                throw new ServiceException("Invalid credential path. Expecting credential directly in the vp_token element.");
            }
        } else {
            throw new NotImplementedException("Credential format '%s' processing not implemented.".formatted(inputDescriptor.format()));
        }
    }

    private EncryptedJWT decryptResponseObject(ResponseObjectResponse responseObject, Transaction transaction) throws ParseException, JOSEException {
        EncryptedJWT jwe = EncryptedJWT.parse(responseObject.response());
        ECDHDecrypter ecdhDecrypter = new ECDHDecrypter(transaction.getResponseEncryptionKey());
        jwe.decrypt(ecdhDecrypter);
        return jwe;
    }

    private Transaction getAndInvalidateTransaction(String responseCode) {
        Transaction transaction = transactionRepository.findByResponseCode(responseCode).orElseThrow(() -> new ServiceException("Invalid response code"));
        transactionRepository.delete(transaction);
        return transaction;
    }
}
