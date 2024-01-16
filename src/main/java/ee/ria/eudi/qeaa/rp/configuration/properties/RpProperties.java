package ee.ria.eudi.qeaa.rp.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudi")
public record RpProperties(
    @NotNull
    RelyingParty rp,
    @NotNull
    RelyingPartyBackend rpBackend) {

    @ConfigurationProperties(prefix = "eudi.rp")
    public record RelyingParty(
        @NotBlank
        @Pattern(regexp = ".*(?<!/)$")
        String baseUrl) {
    }

    @ConfigurationProperties(prefix = "eudi.rp-backend")
    public record RelyingPartyBackend(
        @NotBlank
        @Pattern(regexp = ".*(?<!/)$")
        String baseUrl,
        @NotBlank
        @Pattern(regexp = ".*(?<!/)$")
        String requestEndpointUrl,
        @NotBlank
        @Pattern(regexp = ".*(?<!/)$")
        String responseEndpointUrl) {
    }
}
