package com.devmh;

import com.devmh.util.DeepDiff;
import com.devmh.util.DeepDiffUtil;
import com.google.common.collect.MapDifference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.instancio.Select.all;
import static org.junit.jupiter.api.Assertions.*;

public class DeepDiffTest {

    private final int maxDepth = 3;

    Settings settings = Settings.create()
            .set(Keys.MAX_DEPTH, maxDepth) // use same as model max
            .set(Keys.BEAN_VALIDATION_ENABLED, true)
            .set(Keys.JPA_ENABLED, true)
            .set(Keys.FAIL_ON_ERROR, true)
            .set(Keys.SET_BACK_REFERENCES, true)
            .mapType(SealedBean.class, MutableBean.class);

    @Data
    static final class X {
        //X peer;
        Y y;
        String s;
    }

    @Data
    static final class Y {
        String string;
        @ToString.Exclude
        X peer;
    }

    sealed interface SealedBean permits MutableBean, ImmutableBean {
        String string();
        int i();
        long l();
        double d();
        boolean b();
        char c();
        SealedBean peer();
        SealedBean[] array();
        Set<SealedBean> set();
        //SortedSet<SealedBean> sortedSet();
        List<SealedBean> list();
        Map<String, SealedBean> map();
    }

    record ImmutableBean(String string,
            int i,
            long l,
            double d,
            boolean b,
            char c,
            SealedBean peer,
            SealedBean[] array,
            Set<SealedBean> set,
            //SortedSet<SealedBean> sortedSet,
            List<SealedBean> list,
            Map<String, SealedBean> map) implements SealedBean {}

    @Data
    @Accessors(fluent = true)
    static final class MutableBean implements SealedBean {
        String string;
        int i;
        long l;
        double d;
        boolean b;
        char c;
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        SealedBean peer;
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        SealedBean[] array;
        @ToString.Exclude
        Set<SealedBean> set;
        //@ToString.Exclude
        //SortedSet<SealedBean> sortedSet;
        @ToString.Exclude
        List<SealedBean> list;
        @ToString.Exclude
        Map<String, SealedBean> map;
    }

    @Test
    @Disabled
    void testX() {
        X b1 = Instancio.of(X.class).withSettings(settings).withMaxDepth(maxDepth).create();
        DeepDiff diff = new DeepDiff(Set.of(), Set.of(), maxDepth);
        MapDifference<String, Object> diffs = diff.diff(b1, b1);
        diffs.entriesDiffering().forEach((k,v) ->
                System.out.println(k + ":from " + v.leftValue() + " to " + v.rightValue()));
        diffs.entriesOnlyOnLeft().forEach((k,v) ->
                System.out.println(k + ":from " + v + " to null"));
        diffs.entriesOnlyOnRight().forEach((k,v) ->
                System.out.println(k + ":from null to " + v));
        System.out.println(b1);
    }

    @Test
    @Disabled
    void testNoDiff() {
        MutableBean b1 = Instancio.of(MutableBean.class).withMaxDepth(maxDepth).create();
        DeepDiff diff = new DeepDiff(Set.of(), Set.of(), maxDepth);
        MapDifference<String, Object> diffs = diff.diff(b1, b1);
        diffs.entriesDiffering().forEach((k,v) ->
                System.out.println(k + ":from " + v.leftValue() + " to " + v.rightValue()));
        diffs.entriesOnlyOnLeft().forEach((k,v) ->
                System.out.println(k + ":from " + v + " to null"));
        diffs.entriesOnlyOnRight().forEach((k,v) ->
                System.out.println(k + ":from null to " + v));
    }

    @Test
    //@Disabled
    void testAllDiff() {
        System.out.println("testAllDiff()");
        MutableBean b1 = Instancio.of(MutableBean.class)
                .subtype(all(SealedBean.class), MutableBean.class)
                .withSettings(settings)
                .withSeed(0)
                .withMaxDepth(maxDepth)
                .create();
        MutableBean b2 = Instancio.of(MutableBean.class)
                .subtype(all(SealedBean.class), MutableBean.class)
                .withSettings(settings)
                .withSeed(1)
                .withMaxDepth(maxDepth)
                .create();
        DeepDiff diff = new DeepDiff(Set.of(), Set.of(), maxDepth);
        MapDifference<String, Object> diffs = diff.diff(b1, b2);
        diffs.entriesDiffering().forEach((k,v) ->
                System.out.println(k + ":from " + v.leftValue() + " to " + v.rightValue()));
        diffs.entriesOnlyOnLeft().forEach((k,v) ->
                System.out.println(k + ":from " + v + " to null"));
        diffs.entriesOnlyOnRight().forEach((k,v) ->
                System.out.println(k + ":from null to " + v));
        //System.out.println(b1.array[0]);
        //System.out.println(b2.array[0]);
        System.out.println(b1);
        System.out.println(b2);
    }

    /*
    @Test
    void testMalformedFieldPathsAndNulls() {
        runRobustnessChecks(
                Person.class,
                List.of(
                        p -> p.address = null, // null intermediate path
                        p -> p.nicknames = null // null collection
                ),
                List.of(
                        "nonexistent",               // bogus top-level
                        "address.fakeField",         // bogus nested
                        "nicknames[999]",            // out-of-bounds
                        "address.city.zipcode"       // too deep
                )
        );
    }

    public static <T> void runRobustnessChecks(
            Class<T> type,
            List<Consumer<T>> mutations,
            List<String> bogusPaths
    ) {
        T original = Instancio.create(type);
        T modified = Instancio.create(type);

        // Apply nulls or structural mutations
        for (Consumer<T> mutator : mutations) {
            mutator.accept(modified);
        }

        // Include bogus fields in inclusion set
        Set<String> include = new HashSet<>(bogusPaths);
        include.addAll(List.of("name", "age")); // legit fields for sanity

        DeepDiffUtil util = new DeepDiffUtil(include, Set.of());

        try {
            List<DeepDiffUtil.Difference> diffs = util.diff(original, modified);
            System.out.println("Robustness check diffs:");
            diffs.forEach(System.out::println);
            assertNotNull(diffs);
        } catch (Exception e) {
            fail("DeepDiffUtil should not throw even on malformed or null paths. Threw: " + e);
        }
    }
     */

    /*
    @Test
    void testMultipleChangesDetected() {
        runTest(
                Person.class,
                List.of(
                        mutation("name", Person::getName, (p, v) -> p.name = v, "Alice"),
                        mutation("age", p -> p.age, (p, v) -> p.age = 42, 42),
                        mutation("address.city", p -> p.address.city, (p, v) -> p.address.city = v, "New City")
                ),
                Set.of(), // include all
                Set.of()  // exclude none
        );
    }

    private <E> E mutation(String name, Object o, Object o1, String alice) {
    }

    @Test
    void testMultipleChangesDetected() {
        runTest(
                Person.class,
                List.of(
                        mutation("name", p -> p.name, (p, v) -> p.name = v, "Alice"),
                        mutation("age", p -> p.age, (p, v) -> p.age = 42, 42),
                        mutation("address.city", p -> p.address.city, (p, v) -> p.address.city = v, "New City")
                ),
                Set.of(), // include all
                Set.of()  // exclude none
        );
    }
     */
}
