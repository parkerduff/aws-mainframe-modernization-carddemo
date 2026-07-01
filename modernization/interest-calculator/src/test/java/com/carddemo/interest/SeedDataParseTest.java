package com.carddemo.interest;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.interest.config.FixedWidthFileLoader;
import com.carddemo.interest.domain.AccountRecord;
import com.carddemo.interest.domain.CardXrefRecord;
import com.carddemo.interest.domain.DisclosureGroupRecord;
import com.carddemo.interest.domain.TranCatBalRecord;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Confirms the fixed-width byte offsets match the real CardDemo ASCII seed files
 * (guards against the #1 migration pitfall: offset miscalculation).
 */
class SeedDataParseTest {

    @Test
    void parsesFirstAccountRecordFromSeedFile() {
        List<AccountRecord> accounts =
                FixedWidthFileLoader.loadAccounts(new ClassPathResource("seed/acctdata.txt"));
        assertThat(accounts).isNotEmpty();
        AccountRecord first = accounts.get(0);
        assertThat(first.getAcctId()).isEqualTo(1L);
        assertThat(first.getAcctActiveStatus()).isEqualTo("Y");
        assertThat(first.getAcctCurrBal()).isEqualByComparingTo("194.00");
        assertThat(first.getAcctOpenDate()).isEqualTo("2014-11-20");
    }

    @Test
    void parsesFirstDisclosureGroupRateFromSeedFile() {
        List<DisclosureGroupRecord> groups =
                FixedWidthFileLoader.loadDisclosureGroups(new ClassPathResource("seed/discgrp.txt"));
        DisclosureGroupRecord first = groups.get(0);
        assertThat(first.getDisAcctGroupId()).isEqualTo("A000000000");
        assertThat(first.getDisTranTypeCd()).isEqualTo("01");
        assertThat(first.getDisTranCatCd()).isEqualTo("0001");
        assertThat(first.getDisIntRate()).isEqualByComparingTo("15.00");
    }

    @Test
    void parsesTranCatBalAndXrefSeedFiles() {
        List<TranCatBalRecord> balances =
                FixedWidthFileLoader.loadTranCatBal(new ClassPathResource("seed/tcatbal.txt"));
        assertThat(balances).isNotEmpty();
        assertThat(balances.get(0).getTranCatAcctId()).isEqualTo(1L);
        assertThat(balances.get(0).getTranCatBal()).isEqualByComparingTo("0.00");

        List<CardXrefRecord> xrefs =
                FixedWidthFileLoader.loadCardXrefs(new ClassPathResource("seed/cardxref.txt"));
        assertThat(xrefs).isNotEmpty();
        assertThat(xrefs.get(0).getXrefCardNum()).hasSize(16);
        assertThat(xrefs.get(0).getXrefAcctId()).isPositive();
    }
}
