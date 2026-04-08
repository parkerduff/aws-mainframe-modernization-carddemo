package com.cardemo.repository;

import com.cardemo.entity.DisclosureGroup;
import com.cardemo.entity.DisclosureGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, DisclosureGroupId> {

    List<DisclosureGroup> findByAcctGroupId(String acctGroupId);
}
