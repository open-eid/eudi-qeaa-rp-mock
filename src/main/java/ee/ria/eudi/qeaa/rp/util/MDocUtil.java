package ee.ria.eudi.qeaa.rp.util;

import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import ee.ria.eudi.qeaa.rp.controller.CredentialNamespace;
import ee.ria.eudi.qeaa.rp.error.ServiceException;
import id.walt.mdoc.COSECryptoProviderKeyInfo;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.cose.COSESign1;
import id.walt.mdoc.dataelement.ByteStringElement;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.dataelement.EncodedCBORElement;
import id.walt.mdoc.dataelement.ListElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.NullElement;
import id.walt.mdoc.dataelement.StringElement;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import id.walt.mdoc.mdocauth.DeviceAuthentication;
import id.walt.mdoc.mso.DeviceKeyInfo;
import id.walt.mdoc.mso.MSO;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ee.ria.eudi.qeaa.rp.controller.CredentialAttribute.ORG_ISO_18013_5_1_DRIVING_PRIVILEGES;
import static ee.ria.eudi.qeaa.rp.controller.CredentialAttribute.ORG_ISO_18013_5_1_PORTRAIT;
import static ee.ria.eudi.qeaa.rp.controller.CredentialAttribute.ORG_ISO_18013_5_1_SIGNATURE_USUAL_MARK;
import static ee.ria.eudi.qeaa.rp.error.ErrorCode.INVALID_REQUEST;

@Slf4j
@UtilityClass
public class MDocUtil {
    public static final String KEY_ID_ISSUER = "issuer-key-id";
    public static final String KEY_ID_DEVICE = "device-key-id";

    public SimpleCOSECryptoProvider getIssuerCryptoProvider(MDoc mDoc, List<X509Certificate> issuerTrustedRootCAs) {
        List<X509Certificate> x5Chain = MDocUtil.getX5Chain(mDoc);
        X509Certificate issuerCert = x5Chain.getFirst();
        PublicKey publicKey = issuerCert.getPublicKey();
        COSECryptoProviderKeyInfo issuerKeyInfo = new COSECryptoProviderKeyInfo(KEY_ID_ISSUER,
            getAlgorithmId(publicKey), publicKey, null, x5Chain, issuerTrustedRootCAs);
        return new SimpleCOSECryptoProvider(List.of(issuerKeyInfo));
    }

    public SimpleCOSECryptoProvider getDeviceCryptoProvider(MDoc mDoc) {
        PublicKey devicePublicKey = MDocUtil.getDevicePublicKey(mDoc);
        COSECryptoProviderKeyInfo deviceCryptoProviderKeyInfo = new COSECryptoProviderKeyInfo(KEY_ID_DEVICE,
            getAlgorithmId(devicePublicKey), devicePublicKey, null, List.of(), List.of());
        return new SimpleCOSECryptoProvider(List.of(deviceCryptoProviderKeyInfo));
    }

    public Map<CredentialNamespace, Map<String, Object>> getIssuerSignedItems(MDoc mDoc) {
        return mDoc.getNameSpaces().stream()
            .collect(Collectors.toMap(
                CredentialNamespace::fromUri,
                namespace -> mDoc.getIssuerSignedItems(namespace).stream()
                    .filter(item -> item.getElementValue().getInternalValue() != null)
                    .collect(Collectors.toMap(
                        item -> item.getElementIdentifier().getValue(),
                        item -> getValue(item.getElementIdentifier().getValue(), item.getElementValue())
                    ))
            ));
    }

    private static Object getValue(String elementIdentifier, DataElement internalValue) {
        if (ORG_ISO_18013_5_1_PORTRAIT.getUri().equals(elementIdentifier) || ORG_ISO_18013_5_1_SIGNATURE_USUAL_MARK.getUri().equals(elementIdentifier)) {
            return Base64.getEncoder().encodeToString((byte[]) internalValue.getInternalValue());
        } else if (ORG_ISO_18013_5_1_DRIVING_PRIVILEGES.getUri().equals(elementIdentifier)) {
            return CBORObject.DecodeFromBytes(internalValue.toCBOR()).ToJSONString();
        } else {
            return internalValue.getInternalValue();
        }
    }

    public DeviceAuthentication getDeviceAuthentication(String clientId, String doctype, String responseUri, String nonce, String mdocNonce) {
        ListElement sessionTranscript = new ListElement(
            List.of(
                new NullElement(),
                new NullElement(),
                getOID4VPHandover(clientId, responseUri, nonce, mdocNonce)));
        EncodedCBORElement deviceNameSpaces = new EncodedCBORElement(new MapElement(Map.of()));
        return new DeviceAuthentication(sessionTranscript, doctype, deviceNameSpaces);
    }

    @SneakyThrows
    private ListElement getOID4VPHandover(String clientId, String responseUri, String nonce, String mdocNonce) {
        log.info("OID4VPHandover - client_id: {}, response_uri: {}, nonce: {}, mdoc_nonce: {}", clientId, responseUri, nonce, mdocNonce);
        byte[] clientIdToHash = new ListElement(List.of(new StringElement(clientId), new StringElement(mdocNonce))).toCBOR();
        byte[] responseUriToHash = new ListElement(List.of(new StringElement(responseUri), new StringElement(mdocNonce))).toCBOR();
        byte[] clientIdHash = MessageDigest.getInstance("SHA-256").digest(clientIdToHash);
        byte[] responseUriHash = MessageDigest.getInstance("SHA-256").digest(responseUriToHash);
        return new ListElement(List.of(new ByteStringElement(clientIdHash), new ByteStringElement(responseUriHash), new StringElement(nonce)));
    }

    public AlgorithmID getAlgorithmId(PublicKey publicKey) {
        if (publicKey instanceof ECPublicKey ecPublicKey) {
            int bitLength = ecPublicKey.getParams().getOrder().bitLength();
            return switch (bitLength) {
                case 256 -> AlgorithmID.ECDSA_256;
                case 384 -> AlgorithmID.ECDSA_384;
                case 521 -> AlgorithmID.ECDSA_512;
                default -> throw new IllegalArgumentException("Unsupported key size: " + bitLength);
            };
        } else {
            throw new IllegalArgumentException("Invalid key type. An Elliptic Curve key is required by ISO/IEC 18013-5:2021.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<X509Certificate> getX5Chain(MDoc mDoc) {
        try {
            IssuerSigned issuerSigned = mDoc.getIssuerSigned();
            COSESign1 issuerAuth = Objects.requireNonNull(issuerSigned.getIssuerAuth());
            byte[] x5Chain = Objects.requireNonNull(issuerAuth.getX5Chain());
            ByteArrayInputStream x5CainInputStream = new ByteArrayInputStream(x5Chain);
            return (List<X509Certificate>) CertificateFactory.getInstance("X509").generateCertificates(x5CainInputStream);
        } catch (CertificateException e) {
            throw new ServiceException(INVALID_REQUEST, "Invalid mDL issuer certificate chain", e);
        }
    }

    private PublicKey getDevicePublicKey(MDoc mDoc) {
        try {
            MSO mso = Objects.requireNonNull(mDoc.getMSO());
            DeviceKeyInfo deviceKeyInfo = mso.getDeviceKeyInfo();
            MapElement deviceKey = deviceKeyInfo.getDeviceKey();
            return new OneKey(CBORObject.DecodeFromBytes(deviceKey.toCBOR())).AsPublicKey();
        } catch (CoseException e) {
            throw new ServiceException(INVALID_REQUEST, "Invalid mDL device key", e);
        }
    }
}

