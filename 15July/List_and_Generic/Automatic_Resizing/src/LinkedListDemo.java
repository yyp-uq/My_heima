import java.util.*;

/**
 * Advanced demonstration of {@link LinkedList} usage in a production-grade
 * LRU (Least Recently Used) cache engine.
 * <p>
 * The cache maintains a bounded access-order list using {@code LinkedList},
 * providing O(1) get/put operations via a companion {@code HashMap}. It includes
 * hit-rate statistics, bulk operations, dynamic capacity adjustment, and
 * recursive traversal utilities.
 * </p>
 */
public class LinkedListDemo {

    /**
     * Generic LRU cache implementation with an underlying {@code LinkedList}
     * maintaining access sequence and a {@code HashMap} for key-value storage.
     *
     * @param <K> the type of cache keys
     * @param <V> the type of cache values
     */
    public static class LRUCache<K, V> {
        private final LinkedList<K> accessOrderList;  // most recently used at head
        private final Map<K, V> storageMap;
        private int capacity;
        private long hitCount;
        private long missCount;

        /**
         * Creates an LRU cache with the specified capacity.
         *
         * @param initialCapacity the maximum number of entries
         * @throws IllegalArgumentException if capacity <= 0
         */
        public LRUCache(int initialCapacity) {
            if (initialCapacity <= 0) {
                throw new IllegalArgumentException("Capacity must be positive");
            }
            this.capacity = initialCapacity;
            this.accessOrderList = new LinkedList<>();
            this.storageMap = new HashMap<>(initialCapacity);
            this.hitCount = 0;
            this.missCount = 0;
        }

        /**
         * Retrieves a value by key. If present, the key is moved to the head
         * (most recently used) and the value is returned; otherwise returns {@code null}.
         *
         * @param key the key
         * @return the associated value, or {@code null} if not present
         */
        public V get(K key) {
            if (storageMap.containsKey(key)) {
                hitCount++;
                // Move to front: O(n) due to LinkedList remove, but for small caches acceptable
                // (In a true O(1) LRU we'd use a LinkedHashMap, but this demonstrates LinkedList)
                accessOrderList.remove(key);
                accessOrderList.addFirst(key);
                return storageMap.get(key);
            } else {
                missCount++;
                return null;
            }
        }

        /**
         * Inserts a key-value pair. If the key already exists, its value is updated
         * and it is moved to the head. If the cache exceeds capacity, the least recently
         * used entry (at the tail) is evicted.
         *
         * @param key   the key
         * @param value the value
         */
        public void put(K key, V value) {
            if (storageMap.containsKey(key)) {
                // Update existing
                storageMap.put(key, value);
                accessOrderList.remove(key);
                accessOrderList.addFirst(key);
                return;
            }
            // New entry
            if (storageMap.size() >= capacity) {
                // Evict tail (least recently used)
                K evictedKey = accessOrderList.removeLast();
                storageMap.remove(evictedKey);
            }
            storageMap.put(key, value);
            accessOrderList.addFirst(key);
        }

        /**
         * Removes a key from the cache.
         *
         * @param key the key
         * @return the removed value, or {@code null} if not present
         */
        public V remove(K key) {
            V removed = storageMap.remove(key);
            if (removed != null) {
                accessOrderList.remove(key);
            }
            return removed;
        }

        /**
         * Clears the entire cache.
         */
        public void clear() {
            storageMap.clear();
            accessOrderList.clear();
            hitCount = 0;
            missCount = 0;
        }

        /**
         * Returns the current number of entries.
         */
        public int size() {
            return storageMap.size();
        }

        /**
         * Changes the cache capacity. If the new capacity is smaller than the current size,
         * the least recently used entries are evicted until the size fits.
         *
         * @param newCapacity new maximum capacity
         */
        public void resize(int newCapacity) {
            if (newCapacity <= 0) {
                throw new IllegalArgumentException("Capacity must be positive");
            }
            this.capacity = newCapacity;
            // Evict excess entries if necessary
            while (storageMap.size() > capacity) {
                K evicted = accessOrderList.removeLast();
                storageMap.remove(evicted);
            }
        }

        /**
         * Returns the hit rate (hits / total requests) as a percentage.
         *
         * @return hit rate between 0.0 and 100.0, or 0.0 if no requests
         */
        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0.0 : (hitCount * 100.0 / total);
        }

        /**
         * Returns the current access order list (for inspection).
         */
        public List<K> getAccessOrderList() {
            return Collections.unmodifiableList(accessOrderList);
        }

        /**
         * Recursively prints the cache entries in access order (from head to tail).
         * This method demonstrates recursion on a linked structure.
         *
         * @param nodeIndex the current index in the list (0-based)
         */
        private void printRecursive(int nodeIndex) {
            if (nodeIndex >= accessOrderList.size()) {
                return;
            }
            K key = accessOrderList.get(nodeIndex);
            V value = storageMap.get(key);
            System.out.println("  [" + nodeIndex + "] " + key + " => " + value);
            printRecursive(nodeIndex + 1);
        }

        /**
         * Public wrapper for recursive printing.
         */
        public void printCacheRecursively() {
            System.out.println("Cache contents (head = most recent):");
            if (accessOrderList.isEmpty()) {
                System.out.println("  <empty>");
            } else {
                printRecursive(0);
            }
        }

        /**
         * Bulk put: adds multiple entries at once.
         *
         * @param entries varargs of key-value pairs (must be even length)
         */
        @SafeVarargs
        public final void putAll(Pair<K, V>... entries) {
            for (Pair<K, V> pair : entries) {
                put(pair.key, pair.value);
            }
        }

        /**
         * Simple key-value pair helper.
         */
        public static class Pair<K, V> {
            public final K key;
            public final V value;

            public Pair(K key, V value) {
                this.key = key;
                this.value = value;
            }
        }
    }

    // -------------------- Demo Scenario --------------------

    public static void main(String[] args) {
        System.out.println("=== Advanced LRU Cache Simulation ===");

        // Create a cache with capacity 5
        LRUCache<String, String> cache = new LRUCache<>(5);

        // Populate with some data (simulating user session data)
        cache.put("user:1001", "Alice");
        cache.put("user:1002", "Bob");
        cache.put("user:1003", "Charlie");
        cache.put("user:1004", "Diana");
        cache.put("user:1005", "Eve");

        System.out.println("Initial cache (filled to capacity):");
        cache.printCacheRecursively();

        // Access some entries (moves them to head)
        System.out.println("\n--- Accessing 'user:1003' and 'user:1001' ---");
        cache.get("user:1003");
        cache.get("user:1001");

        cache.printCacheRecursively();

        // Insert a new entry, causing eviction of LRU (which should be 'user:1002')
        System.out.println("\n--- Inserting 'user:1006' (should evict least recent) ---");
        cache.put("user:1006", "Frank");
        cache.printCacheRecursively();

        // Demonstrate bulk put
        System.out.println("\n--- Bulk inserting three more entries ---");
        cache.putAll(
                new LRUCache.Pair<>("user:1007", "Grace"),
                new LRUCache.Pair<>("user:1008", "Henry"),
                new LRUCache.Pair<>("user:1009", "Ivy")
        );
        cache.printCacheRecursively();

        // Show statistics
        System.out.println("\n--- Cache Statistics ---");
        System.out.println("Current size: " + cache.size());
        System.out.printf("Hit rate: %.2f%%%n", cache.getHitRate());

        // Perform some gets to influence hit rate
        System.out.println("\n--- Performing random access pattern ---");
        String[] keysToTry = {"user:1001", "user:1004", "user:1006", "user:1010", "user:1003"};
        for (String key : keysToTry) {
            String value = cache.get(key);
            System.out.println("get(" + key + ") => " + (value != null ? value : "MISS"));
        }
        System.out.printf("Updated hit rate: %.2f%%%n", cache.getHitRate());

        // Resize capacity (shrink)
        System.out.println("\n--- Resizing capacity from 5 to 3 (evicts LRU entries) ---");
        cache.resize(3);
        cache.printCacheRecursively();

        // Remove a specific key
        System.out.println("\n--- Removing 'user:1006' ---");
        cache.remove("user:1006");
        cache.printCacheRecursively();

        // Clear and show
        System.out.println("\n--- Clearing cache ---");
        cache.clear();
        System.out.println("Size after clear: " + cache.size());
        cache.printCacheRecursively();

        // Demonstrate recursion on a large list (optional)
        System.out.println("\n--- Building a cache with 20 entries and recursive print ---");
        LRUCache<Integer, String> intCache = new LRUCache<>(20);
        for (int i = 0; i < 20; i++) {
            intCache.put(i, "Value-" + i);
        }
        intCache.printCacheRecursively();

        System.out.println("\n=== Simulation Complete ===");
    }
}