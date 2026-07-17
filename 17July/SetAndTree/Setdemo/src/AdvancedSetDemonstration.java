import java.lang.reflect.Field;
import java.util.*;
import java.util.Stack;

/**
 * AdvancedSetDemonstration - A comprehensive, formal exploration of Java's
 * HashSet and LinkedHashSet implementations.
 *
 * <p>This class uses multiple focused static methods to demonstrate:
 * <ul>
 *   <li>Ordering guarantees (and lack thereof)</li>
 *   <li>Custom object deduplication via hashCode/equals contracts</li>
 *   <li>The "ghost object" pitfall when mutating identity fields</li>
 *   <li>Safe iteration removal patterns</li>
 *   <li>Internal resizing (capacity expansion) via reflection</li>
 *   <li>Set theory operations (union, intersection, difference)</li>
 *   <li>LinkedHashSet re-insertion ordering semantics</li>
 *   <li>Hash collision handling and treeification</li>
 *   <li>Performance benchmarking between implementations</li>
 * </ul>
 * </p>
 */
public class AdvancedSetDemonstration {

    public static void main(String[] args) throws Exception {
        demonstrateOrdering();
        demonstrateCustomDeduplication();
        demonstrateGhostObjectPhenomenon();
        demonstrateSafeIterationRemoval();
        demonstrateResizingMechanism();
        demonstrateSetTheoryOperations();
        demonstrateLinkedHashSetReinsertionOrder();
        demonstrateHashCollisionImpact();
        performPerformanceBenchmark();
        printSummary();
    }

    // ======================================================================
    // 1. ORDERING DIFFERENCES
    // ======================================================================
    private static void demonstrateOrdering() {
        System.out.println("========== 1. Ordering Differences ==========");
        HashSet<String> hashSet = new HashSet<>();
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();

        List<String> items = Arrays.asList("Delta", "Alpha", "Gamma", "Beta", "Epsilon");
        for (String item : items) {
            hashSet.add(item);
            linkedHashSet.add(item);
        }

        System.out.println("HashSet (non-deterministic bucket order):");
        hashSet.forEach(s -> System.out.print(s + " "));
        System.out.println("\nLinkedHashSet (strict insertion order):");
        linkedHashSet.forEach(s -> System.out.print(s + " "));
        System.out.println("\n");
    }

    // ======================================================================
    // 2. CUSTOM OBJECT DEDUPLICATION (hashCode/equals contract)
    // ======================================================================
    private static void demonstrateCustomDeduplication() {
        System.out.println("========== 2. Custom Object Deduplication ==========");
        HashSet<Employee> employeeSet = new HashSet<>();
        Employee emp1 = new Employee(101, "Alice", "Smith");
        Employee emp2 = new Employee(102, "Bob", "Johnson");
        Employee emp3 = new Employee(101, "Alice", "Smith"); // Duplicate ID

        Collections.addAll(employeeSet, emp1, emp2, emp3);
        System.out.println("Expected size (2 unique IDs), actual: " + employeeSet.size());
        employeeSet.forEach(System.out::println);
        System.out.println();
    }

    // ======================================================================
    // 3. GHOST OBJECT PHENOMENON (mutating hashCode fields)
    // ======================================================================
    private static void demonstrateGhostObjectPhenomenon() {
        System.out.println("========== 3. Ghost Object Phenomenon ==========");
        HashSet<Employee> set = new HashSet<>();
        Employee ghost = Employee.of(999, "Casper", "Ghost");
        set.add(ghost);

        System.out.println("Contains before mutation: " + set.contains(ghost));
        System.out.println("Hash consistent with ID? " + ghost.isHashConsistentWithId());
        ghost.setId(888); // Mutating a field used in hashCode()
        System.out.println("Contains after mutation: " + set.contains(ghost));
        System.out.println("Hash consistent with ID? " + ghost.isHashConsistentWithId());
        System.out.println("Remove attempt: " + set.remove(ghost));
        System.out.println("Set size (ghost is now trapped): " + set.size());

        // Advanced recovery: iterate to find the stranded object
        System.out.print("Scanning for the lost object: ");
        set.stream()
                .filter(e -> e.getFirstName().equals("Casper") && e.getLastName().equals("Ghost"))
                .findFirst()
                .ifPresentOrElse(
                        e -> System.out.println("Found stranded: " + e),
                        () -> System.out.println("Not found.")
                );
        System.out.println();
    }

    // ======================================================================
    // 4. SAFE ITERATION REMOVAL
    // ======================================================================
    private static void demonstrateSafeIterationRemoval() {
        System.out.println("========== 4. Safe Iteration Removal ==========");
        LinkedHashSet<String> words = new LinkedHashSet<>(Arrays.asList("one", "two", "three", "four", "five"));
        System.out.println("Original: " + words);

        Iterator<String> it = words.iterator();
        while (it.hasNext()) {
            if (it.next().length() == 3) {
                it.remove(); // Safe
            }
        }
        System.out.println("After removing length-3 words: " + words);

        // Unsafe version commented out (would throw ConcurrentModificationException)
        System.out.println();
    }

    // ======================================================================
    // 5. RESIZING MECHANISM (Reflection)
    // ======================================================================
    private static void demonstrateResizingMechanism() throws Exception {
        System.out.println("========== 5. Resizing Mechanism (Reflection) ==========");
        HashSet<Integer> set = new HashSet<>(4, 0.75f);
        Field tableField = HashMap.class.getDeclaredField("table");
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        tableField.setAccessible(true);
        thresholdField.setAccessible(true);

        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        HashMap<?, ?> map = (HashMap<?, ?>) mapField.get(set);

        for (int i = 1; i <= 10; i++) {
            set.add(i);
            Object[] table = (Object[]) tableField.get(map);
            int cap = (table == null) ? 0 : table.length;
            int threshold = (int) thresholdField.get(map);
            System.out.printf("Added %d | size=%d | capacity=%d | threshold=%d%n",
                    i, set.size(), cap, threshold);
        }
        System.out.println();
    }

    // ======================================================================
    // 6. SET THEORY OPERATIONS (Union, Intersection, Difference)
    // ======================================================================
    private static void demonstrateSetTheoryOperations() {
        System.out.println("========== 6. Set Theory Operations ==========");
        Set<Integer> setA = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        Set<Integer> setB = new LinkedHashSet<>(Arrays.asList(4, 5, 6, 7, 8));

        System.out.println("Set A (LinkedHashSet): " + setA);
        System.out.println("Set B (LinkedHashSet): " + setB);

        // Union
        Set<Integer> union = new LinkedHashSet<>(setA);
        union.addAll(setB);
        System.out.println("Union (preserves A's order then appends B): " + union);

        // Intersection
        Set<Integer> intersection = new LinkedHashSet<>(setA);
        intersection.retainAll(setB);
        System.out.println("Intersection: " + intersection);

        // Difference (A - B)
        Set<Integer> difference = new LinkedHashSet<>(setA);
        difference.removeAll(setB);
        System.out.println("Difference (A \\ B): " + difference);
        System.out.println();
    }

    // ======================================================================
    // 7. LINKEDHASHSET RE-INSERTION ORDER (removal moves to tail)
    // ======================================================================
    private static void demonstrateLinkedHashSetReinsertionOrder() {
        System.out.println("========== 7. LinkedHashSet Re-insertion Behavior ==========");
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("A", "B", "C", "D"));
        System.out.println("Initial order: " + set);

        set.remove("B");
        System.out.println("After removing 'B': " + set);

        set.add("B");
        System.out.println("After re-adding 'B' (moves to the tail): " + set);

        set.remove("A");
        set.add("A");
        System.out.println("After removing & re-adding 'A' (now at tail): " + set);
        System.out.println();
    }

    // ======================================================================
    // 8. HASH COLLISION SIMULATION (constant hashCode + Treeification)
    // ======================================================================
    private static void demonstrateHashCollisionImpact() throws Exception {
        System.out.println("========== 8. Hash Collision Simulation & Treeification ==========");
        HashSet<CollidingKey> collisionSet = new HashSet<>();

        // Add keys that all hash to the same bucket
        for (int i = 0; i < 15; i++) {
            collisionSet.add(new CollidingKey(i));
        }

        System.out.println("Inserted 15 CollidingKey objects (all hashCode=42).");
        System.out.println("Set size: " + collisionSet.size() + " (all distinct via equals)");

        // Peek into the internal table to see bucket distribution
        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        HashMap<?, ?> map = (HashMap<?, ?>) mapField.get(collisionSet);

        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);

        if (table != null) {
            int nonEmptyBuckets = 0;
            int maxChainLength = 0;
            for (Object bucket : table) {
                if (bucket != null) {
                    nonEmptyBuckets++;
                    int chainLength = countElementsInBucket(bucket);
                    maxChainLength = Math.max(maxChainLength, chainLength);
                }
            }
            System.out.printf("Internal table length: %d | Non-empty buckets: %d | Max chain/tree length: %d%n",
                    table.length, nonEmptyBuckets, maxChainLength);
            System.out.println("(Note: Since CollidingKey implements Comparable, the bucket with 15 entries");
            System.out.println(" has been internally converted into a Red-Black tree for O(log n) lookups.)");
        }
        System.out.println();
    }

    /**
     * Advanced reflection-based bucket inspector.
     * Counts elements in a bucket regardless of whether it is stored as a
     * linked list (Node) or a Red-Black tree (TreeNode).
     *
     * @param bucket the head of the bucket (could be Node or TreeNode)
     * @return total number of entries in this bucket
     * @throws Exception if reflection fails (should not happen in standard JDK 8+)
     */
    private static int countElementsInBucket(Object bucket) throws Exception {
        if (bucket == null) return 0;
        int count = 0;
        Stack<Object> stack = new Stack<>();
        stack.push(bucket);

        // Reflection setup for Node fields
        Class<?> nodeClass = Class.forName("java.util.HashMap$Node");
        Field nextField = nodeClass.getDeclaredField("next");
        nextField.setAccessible(true);

        // Reflection setup for TreeNode fields (may be null if class not loaded)
        Field leftField = null;
        Field rightField = null;
        try {
            Class<?> treeNodeClass = Class.forName("java.util.HashMap$TreeNode");
            leftField = treeNodeClass.getDeclaredField("left");
            leftField.setAccessible(true);
            rightField = treeNodeClass.getDeclaredField("right");
            rightField.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            // TreeNodes may not be present in the current JVM or bucket, fallback gracefully.
        }

        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (current == null) continue;
            count++;

            // Traverse 'next' chain (works for both Node and TreeNode)
            Object next = nextField.get(current);
            if (next != null) stack.push(next);

            // If it's a TreeNode and we have left/right fields, traverse tree subtrees
            if (leftField != null && rightField != null) {
                // Check if current is an instance of TreeNode (it extends Node)
                try {
                    if (current.getClass().getName().equals("java.util.HashMap$TreeNode")) {
                        Object left = leftField.get(current);
                        if (left != null) stack.push(left);
                        Object right = rightField.get(current);
                        if (right != null) stack.push(right);
                    }
                } catch (IllegalAccessException ignored) {
                    // Should not happen as fields are made accessible
                }
            }
        }
        return count;
    }

    // ======================================================================
    // 9. PERFORMANCE BENCHMARK
    // ======================================================================
    private static void performPerformanceBenchmark() {
        System.out.println("========== 9. Performance Benchmark (50k elements) ==========");
        int iterations = 50_000;
        Random random = new Random(42);

        // HashSet
        long start = System.nanoTime();
        Set<Integer> hashSet = new HashSet<>();
        for (int i = 0; i < iterations; i++) {
            hashSet.add(random.nextInt());
        }
        long hashAddTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            hashSet.contains(random.nextInt());
        }
        long hashContainsTime = System.nanoTime() - start;

        // LinkedHashSet
        random = new Random(42); // reset
        start = System.nanoTime();
        Set<Integer> linkedSet = new LinkedHashSet<>();
        for (int i = 0; i < iterations; i++) {
            linkedSet.add(random.nextInt());
        }
        long linkedAddTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            linkedSet.contains(random.nextInt());
        }
        long linkedContainsTime = System.nanoTime() - start;

        System.out.printf("HashSet    - Add time: %.2f ms | Contains time: %.2f ms%n",
                hashAddTime / 1_000_000.0, hashContainsTime / 1_000_000.0);
        System.out.printf("LinkedHashSet - Add time: %.2f ms | Contains time: %.2f ms%n",
                linkedAddTime / 1_000_000.0, linkedContainsTime / 1_000_000.0);
        System.out.println("(Note: LinkedHashSet is slightly slower due to linked-list maintenance.)");
        System.out.println();
    }

    // ======================================================================
    // 10. COMPREHENSIVE SUMMARY
    // ======================================================================
    private static void printSummary() {
        System.out.println("========== Comprehensive Summary ==========");
        System.out.println("1. Ordering:        HashSet → chaotic; LinkedHashSet → insertion order.");
        System.out.println("2. Deduplication:   Relies on hashCode() and equals(). Must override both.");
        System.out.println("3. Ghost Objects:   Mutating hashCode fields makes objects irretrievable.");
        System.out.println("4. Safe Removal:    Use Iterator.remove(), never Collection.remove() in loops.");
        System.out.println("5. Resizing:        Capacity doubles when size > loadFactor * capacity.");
        System.out.println("6. Set Operations:  LinkedHashSet preserves order in union/intersection results.");
        System.out.println("7. Re-insertion:    Re-adding a removed element pushes it to the tail.");
        System.out.println("8. Collisions:      Severe collisions degrade performance; implementing Comparable");
        System.out.println("                     allows Java to use Red-Black trees (treeification) for O(log n)");
        System.out.println("                     lookups even in heavily collided buckets.");
        System.out.println("9. Performance:     HashSet is generally faster, LinkedHashSet has modest overhead.");
    }

    // ======================================================================
    // ENHANCED DOMAIN CLASS 1: Employee
    // ======================================================================

    /**
     * A comprehensive Employee entity representing a full-fledged domain object.
     * <p>
     * <b>Identity Contract:</b> Equality and hash-code are based <i>solely</i> on the {@code id} field.
     * This is explicitly designed to demonstrate the "ghost object" pitfall when mutable identity
     * fields are modified after insertion into a hash-based collection.
     * </p>
     * <p>
     * <b>Additional complexities:</b>
     * <ul>
     *   <li>Rich state: first name, last name, department, and salary.</li>
     *   <li>Business validation in setters (e.g., non-negative salary).</li>
     *   <li>Copy constructor and factory method for safe cloning.</li>
     *   <li>Explicit warning method to check if the object's hash is "broken" relative to its ID.</li>
     * </ul>
     */
    static class Employee {
        // Core identity (mutable to demonstrate ghost objects)
        private int id;
        // Immutable parts (final) – these do not affect hashCode
        private final String firstName;
        private final String lastName;
        // Mutable attributes that do NOT participate in hashCode/equals
        private String department;
        private double salary;

        // --- Constructors ---
        public Employee(int id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.department = "Unassigned";
            this.salary = 0.0;
        }

        // Copy constructor for defensive copying
        public Employee(Employee other) {
            this(other.id, other.firstName, other.lastName);
            this.department = other.department;
            this.salary = other.salary;
        }

        // --- Static Factory ---
        public static Employee of(int id, String firstName, String lastName) {
            return new Employee(id, firstName, lastName);
        }

        // --- Getters & Setters with Validation ---
        public int getId() { return id; }

        /**
         * WARNING: Modifying this field after insertion into a HashSet/LinkedHashSet
         * will change the object's hash code, rendering it "lost" (a ghost).
         * Use with extreme caution.
         */
        public void setId(int id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getDepartment() { return department; }

        public void setDepartment(String department) {
            this.department = (department == null || department.isBlank()) ? "Unassigned" : department;
        }

        public double getSalary() { return salary; }

        public void setSalary(double salary) {
            if (salary < 0) {
                throw new IllegalArgumentException("Salary cannot be negative.");
            }
            this.salary = salary;
        }

        // --- Utility: check hash integrity ---
        public boolean isHashConsistentWithId() {
            return this.hashCode() == Objects.hash(id);
        }

        // --- Identity Contract (ONLY based on 'id') ---
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Employee)) return false;
            Employee other = (Employee) obj;
            return id == other.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id); // Intentionally only uses 'id'
        }

        @Override
        public String toString() {
            return String.format("Employee{id=%d, name='%s %s', dept='%s', salary=%.2f}",
                    id, firstName, lastName, department, salary);
        }
    }

    // ======================================================================
    // ENHANCED DOMAIN CLASS 2: CollidingKey (Implements Comparable for treeification)
    // ======================================================================

    /**
     * A deliberately problematic key class designed to force <b>severe hash collisions</b>.
     * <p>
     * <b>Collision Strategy:</b> All instances return {@code 42} from {@code hashCode()},
     * forcing the backing {@code HashMap} to store them in a single bucket.
     * </p>
     * <p>
     * <b>Advanced Integration with Java Internals:</b>
     * <ul>
     *   <li>Implements {@link Comparable} – this allows the JVM to upgrade the colliding
     *       bucket from a linked list to a <b>Red-Black tree</b> (treeification) once the
     *       bucket size exceeds {@code TREEIFY_THRESHOLD = 8}.</li>
     *   <li>Maintains both a numeric {@code value} and a {@code payload} string to simulate
     *       real-world data attached to a key.</li>
     *   <li>Includes a mutable flag {@code active} to demonstrate that even with treeification,
     *       modifying fields that affect {@code compareTo()} can break the tree invariants.</li>
     * </ul>
     * </p>
     */
    static class CollidingKey implements Comparable<CollidingKey> {
        private final int value;        // Distinct value used for comparison (immutable)
        private String payload;         // Additional data (mutable, but not used in comparison)
        private boolean active;         // Mutable state (not used in equals/hashCode/compareTo)

        public CollidingKey(int value) {
            this.value = value;
            this.payload = "Payload-" + value;
            this.active = true;
        }

        public int getValue() { return value; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        /**
         * Returns a constant hash code to force every instance into the same bucket.
         */
        @Override
        public int hashCode() {
            return 42; // Intentional collision
        }

        /**
         * Equality is based solely on the distinct {@code value}.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CollidingKey)) return false;
            return value == ((CollidingKey) obj).value;
        }

        /**
         * Natural ordering based on {@code value}.
         * <p>
         * <b>Why this matters:</b> When a bucket grows beyond 8 entries, Java's {@code HashMap}
         * converts the linked list into a tree. The tree relies on {@code compareTo()} for
         * ordering. Since our keys all have the same {@code hashCode()} but distinct
         * {@code value}s, they can be efficiently stored in a balanced tree, turning
         * worst-case O(n) lookup into O(log n).
         * </p>
         */
        @Override
        public int compareTo(CollidingKey other) {
            return Integer.compare(this.value, other.value);
        }

        @Override
        public String toString() {
            return String.format("CK(value=%d, payload='%s', active=%s)", value, payload, active);
        }
    }
}