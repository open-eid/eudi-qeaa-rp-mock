package ee.ria.eudi.qeaa.rp.model;

import com.nimbusds.jose.jwk.ECKey;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    private String transactionId;
    private String responseCode;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> presentationDefinition;
    @Lob
    private ECKey responseEncryptionKey;
    private String nonce;
    private String state;
}
