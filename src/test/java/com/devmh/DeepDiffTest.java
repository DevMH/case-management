package com.devmh;

import com.devmh.util.DeepDiffUtil;
import lombok.Data;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.bouncycastle.util.test.SimpleTest.runTest;

public class DeepDiffTest {

    @Data
    static class Address {
        String city;
        String street;
    }

    @Data
    static class Person {
        String name;
        int age;
        Address address;
        List<String> nicknames;
    }

    @Test
    void testDiffWithLoggingAndSkippedPaths() {
        Person p1 = Instancio.create(Person.class);
        Person p2 = Instancio.create(Person.class);
        p2.address = null;

        DeepDiffUtil util = new DeepDiffUtil(Set.of(), Set.of());
        List<DeepDiffUtil.Difference> diffs = util.diff(p1, p2);
        diffs.forEach(System.out::println);
        util.getSkippedPaths().forEach(System.out::println);
        assertFalse(diffs.isEmpty());
        assertFalse(util.getSkippedPaths().isEmpty());
    }

    /*
    @Test
    void testDiffWithControlledDifference() {
        Person p1 = Instancio.create(Person.class);
        Person p2 = Instancio.of(Person.class)
                .set(Select.field(Person::getName), p1.name)
                .set(Select.field(Person::getAddress::getCity), "ChangedCity")
                .create();

        DeepDiffUtil util = new DeepDiffUtil(Set.of(), Set.of());
        DeepDiffUtil.DifferenceSummary summary = util.summarizeDifferences(p1, p2);

        summary.printSummary();
        assertTrue(summary.both.stream().anyMatch(d -> d.path.equals("address.city")));
    }
    */

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
