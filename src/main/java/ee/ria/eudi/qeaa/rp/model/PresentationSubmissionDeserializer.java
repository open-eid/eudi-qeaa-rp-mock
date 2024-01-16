package ee.ria.eudi.qeaa.rp.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class PresentationSubmissionDeserializer extends JsonDeserializer<PresentationSubmission> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public PresentationSubmission deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String presentationSubmissionString = jp.getText();
        return mapper.readValue(presentationSubmissionString, PresentationSubmission.class);
    }
}
