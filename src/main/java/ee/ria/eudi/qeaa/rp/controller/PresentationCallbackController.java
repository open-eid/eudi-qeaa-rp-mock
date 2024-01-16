package ee.ria.eudi.qeaa.rp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.ria.eudi.qeaa.rp.error.ServiceException;
import ee.ria.eudi.qeaa.rp.model.CredentialNamespace;
import ee.ria.eudi.qeaa.rp.model.ResponseObjectResponse;
import ee.ria.eudi.qeaa.rp.model.Transaction;
import ee.ria.eudi.qeaa.rp.repository.TransactionRepository;
import ee.ria.eudi.qeaa.rp.service.RpBackendService;
import ee.ria.eudi.qeaa.rp.validation.VpTokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static ee.ria.eudi.qeaa.rp.model.CredentialAttribute.ORG_ISO_18013_5_1_PORTRAIT;
import static ee.ria.eudi.qeaa.rp.model.CredentialNamespace.ORG_ISO_18013_5_1;

@Controller
@RequiredArgsConstructor
public class PresentationCallbackController {
    public static final String PRESENTATION_CALLBACK_REQUEST_MAPPING = "/presentation-callback";
    private final RpBackendService rpBackendService;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final VpTokenValidator vpTokenValidator;

    @GetMapping(PRESENTATION_CALLBACK_REQUEST_MAPPING)
    public ModelAndView presentationRequestCallback(@RequestParam(name = "response_code") String responseCode) throws IOException {
        Transaction transaction = getAndInvalidateTransaction(responseCode);
        ResponseObjectResponse responseObject = rpBackendService.getResponseObject(transaction.getTransactionId(), responseCode);
        Map<CredentialNamespace, Map<String, Object>> claims = vpTokenValidator.validate(responseObject.vpToken(), responseObject.presentationSubmission(), transaction.getPresentationId(), transaction.getNonce());

        ModelAndView modelAndView = new ModelAndView("credential");
        modelAndView.addObject("vp_token", responseObject.vpToken());
        String presentationSubmission = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseObject.presentationSubmission());
        modelAndView.addObject("presentation_submission", presentationSubmission);

        Map<String, Object> mdlClaims = claims.getOrDefault(ORG_ISO_18013_5_1, Collections.emptyMap());
        if (mdlClaims.containsKey("portrait")) {
            String encodedPortrait = Base64.getEncoder().encodeToString((byte[]) mdlClaims.get(ORG_ISO_18013_5_1_PORTRAIT.getUri()));
            mdlClaims.replace("portrait", encodedPortrait);
        }
        modelAndView.addObject("claims", claims);
        return modelAndView;
    }

    private Transaction getAndInvalidateTransaction(String responseCode) {
        Transaction transaction = transactionRepository.findByResponseCode(responseCode).orElseThrow(() -> new ServiceException("Invalid response code"));
        transactionRepository.delete(transaction);
        return transaction;
    }
}
