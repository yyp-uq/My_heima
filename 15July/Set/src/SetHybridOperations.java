import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A comprehensive utility class that demonstrates advanced manipulation and
 * hybridization of the three core Java Set implementations:
 * {@link HashSet}, {@link LinkedHashSet}, and {@link TreeSet}.
 *
 * <p>This class provides a rich set of algorithms that combine these
 * implementations to leverage their distinct characteristics:
 * <ul>
 *   <li><b>HashSet</b> – O(1) average time for basic operations, no ordering</li>
 *   <li><b>LinkedHashSet</b> – predictable iteration order (insertion-order)</li>
 *   <li><b>TreeSet</b> – sorted order with O(log n) operations, supporting
 *   {@link NavigableSet} navigation</li>
 * </ul>
 *
 * <p>All methods are static and designed to be side-effect-free unless
 * explicitly noted. Input parameters are validated with
 * {@link Objects#requireNonNull} to fail-fast on null references.
 *
 * @author Advanced Java Developer
 * @version 2.0
 * @since 1.0
 */
public final class SetHybridOperations {

    // ==================== Private Constructor ====================

    private SetHybridOperations() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 1. Fundamental Set Transformations ====================

    /**
     * Eliminates duplicate elements from the input list while preserving the
     * original encounter order. The operation is implemented via a
     * {@link LinkedHashSet}, which provides both uniqueness and order retention.
     *
     * @param list the input list, may contain {@code null} elements if the
     *             list type permits them
     * @param <T>  the type of elements in the list
     * @return a new {@code List} containing the distinct elements in their
     * first occurrence order
     * @throws NullPointerException if the input list is {@code null}
     */
    public static <T> List<T> deduplicatePreservingOrder(final List<? extends T> list) {
        Objects.requireNonNull(list, "Input list must not be null");
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    /**
     * Converts a collection into a sorted set using the natural ordering of its
     * elements. The result is a {@link TreeSet} that contains all unique
     * elements from the source collection, sorted according to their
     * {@link Comparable} implementation.
     *
     * @param collection the source collection (non-null)
     * @param <T>        the element type, must implement {@code Comparable}
     * @return a new {@code TreeSet} containing the sorted unique elements
     * @throws NullPointerException if {@code collection} is {@code null}
     * @throws ClassCastException   if any element is not {@code Comparable}
     */
    public static <T extends Comparable<? super T>> TreeSet<T> toSortedSet(
            final Collection<? extends T> collection) {
        Objects.requireNonNull(collection, "Collection must not be null");
        return new TreeSet<>(collection);
    }

    /**
     * Converts a collection into a sorted set using a custom comparator.
     *
     * @param collection the source collection (non-null)
     * @param comparator the comparator to determine the order (must be consistent
     *                   with {@code equals} if required by the application)
     * @param <T>        the element type
     * @return a new {@code TreeSet} with the specified ordering
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <T> TreeSet<T> toSortedSet(
            final Collection<? extends T> collection,
            final Comparator<? super T> comparator) {
        Objects.requireNonNull(collection, "Collection must not be null");
        Objects.requireNonNull(comparator, "Comparator must not be null");
        final TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        return treeSet;
    }

    /**
     * Returns a {@link LinkedHashSet} copy of the given set. This is useful for
     * ensuring predictable iteration order while retaining the uniqueness property.
     *
     * @param set the input set (non-null)
     * @param <T> the element type
     * @return a {@code LinkedHashSet} containing the same elements as the input
     * @throws NullPointerException if {@code set} is {@code null}
     */
    public static <T> LinkedHashSet<T> toOrderedSet(final Set<? extends T> set) {
        Objects.requireNonNull(set, "Set must not be null");
        return new LinkedHashSet<>(set);
    }

    // ==================== 2. Set Algebra with Mixed Implementations ====================

    /**
     * Computes the intersection (common elements) of two sets. To optimize
     * performance, the method iterates over the smaller set and checks
     * membership in the larger set.
     *
     * <p>The result is returned as a {@link LinkedHashSet} to preserve the
     * iteration order of the first argument, which is useful when the order
     * of the first set carries semantic meaning.
     *
     * @param set1 the first set (non-null)
     * @param set2 the second set (non-null)
     * @param <T>  the element type
     * @return a {@code LinkedHashSet} containing the elements that appear in both sets
     * @throws NullPointerException if either set is {@code null}
     */
    public static <T> LinkedHashSet<T> computeIntersection(
            final Set<? extends T> set1,
            final Set<? extends T> set2) {
        Objects.requireNonNull(set1, "First set must not be null");
        Objects.requireNonNull(set2, "Second set must not be null");

        final Set<? extends T> smaller = (set1.size() <= set2.size()) ? set1 : set2;
        final Set<? extends T> larger  = (set1.size() <= set2.size()) ? set2 : set1;

        final LinkedHashSet<T> result = new LinkedHashSet<>();
        for (final T element : smaller) {
            if (larger.contains(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Computes the union of two sets. The result is returned as a {@link HashSet}
     * for optimal performance; order is not preserved.
     *
     * @param set1 the first set (non-null)
     * @param set2 the second set (non-null)
     * @param <T>  the element type
     * @return a {@code HashSet} containing all distinct elements from both sets
     * @throws NullPointerException if either set is {@code null}
     */
    public static <T> HashSet<T> computeUnion(final Set<? extends T> set1, final Set<? extends T> set2) {
        Objects.requireNonNull(set1, "First set must not be null");
        Objects.requireNonNull(set2, "Second set must not be null");
        final HashSet<T> result = new HashSet<>(set1);
        result.addAll(set2);
        return result;
    }

    /**
     * Computes the asymmetric difference (relative complement) {@code set1 \ set2},
     * i.e., elements that are in {@code set1} but not in {@code set2}.
     *
     * <p>The result preserves the iteration order of {@code set1} by using
     * a {@link LinkedHashSet} as the return type.
     *
     * @param set1 the minuend set (non-null)
     * @param set2 the subtrahend set (non-null)
     * @param <T>  the element type
     * @return a {@code LinkedHashSet} containing the elements present in
     * {@code set1} but absent in {@code set2}
     * @throws NullPointerException if either set is {@code null}
     */
    public static <T> LinkedHashSet<T> computeDifference(final Set<? extends T> set1, final Set<? extends T> set2) {
        Objects.requireNonNull(set1, "First set must not be null");
        Objects.requireNonNull(set2, "Second set must not be null");
        final LinkedHashSet<T> result = new LinkedHashSet<>(set1);
        result.removeAll(set2);
        return result;
    }

    /**
     * Computes the symmetric difference (elements that are in exactly one of the sets).
     * This is equivalent to {@code (set1 ∪ set2) \ (set1 ∩ set2)}.
     *
     * <p>The result is returned as a {@link HashSet} for performance; order is not guaranteed.
     *
     * @param set1 the first set (non-null)
     * @param set2 the second set (non-null)
     * @param <T>  the element type
     * @return a {@code HashSet} containing elements that belong to exactly one of the input sets
     * @throws NullPointerException if either set is {@code null}
     */
    public static <T> HashSet<T> computeSymmetricDifference(final Set<? extends T> set1, final Set<? extends T> set2) {
        Objects.requireNonNull(set1, "First set must not be null");
        Objects.requireNonNull(set2, "Second set must not be null");
        final HashSet<T> result = new HashSet<>(set1);
        // Remove common elements from one side and add unique from the other
        final HashSet<T> temp = new HashSet<>(set2);
        temp.removeAll(set1);
        result.removeAll(set2);
        result.addAll(temp);
        return result;
    }

    /**
     * Performs an intersection followed by sorting. This is a composite operation
     * that first finds the common elements of two sets, then returns a
     * {@link TreeSet} sorted according to the provided comparator.
     *
     * @param set1       the first set (non-null)
     * @param set2       the second set (non-null)
     * @param comparator the comparator used for ordering the result (non-null)
     * @param <T>        the element type
     * @return a {@code TreeSet} containing the sorted intersection
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <T> TreeSet<T> intersectAndSort(
            final Set<? extends T> set1,
            final Set<? extends T> set2,
            final Comparator<? super T> comparator) {
        Objects.requireNonNull(set1, "First set must not be null");
        Objects.requireNonNull(set2, "Second set must not be null");
        Objects.requireNonNull(comparator, "Comparator must not be null");
        final LinkedHashSet<T> intersection = computeIntersection(set1, set2);
        final TreeSet<T> sorted = new TreeSet<>(comparator);
        sorted.addAll(intersection);
        return sorted;
    }

    // ==================== 3. Multi-Set Operations ====================

    /**
     * Computes the intersection of three or more sets. The result preserves
     * the order of the first set (via {@code LinkedHashSet}) and uses a
     * streaming reduction approach.
     *
     * @param first  the first set (non-null)
     * @param others the remaining sets (non-null, at least one)
     * @param <T>    the element type
     * @return a {@code LinkedHashSet} containing the elements common to all sets
     * @throws NullPointerException if any set or the varargs array is {@code null}
     * @throws IllegalArgumentException if no other sets are provided (the operation
     *         would be degenerate)
     */
    @SafeVarargs
    public static <T> LinkedHashSet<T> computeIntersectionOfMultiple(
            final Set<? extends T> first,
            final Set<? extends T>... others) {
        Objects.requireNonNull(first, "First set must not be null");
        Objects.requireNonNull(others, "Vargargs array must not be null");
        if (others.length == 0) {
            throw new IllegalArgumentException("At least one additional set is required");
        }
        Set<? extends T> current = first;
        for (final Set<? extends T> other : others) {
            Objects.requireNonNull(other, "One of the other sets is null");
            current = computeIntersection(current, other);
        }
        // The result is already a LinkedHashSet from computeIntersection, but we
        // rewrap to ensure order from the first set is preserved.
        return new LinkedHashSet<>(current);
    }

    /**
     * Computes the union of multiple sets using a streaming reduction.
     *
     * @param sets the sets to merge (non-null, at least two)
     * @param <T>  the element type
     * @return a {@code HashSet} containing all distinct elements
     * @throws NullPointerException if the array or any element is {@code null}
     * @throws IllegalArgumentException if fewer than two sets are provided
     */
    @SafeVarargs
    public static <T> HashSet<T> computeUnionOfMultiple(final Set<? extends T>... sets) {
        Objects.requireNonNull(sets, "Sets array must not be null");
        if (sets.length < 2) {
            throw new IllegalArgumentException("At least two sets are required for a union");
        }
        final HashSet<T> result = new HashSet<>();
        for (final Set<? extends T> set : sets) {
            Objects.requireNonNull(set, "A provided set is null");
            result.addAll(set);
        }
        return result;
    }

    // ==================== 4. Advanced TreeSet Specifics ====================

    /**
     * Extracts a sub-set from a {@link TreeSet} based on element ranges.
     * This leverages the {@link NavigableSet} API to obtain a view of the
     * set containing elements between {@code fromElement} (inclusive) and
     * {@code toElement} (exclusive).
     *
     * @param treeSet     the source tree set (non-null)
     * @param fromElement the lower bound (inclusive), may be {@code null}
     *                    meaning unbounded
     * @param toElement   the upper bound (exclusive), may be {@code null}
     *                    meaning unbounded
     * @param <T>         the element type
     * @return a {@code NavigableSet} view of the specified range
     * @throws NullPointerException if {@code treeSet} is {@code null}
     */
    public static <T> NavigableSet<T> getRangeView(
            final TreeSet<T> treeSet,
            final T fromElement,
            final T toElement) {
        Objects.requireNonNull(treeSet, "TreeSet must not be null");
        if (fromElement == null && toElement == null) {
            return treeSet;
        } else if (fromElement == null) {
            return treeSet.headSet(toElement, false);
        } else if (toElement == null) {
            return treeSet.tailSet(fromElement, true);
        } else {
            return treeSet.subSet(fromElement, true, toElement, false);
        }
    }

    /**
     * Returns the element strictly greater than the given element in a TreeSet,
     * or {@code null} if none exists.
     *
     * @param treeSet the tree set (non-null)
     * @param element the reference element
     * @param <T>     the element type
     * @return the next higher element, or {@code null}
     * @throws NullPointerException if {@code treeSet} or {@code element} is {@code null}
     */
    public static <T> T getHigher(final TreeSet<T> treeSet, final T element) {
        Objects.requireNonNull(treeSet, "TreeSet must not be null");
        Objects.requireNonNull(element, "Element must not be null");
        return treeSet.higher(element);
    }

    // ==================== 5. Performance Benchmarking Framework ====================

    /**
     * A comprehensive benchmark that measures the average time for add, contains,
     * and remove operations on a given Set implementation.
     *
     * <p>The benchmark runs the operation {@code iterations} times and reports
     * the average duration in nanoseconds.
     *
     * @param setSupplier      a supplier that produces a fresh empty set instance
     * @param dataSize         the number of elements to pre-populate for the test
     * @param elementGenerator a function that maps an index to an element
     * @param iterations       the number of times each operation is repeated
     * @param <T>              the element type
     * @return a {@code Map} associating operation names with their average
     *         execution time in nanoseconds
     * @throws NullPointerException if any parameter is {@code null}
     * @throws IllegalArgumentException if {@code dataSize} or {@code iterations}
     *         is non-positive
     */
    public static <T> Map<BenchmarkOperation, Double> performBenchmark(
            final java.util.function.Supplier<Set<T>> setSupplier,
            final int dataSize,
            final Function<Integer, T> elementGenerator,
            final int iterations) {
        Objects.requireNonNull(setSupplier, "Set supplier must not be null");
        Objects.requireNonNull(elementGenerator, "Element generator must not be null");
        if (dataSize <= 0) {
            throw new IllegalArgumentException("Data size must be positive: " + dataSize);
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be positive: " + iterations);
        }

        final Map<BenchmarkOperation, Double> results = new EnumMap<>(BenchmarkOperation.class);

        for (final BenchmarkOperation op : BenchmarkOperation.values()) {
            long totalNanos = 0L;
            for (int i = 0; i < iterations; i++) {
                final Set<T> set = setSupplier.get();
                // Pre-populate the set with dataSize elements for contains/remove tests
                for (int j = 0; j < dataSize; j++) {
                    set.add(elementGenerator.apply(j));
                }
                final long start = System.nanoTime();
                switch (op) {
                    case ADD:
                        set.add(elementGenerator.apply(dataSize + 1));
                        break;
                    case CONTAINS:
                        set.contains(elementGenerator.apply(dataSize / 2));
                        break;
                    case REMOVE:
                        set.remove(elementGenerator.apply(dataSize / 2));
                        break;
                }
                totalNanos += (System.nanoTime() - start);
            }
            results.put(op, totalNanos / (double) iterations);
        }
        return results;
    }

    /**
     * Enumeration of benchmark operation types.
     */
    public enum BenchmarkOperation {
        /** Test the {@code add} operation. */
        ADD,
        /** Test the {@code contains} operation. */
        CONTAINS,
        /** Test the {@code remove} operation. */
        REMOVE
    }

    // ==================== 6. Utility: Partitioning by Predicate ====================

    /**
     * Partitions a set into two subsets based on a predicate: one containing
     * elements that satisfy the predicate, and the other containing those that do not.
     *
     * <p>The results are returned as {@link LinkedHashSet} to preserve the
     * original order of the input set.
     *
     * @param set       the input set (non-null)
     * @param predicate the predicate used for partitioning (non-null)
     * @param <T>       the element type
     * @return a {@code Map} with keys {@code true} and {@code false} mapping to
     *         the corresponding subsets
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <T> Map<Boolean, LinkedHashSet<T>> partitionSet(
            final Set<? extends T> set,
            final Predicate<? super T> predicate) {
        Objects.requireNonNull(set, "Set must not be null");
        Objects.requireNonNull(predicate, "Predicate must not be null");
        final Map<Boolean, LinkedHashSet<T>> result = new EnumMap<>(Boolean.class);
        result.put(true, new LinkedHashSet<>());
        result.put(false, new LinkedHashSet<>());
        for (final T element : set) {
            result.get(predicate.test(element)).add(element);
        }
        return result;
    }

    // ==================== 7. Main Demonstration ====================

    public static void main(final String[] args) {
        // ----- Sample Data -----
        final List<Integer> numbers = Arrays.asList(5, 3, 8, 1, 9, 3, 7, 5, 2, 4, 8, 6);
        System.out.println("Original list: " + numbers);

        // 1. Deduplicate preserving order
        final List<Integer> deduped = deduplicatePreservingOrder(numbers);
        System.out.println("Deduplicated (order preserved): " + deduped);

        // 2. Convert to sorted TreeSet
        final TreeSet<Integer> sortedSet = toSortedSet(numbers);
        System.out.println("Sorted TreeSet: " + sortedSet);

        // 3. Custom descending comparator
        final TreeSet<Integer> descSorted = toSortedSet(numbers, Comparator.reverseOrder());
        System.out.println("Sorted descending: " + descSorted);

        // ----- Set Algebra with Mixed Implementations -----
        final HashSet<Integer> hashSet = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        final TreeSet<Integer> treeSet = new TreeSet<>(Arrays.asList(4, 5, 6, 7, 8));
        final LinkedHashSet<Integer> linkedSet = new LinkedHashSet<>(Arrays.asList(3, 4, 9, 10));

        System.out.println("\nHashSet: " + hashSet);
        System.out.println("TreeSet: " + treeSet);
        System.out.println("LinkedHashSet: " + linkedSet);

        // Intersection (HashSet ∩ TreeSet)
        final LinkedHashSet<Integer> inter = computeIntersection(hashSet, treeSet);
        System.out.println("Intersection: " + inter);

        // Union (TreeSet ∪ LinkedHashSet)
        final HashSet<Integer> union = computeUnion(treeSet, linkedSet);
        System.out.println("Union: " + union);

        // Difference (LinkedHashSet \ HashSet)
        final LinkedHashSet<Integer> diff = computeDifference(linkedSet, hashSet);
        System.out.println("Difference: " + diff);

        // Symmetric difference
        final HashSet<Integer> symDiff = computeSymmetricDifference(hashSet, treeSet);
        System.out.println("Symmetric difference: " + symDiff);

        // Intersect then sort ascending
        final TreeSet<Integer> interSorted = intersectAndSort(hashSet, linkedSet, Comparator.naturalOrder());
        System.out.println("Intersection sorted: " + interSorted);

        // ----- Multi-set operations -----
        final Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> setB = new HashSet<>(Arrays.asList(2, 3, 4));
        final Set<Integer> setC = new HashSet<>(Arrays.asList(3, 4, 5));
        final LinkedHashSet<Integer> multiInter = computeIntersectionOfMultiple(setA, setB, setC);
        System.out.println("Intersection of {A,B,C}: " + multiInter); // [3]

        final HashSet<Integer> multiUnion = computeUnionOfMultiple(setA, setB, setC);
        System.out.println("Union of {A,B,C}: " + multiUnion); // [1,2,3,4,5]

        // ----- TreeSet range view -----
        final TreeSet<Integer> ts = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50, 60));
        final NavigableSet<Integer> range = getRangeView(ts, 20, 50);
        System.out.println("Range [20,50): " + range); // [20, 30, 40]

        // Higher element
        System.out.println("Higher than 35: " + getHigher(ts, 35)); // 40

        // ----- Partition -----
        final Set<Integer> sample = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        final Map<Boolean, LinkedHashSet<Integer>> partitions = partitionSet(sample, n -> n % 2 == 0);
        System.out.println("Even partition: " + partitions.get(true));
        System.out.println("Odd partition: " + partitions.get(false));

        // ----- Performance Benchmark -----
        System.out.println("\n--- Performance Benchmark (10,000 elements, 100 iterations) ---");
        final int dataSize = 10_000;
        final int iterations = 100;
        final Function<Integer, String> gen = i -> "Value" + i;

        final Map<BenchmarkOperation, Double> hashBench =
                performBenchmark(HashSet::new, dataSize, gen, iterations);
        final Map<BenchmarkOperation, Double> linkedBench =
                performBenchmark(LinkedHashSet::new, dataSize, gen, iterations);
        final Map<BenchmarkOperation, Double> treeBench =
                performBenchmark(TreeSet::new, dataSize, gen, iterations);

        System.out.printf("%-15s %15s %15s %15s%n",
                "Operation", "HashSet", "LinkedHashSet", "TreeSet");
        for (final BenchmarkOperation op : BenchmarkOperation.values()) {
            System.out.printf("%-15s %15.2f ns %15.2f ns %15.2f ns%n",
                    op, hashBench.get(op), linkedBench.get(op), treeBench.get(op));
        }

        // ----- Custom comparator with length then natural -----
        final TreeSet<String> customTree = new TreeSet<>(
                Comparator.comparingInt(String::length).thenComparing(String::compareTo));
        customTree.addAll(Arrays.asList("a", "bb", "ccc", "dd", "e", "ffff"));
        System.out.println("\nTreeSet sorted by length then alphabetical: " + customTree);
    }
}