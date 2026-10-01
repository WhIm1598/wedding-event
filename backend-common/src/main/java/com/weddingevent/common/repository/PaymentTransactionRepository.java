package com.weddingevent.common.repository;

import com.weddingevent.common.domain.PaymentTransaction;
import com.weddingevent.common.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    @EntityGraph(attributePaths = "contract")
    List<PaymentTransaction> findAllByOrderByTransactionDateDescCreatedAtDesc();

    @Query("""
            select coalesce(sum(t.amount), 0) from PaymentTransaction t
            where t.type = :type and t.transactionDate between :from and :to""")
    BigDecimal sumByTypeBetween(@Param("type") TransactionType type, @Param("from") LocalDate from, @Param("to") LocalDate to);

    interface MonthlyTotal {
        Integer getMonth();

        BigDecimal getAmount();
    }

    @Query("""
            select extract(month from t.transactionDate) as month, sum(t.amount) as amount
            from PaymentTransaction t
            where t.type = :type and t.transactionDate between :from and :to
            group by extract(month from t.transactionDate)""")
    List<MonthlyTotal> monthlyTotals(@Param("type") TransactionType type, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
