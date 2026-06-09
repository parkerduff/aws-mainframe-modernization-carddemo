package com.aws.carddemo.repository;

import com.aws.carddemo.entity.AccountEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link AccountEntity}, replacing CICS file control / VSAM
 * access against the ACCTFILE KSDS.
 */
@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    List<AccountEntity> findByActiveStatus(String activeStatus);

    List<AccountEntity> findByGroupId(String groupId);
}
