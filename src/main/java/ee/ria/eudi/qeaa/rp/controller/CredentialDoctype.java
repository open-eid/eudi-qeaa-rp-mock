package ee.ria.eudi.qeaa.rp.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CredentialDoctype {
    ORG_ISO_18013_5_1_MDL("org.iso.18013.5.1.mDL"),
    EU_EUROPA_EC_EUDI_PID_1("eu.europa.ec.eudi.pid.1");

    private final String uri;
}
