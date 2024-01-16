package ee.ria.eudi.qeaa.rp.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ResponseObjectResponse(
    String vpToken,
    @JsonDeserialize(using = PresentationSubmissionDeserializer.class)
    PresentationSubmission presentationSubmission,
    String state) {
}
