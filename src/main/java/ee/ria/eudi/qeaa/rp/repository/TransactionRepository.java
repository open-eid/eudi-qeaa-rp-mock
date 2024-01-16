package ee.ria.eudi.qeaa.rp.repository;

import ee.ria.eudi.qeaa.rp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Optional<Transaction> findByResponseCode(String responseCode);
}
