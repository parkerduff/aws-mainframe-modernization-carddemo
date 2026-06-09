package com.aws.carddemo.repository;

import com.aws.carddemo.entity.TransactionEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link TransactionEntity}, replacing access to the TRANSACT
 * KSDS. The card-number ordered query supports the paged transaction list of COTRN00C.
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findByTranCardNum(String tranCardNum);

    Page<TransactionEntity> findByTranCardNumOrderByTranOrigTsDesc(String tranCardNum, Pageable pageable);

    List<TransactionEntity> findByTranCardNumOrderByTranOrigTsAsc(String tranCardNum);
}
