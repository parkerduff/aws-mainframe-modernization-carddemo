package com.aws.carddemo.repository;

import com.aws.carddemo.entity.CardXrefEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link CardXrefEntity}, replacing access to the XREFFILE KSDS.
 * Resolving a card number to an account id (findByCardNum) mirrors the CBTRN02C
 * 1500-A-LOOKUP-XREF read.
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXrefEntity, String> {

    Optional<CardXrefEntity> findByCardNum(String cardNum);

    List<CardXrefEntity> findByAcctId(Long acctId);

    List<CardXrefEntity> findByCustId(Long custId);
}
