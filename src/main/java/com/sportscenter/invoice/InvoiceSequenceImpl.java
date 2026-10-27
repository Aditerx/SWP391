package com.sportscenter.invoice;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
/** Uses the configured dialect's real sequence syntax for PostgreSQL, H2, and SQL Server. */
@RequiredArgsConstructor
public class InvoiceSequenceImpl implements InvoiceSequence {
    private final EntityManager entityManager;
    @Override public long nextInvoiceCodeValue(){
        var factory=entityManager.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);
        String sql=factory.getJdbcServices().getDialect().getSequenceSupport().getSequenceNextValString("invoice_code_seq");
        return ((Number)entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }
}
