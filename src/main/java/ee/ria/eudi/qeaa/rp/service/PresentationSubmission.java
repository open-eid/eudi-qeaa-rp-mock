package ee.ria.eudi.qeaa.rp.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PresentationSubmission(
    String id,
    String definitionId,
    List<InputDescriptor> descriptorMap) {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InputDescriptor(
        String id,
        String format,
        String path,
        PathNested pathNested) {
        public static final String CREDENTIAL_FORMAT_MSO_MDOC = "mso_mdoc";
        public static final String CREDENTIAL_PATH_AS_DIRECT_VP_TOKEN_VALUE = "$";

    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PathNested(
        String path,
        String format) {

    }
}
