import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * AdvancedMapDemonstration - A comprehensive, formal exploration of Java's
 * HashMap, LinkedHashMap, and TreeMap implementations.
 *
 * <p>This class uses multiple focused static methods to demonstrate:
 * <ul>
 *   <li>Ordering guarantees (none, insertion‑order, sorted order)</li>
 *   <li>Custom key deduplication via hashCode/equals/Comparable contracts</li>
 *   <li>The "ghost key" pitfall when mutating identity fields</li>
 *   <li>Safe iteration removal patterns</li>
 *   <li>Internal resizing (capacity expansion) via reflection</li>
 *   <li>Set‑theory operations on key sets (union, intersection, difference)</li>
 *   <li>LinkedHashMap access‑order vs. insertion‑order</li>
 *   <li>Hash collision handling and treeification in HashMap</li>
 *   <li>Performance benchmarking between implementations</li>
 * </ul>
 * </p>
 */
public class AdvancedMapDemonstration {

    public static void main(String[] args) throws Exception {
        demonstrateOrdering();
        demonstrateCustomKeyDeduplication();
        demonstrateGhostKeyPhenomenon();
        demonstrateSafeIterationRemoval();
        demonstrateResizingMechanism();
        demonstrateKeySetOperations();
        demonstrateLinkedHashMapAccessOrder();
        demonstrateHashCollisionImpact();
        performPerformanceBenchmark();
        printSummary();
    }

    // ======================================================================
    // 1. ORDERING DIFFERENCES
    // ======================================================================
    private static void demonstrateOrdering() {
        System.out.println("========== 1. Ordering Differences ==========");
        Map<String, Integer> hashMap = new HashMap<>();
        Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
        Map<String, Integer> treeMap = new TreeMap<>();

        List<String> keys = Arrays.asList("Delta", "Alpha", "Gamma", "Beta", "Epsilon");
        for (String key : keys) {
            int value = key.length();
            hashMap.put(key, value);
            linkedHashMap.put(key, value);
            treeMap.put(key, value);
        }

        System.out.println("HashMap (non‑deterministic bucket order):");
        hashMap.forEach((k, v) -> System.out.print(k + "=" + v + " "));
        System.out.println("\nLinkedHashMap (strict insertion order):");
        linkedHashMap.forEach((k, v) -> System.out.print(k + "=" + v + " "));
        System.out.println("\nTreeMap (natural order of keys):");
        treeMap.forEach((k, v) -> System.out.print(k + "=" + v + " "));
        System.out.println("\n");
    }

    // ======================================================================
    // 2. CUSTOM KEY DEDUPLICATION (hashCode/equals & Comparable)
    // ======================================================================
    private static void demonstrateCustomKeyDeduplication() {
        System.out.println("========== 2. Custom Key Deduplication ==========");
        Map<Employee, String> employeeMap = new HashMap<>();
        Employee emp1 = new Employee(101, "Alice", "Smith");
        Employee emp2 = new Employee(102, "Bob", "Johnson");
        Employee emp3 = new Employee(101, "Alice", "Smith"); // Duplicate ID

        employeeMap.put(emp1, "Developer");
        employeeMap.put(emp2, "Manager");
        employeeMap.put(emp3, "Lead");  // Should replace emp1's value because equals/hashCode are ID‑based

        System.out.println("Expected size (2 unique IDs), actual: " + employeeMap.size());
        employeeMap.forEach((k, v) -> System.out.println(k + " -> " + v));

        // TreeMap requires keys to be Comparable (Employee implements Comparable by ID)
        Map<Employee, String> treeMap = new TreeMap<>();
        treeMap.put(emp1, "Developer");
        treeMap.put(emp2, "Manager");
        treeMap.put(emp3, "Lead");
        System.out.println("TreeMap sorted by ID:");
        treeMap.forEach((k, v) -> System.out.println(k + " -> " + v));
        System.out.println();
    }

    // ======================================================================
    // 3. GHOST KEY PHENOMENON (mutating hashCode fields)
    // ======================================================================
    private static void demonstrateGhostKeyPhenomenon() {
        System.out.println("========== 3. Ghost Key Phenomenon ==========");
        Map<Employee, String> map = new HashMap<>();
        Employee ghost = Employee.of(999, "Casper", "Ghost");
        map.put(ghost, "Spirit");

        System.out.println("Contains before mutation: " + map.containsKey(ghost));
        ghost.setId(888); // Mutating a field used in hashCode()
        System.out.println("Contains after mutation: " + map.containsKey(ghost));
        System.out.println("Get after mutation: " + map.get(ghost));
        System.out.println("Remove attempt: " + map.remove(ghost));
        System.out.println("Map size (ghost is now trapped): " + map.size());

        // Advanced recovery: iterate to find the stranded key and its value
        System.out.print("Scanning for the lost key: ");
        map.entrySet().stream()
                .filter(e -> e.getKey().getFirstName().equals("Casper") && e.getKey().getLastName().equals("Ghost"))
                .findFirst()
                .ifPresentOrElse(
                        e -> System.out.println("Found stranded: " + e.getKey() + " -> " + e.getValue()),
                        () -> System.out.println("Not found.")
                );
        System.out.println();
    }

    // ======================================================================
    // 4. SAFE ITERATION REMOVAL (Iterator.remove())
    // ======================================================================
    private static void demonstrateSafeIterationRemoval() {
        System.out.println("========== 4. Safe Iteration Removal ==========");
        Map<Integer, String> map = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            map.put(i, "Value" + i);
        }
        System.out.println("Original: " + map);

        Iterator<Map.Entry<Integer, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> entry = it.next();
            if (entry.getKey() % 2 == 0) {
                it.remove(); // Safe
            }
        }
        System.out.println("After removing even keys: " + map);
        System.out.println();
    }

    // ======================================================================
    // 5. RESIZING MECHANISM (Reflection)
    // ======================================================================
    private static void demonstrateResizingMechanism() throws Exception {
        System.out.println("========== 5. Resizing Mechanism (Reflection) ==========");
        // HashMap
        Map<Integer, Integer> hashMap = new HashMap<>(4, 0.75f);
        Field tableField = HashMap.class.getDeclaredField("table");
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        tableField.setAccessible(true);
        thresholdField.setAccessible(true);

        for (int i = 1; i <= 10; i++) {
            hashMap.put(i, i * i);
            Object[] table = (Object[]) tableField.get(hashMap);
            int cap = (table == null) ? 0 : table.length;
            int threshold = (int) thresholdField.get(hashMap);
            System.out.printf("HashMap: added %d | size=%d | capacity=%d | threshold=%d%n",
                    i, hashMap.size(), cap, threshold);
        }

        // TreeMap does not have a table; it's a tree. Show size only.
        System.out.println("\nTreeMap does not have a table/capacity; it grows dynamically as a balanced tree.");
        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        for (int i = 1; i <= 10; i++) {
            treeMap.put(i, i * i);
            System.out.printf("TreeMap: added %d | size=%d%n", i, treeMap.size());
        }
        System.out.println();
    }

    // ======================================================================
    // 6. SET THEORY OPERATIONS ON KEY SETS (Union, Intersection, Difference)
    // ======================================================================
    private static void demonstrateKeySetOperations() {
        System.out.println("========== 6. Set Theory Operations on Key Sets ==========");
        Map<Integer, String> mapA = new LinkedHashMap<>();
        Map<Integer, String> mapB = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            mapA.put(i, "A" + i);
        }
        for (int i = 4; i <= 8; i++) {
            mapB.put(i, "B" + i);
        }

        System.out.println("Map A keys: " + mapA.keySet());
        System.out.println("Map B keys: " + mapB.keySet());

        // Union of key sets (preserves order from A, then B's extras)
        Set<Integer> union = new LinkedHashSet<>(mapA.keySet());
        union.addAll(mapB.keySet());
        System.out.println("Union: " + union);

        // Intersection
        Set<Integer> intersection = new LinkedHashSet<>(mapA.keySet());
        intersection.retainAll(mapB.keySet());
        System.out.println("Intersection: " + intersection);

        // Difference (A - B)
        Set<Integer> difference = new LinkedHashSet<>(mapA.keySet());
        difference.removeAll(mapB.keySet());
        System.out.println("Difference (A \\ B): " + difference);
        System.out.println();
    }

    // ======================================================================
    // 7. LINKEDHASHMAP ACCESS ORDER vs. INSERTION ORDER
    // ======================================================================
    private static void demonstrateLinkedHashMapAccessOrder() {
        System.out.println("========== 7. LinkedHashMap Access‑Order vs. Insertion‑Order ==========");

        // Insertion‑order (default)
        LinkedHashMap<String, Integer> insertionOrder = new LinkedHashMap<>();
        putAndShow(insertionOrder, "A", 1, "Insertion-order");

        // Access‑order (true)
        LinkedHashMap<String, Integer> accessOrder = new LinkedHashMap<>(16, 0.75f, true);
        putAndShow(accessOrder, "A", 1, "Access-order (before access)");
        // Now access some entries
        accessOrder.get("A");
        accessOrder.get("C");
        System.out.println("Access-order (after accessing A and C): " + accessOrder);
        System.out.println("(Most recently accessed entries move to the tail)");

        // Re‑insertion behavior: removing and re‑adding moves to tail in insertion‑order mode
        LinkedHashMap<String, Integer> reinsertMap = new LinkedHashMap<>();
        reinsertMap.put("X", 10);
        reinsertMap.put("Y", 20);
        reinsertMap.put("Z", 30);
        System.out.println("\nInitial insertion-order: " + reinsertMap);
        reinsertMap.remove("Y");
        reinsertMap.put("Y", 200);
        System.out.println("After removing and re‑adding 'Y' (moves to tail): " + reinsertMap);
        System.out.println();
    }

    private static void putAndShow(LinkedHashMap<String, Integer> map, String key, int val, String label) {
        map.put(key, val);
        System.out.println(label + ": " + map);
    }

    // ======================================================================
    // 8. HASH COLLISION SIMULATION (constant hashCode + Treeification)
    // ======================================================================
    private static void demonstrateHashCollisionImpact() throws Exception {
        System.out.println("========== 8. Hash Collision Simulation & Treeification ==========");
        Map<CollidingKey, String> collisionMap = new HashMap<>();

        // Add keys that all hash to the same bucket
        for (int i = 0; i < 15; i++) {
            collisionMap.put(new CollidingKey(i), "Value" + i);
        }

        System.out.println("Inserted 15 CollidingKey objects (all hashCode=42).");
        System.out.println("Map size: " + collisionMap.size() + " (all distinct via equals)");

        // Peek into the internal table to see bucket distribution
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(collisionMap);

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
            System.out.printf("Internal table length: %d | Non‑empty buckets: %d | Max chain/tree length: %d%n",
                    table.length, nonEmptyBuckets, maxChainLength);
            System.out.println("(Note: Since CollidingKey implements Comparable, the bucket with 15 entries");
            System.out.println(" has been internally converted into a Red‑Black tree for O(log n) lookups.)");
        }
        System.out.println();
    }

    /**
     * Advanced reflection-based bucket inspector (same as in Set demo, but re‑used).
     * Counts elements in a bucket regardless of whether it is stored as a
     * linked list (Node) or a Red‑Black tree (TreeNode).
     */
    private static int countElementsInBucket(Object bucket) throws Exception {
        if (bucket == null) return 0;
        int count = 0;
        Stack<Object> stack = new Stack<>();
        stack.push(bucket);

        Class<?> nodeClass = Class.forName("java.util.HashMap$Node");
        Field nextField = nodeClass.getDeclaredField("next");
        nextField.setAccessible(true);

        Field leftField = null, rightField = null;
        try {
            Class<?> treeNodeClass = Class.forName("java.util.HashMap$TreeNode");
            leftField = treeNodeClass.getDeclaredField("left");
            leftField.setAccessible(true);
            rightField = treeNodeClass.getDeclaredField("right");
            rightField.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            // TreeNodes may not be present in this JVM or bucket.
        }

        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (current == null) continue;
            count++;

            Object next = nextField.get(current);
            if (next != null) stack.push(next);

            if (leftField != null && rightField != null) {
                if (current.getClass().getName().equals("java.util.HashMap$TreeNode")) {
                    Object left = leftField.get(current);
                    if (left != null) stack.push(left);
                    Object right = rightField.get(current);
                    if (right != null) stack.push(right);
                }
            }
        }
        return count;
    }

    // ======================================================================
    // 9. PERFORMANCE BENCHMARK
    // ======================================================================
    private static void performPerformanceBenchmark() {
        System.out.println("========== 9. Performance Benchmark (50k insertions, 10k lookups) ==========");
        int iterations = 50_000;
        Random random = new Random(42);

        // HashMap
        long start = System.nanoTime();
        Map<Integer, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < iterations; i++) {
            int key = random.nextInt();
            hashMap.put(key, key);
        }
        long hashPutTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            hashMap.get(random.nextInt());
        }
        long hashGetTime = System.nanoTime() - start;

        // LinkedHashMap
        random = new Random(42);
        start = System.nanoTime();
        Map<Integer, Integer> linkedMap = new LinkedHashMap<>();
        for (int i = 0; i < iterations; i++) {
            int key = random.nextInt();
            linkedMap.put(key, key);
        }
        long linkedPutTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            linkedMap.get(random.nextInt());
        }
        long linkedGetTime = System.nanoTime() - start;

        // TreeMap
        random = new Random(42);
        start = System.nanoTime();
        Map<Integer, Integer> treeMap = new TreeMap<>();
        for (int i = 0; i < iterations; i++) {
            int key = random.nextInt();
            treeMap.put(key, key);
        }
        long treePutTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            treeMap.get(random.nextInt());
        }
        long treeGetTime = System.nanoTime() - start;

        System.out.printf("HashMap     - Put time: %.2f ms | Get time: %.2f ms%n",
                hashPutTime / 1_000_000.0, hashGetTime / 1_000_000.0);
        System.out.printf("LinkedHashMap - Put time: %.2f ms | Get time: %.2f ms%n",
                linkedPutTime / 1_000_000.0, linkedGetTime / 1_000_000.0);
        System.out.printf("TreeMap     - Put time: %.2f ms | Get time: %.2f ms%n",
                treePutTime / 1_000_000.0, treeGetTime / 1_000_000.0);
        System.out.println("(Note: TreeMap is generally slower due to tree balancing, but guarantees ordering.)");
        System.out.println();
    }

    // ======================================================================
    // 10. COMPREHENSIVE SUMMARY
    // ======================================================================
    private static void printSummary() {
        System.out.println("========== Comprehensive Summary ==========");
        System.out.println("1. Ordering:        HashMap → chaotic; LinkedHashMap → insertion order; TreeMap → sorted order.");
        System.out.println("2. Deduplication:   HashMap/LinkedHashMap rely on hashCode() and equals(); TreeMap relies on Comparable/Comparator.");
        System.out.println("3. Ghost Keys:      Mutating hashCode fields makes keys irretrievable.");
        System.out.println("4. Safe Removal:    Use Iterator.remove() when iterating, never map.remove(key) inside loops.");
        System.out.println("5. Resizing:        HashMap doubles capacity when size > loadFactor * capacity; TreeMap grows without rehashing.");
        System.out.println("6. Key Set Ops:     LinkedHashSet preserves order in union/intersection/difference results.");
        System.out.println("7. Access Order:    LinkedHashMap can be configured to order by access (LRU‑like).");
        System.out.println("8. Collisions:      Severe collisions degrade performance; implementing Comparable allows treeification.");
        System.out.println("9. Performance:     HashMap is fastest, LinkedHashMap slightly slower, TreeMap slowest but sorted.");
    }

    // ======================================================================
    // DOMAIN CLASS 1: Employee (for HashMap/LinkedHashMap keys)
    // ======================================================================

    /**
     * Employee as a key. Equality/hashCode based ONLY on 'id'.
     * Also implements Comparable to be usable in TreeMap (natural order by ID).
     */
    static class Employee implements Comparable<Employee> {
        private int id;
        private final String firstName;
        private final String lastName;
        private String department;
        private double salary;

        public Employee(int id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.department = "Unassigned";
            this.salary = 0.0;
        }

        public static Employee of(int id, String firstName, String lastName) {
            return new Employee(id, firstName, lastName);
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }  // DANGEROUS for hash‑based maps

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Employee)) return false;
            Employee e = (Employee) o;
            return id == e.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public int compareTo(Employee other) {
            return Integer.compare(this.id, other.id);
        }

        @Override
        public String toString() {
            return String.format("Employee{id=%d, name='%s %s'}", id, firstName, lastName);
        }
    }

    // ======================================================================
    // DOMAIN CLASS 2: CollidingKey (for collision/treeification demo)
    // ======================================================================

    /**
     * Deliberate collision key that always returns 42 as hashCode.
     * Implements Comparable to allow treeification.
     */
    static class CollidingKey implements Comparable<CollidingKey> {
        private final int value;

        public CollidingKey(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CollidingKey)) return false;
            return value == ((CollidingKey) o).value;
        }

        @Override
        public int compareTo(CollidingKey other) {
            return Integer.compare(this.value, other.value);
        }

        @Override
        public String toString() {
            return "CK(" + value + ")";
        }
    }
}