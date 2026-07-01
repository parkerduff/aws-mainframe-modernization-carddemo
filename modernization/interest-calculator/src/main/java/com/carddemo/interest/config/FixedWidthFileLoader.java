package com.carddemo.interest.config;

import com.carddemo.interest.domain.AccountRecord;
import com.carddemo.interest.domain.CardXrefRecord;
import com.carddemo.interest.domain.DisclosureGroupRecord;
import com.carddemo.interest.domain.TranCatBalRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.core.io.Resource;

/**
 * Loads the CardDemo ASCII flat files into typed records. Each non-empty line is one
 * fixed-width record; blank lines are skipped. Used by the standalone batch runner to
 * feed real seed data through {@code InterestCalculationBatch}.
 */
public final class FixedWidthFileLoader {

    private FixedWidthFileLoader() {
    }

    public static List<TranCatBalRecord> loadTranCatBal(Resource resource) {
        return load(resource, TranCatBalRecord::parse);
    }

    public static List<DisclosureGroupRecord> loadDisclosureGroups(Resource resource) {
        return load(resource, DisclosureGroupRecord::parse);
    }

    public static List<AccountRecord> loadAccounts(Resource resource) {
        return load(resource, AccountRecord::parse);
    }

    public static List<CardXrefRecord> loadCardXrefs(Resource resource) {
        return load(resource, CardXrefRecord::parse);
    }

    private static <T> List<T> load(Resource resource, Function<String, T> parser) {
        List<T> out = new ArrayList<>();
        try (InputStream in = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    out.add(parser.apply(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + resource.getDescription(), e);
        }
        return out;
    }
}
