package com.aws.carddemo.repository;

import com.aws.carddemo.entity.TranCatBalEntity;
import com.aws.carddemo.entity.TranCatBalId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link TranCatBalEntity}, replacing access to the TCATBALF KSDS
 * maintained by CBTRN02C (2700-UPDATE-TCATBAL).
 */
@Repository
public interface TranCatBalRepository extends JpaRepository<TranCatBalEntity, TranCatBalId> {

    List<TranCatBalEntity> findByIdAcctId(Long acctId);
}
