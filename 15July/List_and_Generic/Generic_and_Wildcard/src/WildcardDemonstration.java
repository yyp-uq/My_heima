import java.util.*;
import java.util.function.Predicate;

/**
 * A comprehensive demonstration of generic wildcards in Java,
 * illustrating bounded wildcards (producer/consumer), unbounded wildcards,
 * and wildcard capture. This class serves as a formal reference for
 * understanding wildcard semantics and their practical applications.
 */
public final class WildcardDemonstration {

    // ==================== 1. Unbounded Wildcard ====================
    /**
     * Displays the elements of any collection to the standard output.
     * Only read operations are permitted; the collection's element type
     * remains unknown at compile time.
     *
     * @param collection the collection to display (non-null)
     */
    public static void displayCollection(final Collection<?> collection) {
        for (final Object element : collection) {
            System.out.print(element + " ");
        }
        System.out.println();
    }

    // ==================== 2. Upper-Bounded Wildcard (Producer) ====================
    /**
     * Computes the arithmetic sum of a list of numeric values.
     * The method is generic in that it accepts any subtype of {@link Number}.
     *
     * @param numbers the list of numbers to sum (non-null)
     * @return the total sum as a {@code double}
     */
    public static double computeSum(final List<? extends Number> numbers) {
        double total = 0.0;
        for (final Number number : numbers) {
            total += number.doubleValue();
        }
        return total;
    }

    // ==================== 3. Lower-Bounded Wildcard (Consumer) ====================
    /**
     * Populates the given list with consecutive integers starting from 0.
     * The list must accept elements of type {@code Integer} or any supertype.
     *
     * @param list   the list to receive the integers (non-null)
     * @param count  the number of integers to add
     */
    public static void populateWithIntegers(final List<? super Integer> list, final int count) {
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
    }

    // ==================== 4. Wildcard Capture ====================
    /**
     * Swaps the elements at the specified positions in a list.
     * The wildcard is captured via a private helper to allow type-safe
     * read and write operations.
     *
     * @param list the list containing the elements (non-null)
     * @param i    the index of the first element
     * @param j    the index of the second element
     * @throws IndexOutOfBoundsException if either index is invalid
     */
    public static void exchangeElements(final List<?> list, final int i, final int j) {
        captureAndSwap(list, i, j);
    }

    // Helper that captures the wildcard as a type variable T
    private static <T> void captureAndSwap(final List<T> list, final int i, final int j) {
        final T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    // ==================== 5. Combining Producer and Consumer ====================
    /**
     * Filters elements from a source list using a predicate and copies
     * the matching elements to a destination list. The source acts as a
     * producer ({@code ? extends T}) and the destination as a consumer
     * ({@code ? super T}).
     *
     * @param src       the source list to read from (non-null)
     * @param dest      the destination list to write to (non-null)
     * @param predicate the condition to test each element (non-null)
     * @param <T>       the common type parameter
     */
    public static <T> void filterAndTransfer(
            final List<? extends T> src,
            final List<? super T> dest,
            final Predicate<? super T> predicate) {
        for (final T item : src) {
            if (predicate.test(item)) {
                dest.add(item);
            }
        }
    }

    // ==================== 6. Class with Unbounded Wildcard Field ====================
    /**
     * A container that holds a collection of an unknown type.
     * It can be assigned any collection, but direct modifications are
     * restricted (except for entire replacement) due to type safety.
     */
    public static final class FlexibleBox {
        private List<?> content = new ArrayList<>();

        public void setContent(final List<?> newContent) {
            this.content = Objects.requireNonNull(newContent);
        }

        public void displayContent() {
            for (final Object element : content) {
                System.out.print(element + " ");
            }
            System.out.println();
        }

        // No add() method is provided intentionally
    }

    // ==================== 7. Intersection with Bounded Wildcards ====================
    /**
     * Computes the intersection of two lists and stores the result in
     * a destination list. Both source lists are read-only producers,
     * while the destination is a consumer that accepts supertypes of T.
     *
     * @param list1  first source list (non-null)
     * @param list2  second source list (non-null)
     * @param result destination list for the intersection (non-null)
     * @param <T>    the element type
     */
    public static <T> void determineIntersection(
            final List<? extends T> list1,
            final List<? extends T> list2,
            final List<? super T> result) {
        final Set<T> temporarySet = new HashSet<>(list1);
        for (final T item : list2) {
            if (temporarySet.contains(item)) {
                result.add(item);
            }
        }
    }

    // ==================== Main Entry Point ====================
    public static void main(final String[] args) {
        // 1. Unbounded wildcard display
        final List<String> stringList = Arrays.asList("Alpha", "Beta", "Gamma");
        final List<Integer> integerList = Arrays.asList(10, 20, 30);
        System.out.print("String list: ");
        displayCollection(stringList);
        System.out.print("Integer list: ");
        displayCollection(integerList);

        // 2. Summation of numeric lists
        final List<Integer> intValues = Arrays.asList(1, 2, 3, 4);
        final List<Double> doubleValues = Arrays.asList(1.5, 2.5, 3.5);
        System.out.println("Sum of intValues: " + computeSum(intValues));
        System.out.println("Sum of doubleValues: " + computeSum(doubleValues));

        // 3. Populate a Number list with integers
        final List<Number> numberList = new ArrayList<>();
        populateWithIntegers(numberList, 5);
        System.out.print("Number list after population: ");
        displayCollection(numberList);  // Output: 0 1 2 3 4

        // 4. Swap elements (wildcard capture)
        final List<String> names = new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie"));
        exchangeElements(names, 0, 2);
        System.out.println("Names after swap: " + names);

        // 5. Filter and transfer even numbers
        final List<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 6);
        final List<Number> destination = new ArrayList<>();
        filterAndTransfer(source, destination, n -> n % 2 == 0);
        System.out.print("Even numbers transferred: ");
        displayCollection(destination);

        // 6. FlexibleBox usage
        final FlexibleBox box = new FlexibleBox();
        box.setContent(Arrays.asList("X", "Y", "Z"));
        System.out.print("FlexibleBox content: ");
        box.displayContent();
        box.setContent(Arrays.asList(100, 200, 300));
        System.out.print("FlexibleBox new content: ");
        box.displayContent();

        // 7. Intersection computation
        final List<Integer> first = Arrays.asList(1, 2, 3, 4);
        final List<Integer> second = Arrays.asList(3, 4, 5, 6);
        final List<Number> intersectionResult = new ArrayList<>();
        determineIntersection(first, second, intersectionResult);
        System.out.println("Intersection: " + intersectionResult); // [3, 4]

        // Additional demonstration of lower-bounded wildcard restriction
        final List<? super Integer> lowerBoundedList = new ArrayList<Number>();
        lowerBoundedList.add(100);               // allowed
        // Integer retrieved = lowerBoundedList.get(0); // compile error
        final Object retrieved = lowerBoundedList.get(0);
        System.out.println("Object retrieved from lower-bounded list: " + retrieved);
    }
}