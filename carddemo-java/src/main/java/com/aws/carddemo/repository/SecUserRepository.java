package com.aws.carddemo.repository;

import com.aws.carddemo.entity.SecUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link SecUserEntity}, replacing access to the USRSEC KSDS
 * read by COSGN00C and maintained by COUSR00C-COUSR03C.
 */
@Repository
public interface SecUserRepository extends JpaRepository<SecUserEntity, String> {
}
