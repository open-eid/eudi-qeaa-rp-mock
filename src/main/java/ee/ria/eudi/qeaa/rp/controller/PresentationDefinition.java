package ee.ria.eudi.qeaa.rp.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PresentationDefinition(
    String id,
    String name,
    String purpose,
    List<InputDescriptor> inputDescriptors) {

    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record InputDescriptor(
        String id,
        Map<String, Object> format,
        Constraints constraints) {
    }

    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Constraints(
        String limitDisclosure,
        List<Field> fields) {
    }

    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Field(
        List<String> path,
        Filter filter,
        Boolean intentToRetain) {
    }

    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Filter(
        String type,
        String pattern) {
    }
}
