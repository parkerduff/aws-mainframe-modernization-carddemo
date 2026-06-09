package com.aws.carddemo.repository;

import com.aws.carddemo.entity.CardEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link CardEntity}, replacing access to the CARDFILE KSDS
 * (and its account alternate index) used by COCRDLIC / COCRDSLC / COCRDUPC.
 */
@Repository
public interface CardRepository extends JpaRepository<CardEntity, String> {

    Optional<CardEntity> findByCardNum(String cardNum);

    List<CardEntity> findByAcctId(Long acctId);
}
