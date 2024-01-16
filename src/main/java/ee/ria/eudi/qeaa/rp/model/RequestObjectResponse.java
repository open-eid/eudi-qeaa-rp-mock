package ee.ria.eudi.qeaa.rp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.net.URI;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RequestObjectResponse(
    URI requestUri,
    @JsonProperty("exp")
    Long expiryTime,
    String transactionId,
    String responseCode) {
}
