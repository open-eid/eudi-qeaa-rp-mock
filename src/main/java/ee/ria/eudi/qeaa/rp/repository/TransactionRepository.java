package ee.ria.eudi.qeaa.rp.repository;

import ee.ria.eudi.qeaa.rp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

}
