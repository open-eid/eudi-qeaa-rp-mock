package ee.ria.eudi.qeaa.rp.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum CredentialAttribute {
    EU_EUROPA_EC_EUDI_PID_1_FAMILY_NAME(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_1, "family_name"),
    EU_EUROPA_EC_EUDI_PID_1_GIVEN_NAME(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_1, "given_name"),
    EU_EUROPA_EC_EUDI_PID_1_BIRTH_DATE(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_1, "birth_date"),
    EU_EUROPA_EC_EUDI_PID_1_ISSUANCE_DATE(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_1, "issuance_date"),
    EU_EUROPA_EC_EUDI_PID_1_EXPIRY_DATE(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_1, "expiry_date"),
    EU_EUROPA_EC_EUDI_PID_1_ISSUING_AUTHORITY(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_1, "issuing_authority"),
    EU_EUROPA_EC_EUDI_PID_1_ISSUING_COUNTRY(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_1, "issuing_country"),
    EU_EUROPA_EC_EUDI_PID_1_DOCUMENT_NUMBER(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_1, "document_number"),
    EU_EUROPA_EC_EUDI_PID_EE_1_PERSONAL_IDENTIFICATION_NUMBER(CredentialDoctype.EU_EUROPA_EC_EUDI_PID_1, CredentialNamespace.EU_EUROPA_EC_EUDI_PID_EE_1, "personal_identification_number"),

    ORG_ISO_18013_5_1_FAMILY_NAME(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "family_name"),
    ORG_ISO_18013_5_1_GIVEN_NAME(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "given_name"),
    ORG_ISO_18013_5_1_BIRTH_DATE(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "birth_date"),
    ORG_ISO_18013_5_1_ISSUE_DATE(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "issue_date"),
    ORG_ISO_18013_5_1_EXPIRY_DATE(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "expiry_date"),
    ORG_ISO_18013_5_1_ISSUING_COUNTRY(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "issuing_country"),
    ORG_ISO_18013_5_1_ISSUING_AUTHORITY(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "issuing_authority"),
    ORG_ISO_18013_5_1_DOCUMENT_NUMBER(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "document_number"),
    ORG_ISO_18013_5_1_PORTRAIT(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "portrait"),
    ORG_ISO_18013_5_1_DRIVING_PRIVILEGES(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "driving_privileges"),
    ORG_ISO_18013_5_1_UN_DISTINGUISHING_SIGN(CredentialDoctype.ORG_ISO_18013_5_1_MDL, CredentialNamespace.ORG_ISO_18013_5_1, "un_distinguishing_sign");

    private final CredentialDoctype doctype;
    private final CredentialNamespace namespace;
    private final String uri;

    public static List<CredentialAttribute> getAttributes(CredentialDoctype doctype) {
        return Arrays.stream(CredentialAttribute.values())
            .filter(e -> e.getDoctype().equals(doctype))
            .toList();
    }

    public String getPresentationDefinitionPath() {
        return "$['" + namespace.getUri() + "']['" + uri + "']";
    }
}
