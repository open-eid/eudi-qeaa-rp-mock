package ee.ria.eudi.qeaa.rp.validation;

import com.upokecenter.cbor.CBORObject;
import ee.ria.eudi.qeaa.rp.configuration.properties.RpProperties;
import ee.ria.eudi.qeaa.rp.configuration.properties.RpProperties.RelyingPartyBackend;
import ee.ria.eudi.qeaa.rp.controller.CredentialNamespace;
import ee.ria.eudi.qeaa.rp.error.ServiceException;
import ee.ria.eudi.qeaa.rp.util.MDocUtil;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.mdocauth.DeviceAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static ee.ria.eudi.qeaa.rp.util.MDocUtil.KEY_ID_DEVICE;
import static ee.ria.eudi.qeaa.rp.util.MDocUtil.KEY_ID_ISSUER;

@Slf4j
@Component
@RequiredArgsConstructor
public class VpTokenValidator {
    private final RelyingPartyBackend rpBackendProperties;
    private final RpProperties.RelyingParty rpProperties;
    @Qualifier("issuerTrustedRootCAs")
    private final List<X509Certificate> issuerTrustedRootCAs;

    public Map<CredentialNamespace, Map<String, Object>> validateMsoMDoc(String vpToken, String nonce, String mDocNonce) {
        MDoc mDoc = getMDoc(vpToken);
        if (!mDoc.verifyDocType()) {
            throw new ServiceException("Invalid mDoc doctype");
        }
        if (!mDoc.verifyValidity()) {
            throw new ServiceException("Expired mDoc");
        }
        if (!mDoc.verifyIssuerSignedItems()) {
            throw new ServiceException("Invalid mDoc issuer signed items");
        }
        SimpleCOSECryptoProvider issuerCryptoProvider = MDocUtil.getIssuerCryptoProvider(mDoc, issuerTrustedRootCAs);
        if (!mDoc.verifyCertificate(issuerCryptoProvider, KEY_ID_ISSUER)) {
            throw new ServiceException("Invalid mDoc certificate chain");
        }
        if (!mDoc.verifySignature(issuerCryptoProvider, KEY_ID_ISSUER)) {
            throw new ServiceException("Invalid mDoc issuer signature");
        }
        DeviceAuthentication deviceAuthentication = MDocUtil.getDeviceAuthentication(rpProperties.clientId(), mDoc.getDocType().getValue(), rpBackendProperties.responseEndpointUrl(), nonce, mDocNonce);
        log.info("Device authentication for client {} and nonce {} -> cbor hex: {}", rpProperties.clientId(), nonce, deviceAuthentication.toDE().toCBORHex());
        SimpleCOSECryptoProvider deviceCryptoProvider = MDocUtil.getDeviceCryptoProvider(mDoc);
        if (!mDoc.verifyDeviceSignature(deviceAuthentication, deviceCryptoProvider, KEY_ID_DEVICE)) {
            throw new ServiceException("Invalid mDoc device signature");
        }
        return MDocUtil.getIssuerSignedItems(mDoc);
    }

    private static MDoc getMDoc(String vpToken) {
        try {
            return getMDocFromDeviceResponse(vpToken);
        } catch (Exception e) {
            return MDoc.Companion.fromCBORHex(vpToken); // TODO: Remove. Needed to support older version of OpenID4VP.
        }
    }

    private static MDoc getMDocFromDeviceResponse(String vpToken) {
        byte[] vpTokenCbor = Base64.getUrlDecoder().decode(vpToken);
        CBORObject cborObject = CBORObject.DecodeFromBytes(vpTokenCbor);
        CBORObject documentErrors = cborObject.get("documentErrors");
        if (documentErrors != null && documentErrors.size() != 0) {
            throw new ServiceException("Invalid device response");
        }
        CBORObject documents = cborObject.get("documents");
        if (documents.size() != 1) {
            log.warn("Multiple mdoc documents processing from device response is not implemented. Using first document.");
        }
        byte[] mdocBytes = documents.get(0).EncodeToBytes();
        return MDoc.Companion.fromCBOR(mdocBytes);
    }
}
