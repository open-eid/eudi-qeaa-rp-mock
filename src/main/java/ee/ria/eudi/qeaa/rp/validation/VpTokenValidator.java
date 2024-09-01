package ee.ria.eudi.qeaa.rp.validation;

import ee.ria.eudi.qeaa.rp.controller.CredentialNamespace;
import ee.ria.eudi.qeaa.rp.error.ServiceException;
import ee.ria.eudi.qeaa.rp.service.PresentationSubmission;
import ee.ria.eudi.qeaa.rp.service.PresentationSubmission.InputDescriptor;
import ee.ria.eudi.qeaa.rp.util.MDocUtil;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.mdocauth.DeviceAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import static ee.ria.eudi.qeaa.rp.util.MDocUtil.KEY_ID_DEVICE;
import static ee.ria.eudi.qeaa.rp.util.MDocUtil.KEY_ID_ISSUER;

@Slf4j
@Component
@RequiredArgsConstructor
public class VpTokenValidator {
    public static final String CREDENTIAL_FORMAT_MSO_MDOC = "mso_mdoc";
    public static final String CREDENTIAL_PATH_AS_DIRECT_VP_TOKEN_VALUE = "$";

    private final String rpClientId;
    @Qualifier("issuerTrustedRootCAs")
    private final List<X509Certificate> issuerTrustedRootCAs;

    public Map<CredentialNamespace, Map<String, Object>> validate(String vpToken, PresentationSubmission presentationSubmission, String presentationDefinitionId, String nonce) {
        List<InputDescriptor> inputDescriptors = validatePresentationSubmission(presentationSubmission, presentationDefinitionId);
        if (inputDescriptors.size() > 1) {
            throw new NotImplementedException("Multiple input descriptors processing not implemented.");
        }
        InputDescriptor inputDescriptor = inputDescriptors.getFirst();
        if (CREDENTIAL_FORMAT_MSO_MDOC.equals(inputDescriptor.format())) {
            if (!CREDENTIAL_PATH_AS_DIRECT_VP_TOKEN_VALUE.equals(inputDescriptor.path())) {
                throw new ServiceException("Invalid credential path. Expecting CBOR encoded credential directly in the vp_token element.");
            }
            MDoc mDoc = validateMsoMDoc(vpToken, nonce);
            return MDocUtil.getIssuerSignedItems(mDoc);
        } else {
            throw new NotImplementedException("Input descriptor format '%s' processing not implemented.".formatted(inputDescriptor.format()));
        }
    }

    private MDoc validateMsoMDoc(String vpToken, String nonce) {
        MDoc mDoc = MDoc.Companion.fromCBORHex(vpToken);
        if (!mDoc.verifyDocType()) {
            throw new ServiceException("Invalid mDoc doctype");
        }
        if (!mDoc.verifyValidity()) {
            throw new ServiceException("Expired mDoc");
        }
        if (!mDoc.verifyIssuerSignedItems()) {
            throw new ServiceException("Invalid mDoc claims");
        }
        SimpleCOSECryptoProvider issuerCryptoProvider = MDocUtil.getIssuerCryptoProvider(mDoc, issuerTrustedRootCAs);
        if (!mDoc.verifyCertificate(issuerCryptoProvider, KEY_ID_ISSUER)) {
            throw new ServiceException("Invalid mDoc certificate chain");
        }
        if (!mDoc.verifySignature(issuerCryptoProvider, KEY_ID_ISSUER)) {
            throw new ServiceException("Invalid mDoc issuer signature");
        }
        DeviceAuthentication deviceAuthentication = MDocUtil.getDeviceAuthentication(rpClientId, nonce, mDoc.getDocType().getValue());
        log.info("Device authentication for client {} and nonce {} -> cbor hex: {}", rpClientId, nonce, deviceAuthentication.toDE().toCBORHex());
        SimpleCOSECryptoProvider deviceCryptoProvider = MDocUtil.getDeviceCryptoProvider(mDoc);
        if (!mDoc.verifyDeviceSignature(deviceAuthentication, deviceCryptoProvider, KEY_ID_DEVICE)) {
            throw new ServiceException("Invalid mDoc device signature");
        }
        return mDoc;
    }

    private List<InputDescriptor> validatePresentationSubmission(PresentationSubmission presentationSubmission, String presentationDefinitionId) {
        if (!presentationDefinitionId.equals(presentationSubmission.definitionId())) {
            throw new ServiceException("Invalid presentation submission definition id");
        }
        List<InputDescriptor> inputDescriptors = presentationSubmission.descriptorMap();
        if (inputDescriptors == null || inputDescriptors.isEmpty()) {
            throw new ServiceException("Invalid presentation submission. No input descriptors.");
        }
        return inputDescriptors;
    }
}
