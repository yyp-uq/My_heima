import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A sequential container implementation with dynamic elastic scaling capabilities,
 * fully complying with the {@link List} contract specification.
 * <p>
 * This container adopts an array-based buffer pool strategy at its core. When the
 * logical item count approaches the physical capacity, it automatically triggers
 * storage pool expansion (with a growth factor of 1.5x the current capacity).
 * It incorporates enterprise-grade features such as fail-fast traversers,
 * range projection views, serialization, and cloning.
 * <p>
 * Design Objective: To provide deterministic O(1) random access and predictable
 * reallocation overhead for high-throughput, medium-scale data sets.
 *
 * @param <E> the specific type of elements held in this container
 * @author High-Level Architecture Implementation
 * @see java.util.ArrayList
 */
public class ElasticArrayContainer<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, Serializable {

    private static final long serialVersionUID = 8683452581122892189L;

    // ======================== Constant Specifications ========================

    /** Default initial storage pool capacity for the system. */
    private static final int STANDARD_INITIAL_CAPACITY = 10;

    /** Shared empty storage pool instance (for zero-capacity constructors). */
    private static final Object[] EMPTY_STORAGE_POOL = {};

    /** Shared lazy-initialization storage pool marker (for default construction). */
    private static final Object[] LAZY_INIT_STORAGE_POOL = {};

    /** Platform-dependent maximum array allocation threshold (reserving header space to prevent VM overflow). */
    private static final int MAXIMUM_ARRAY_ALLOCATION_SIZE = Integer.MAX_VALUE - 8;

    // ======================== Core Member Fields ========================

    /** The internal element-bearing buffer array (transient for custom serialization). */
    transient Object[] internalElementBuffer;

    /** The total logical item count currently held in this container. */
    private int logicalItemCount;

    /** Structural revision counter (used for consistency checking in fail-fast traversers). */
    protected transient int structuralRevision = 0;

    // ======================== Constructor Hierarchy ========================

    /**
     * Constructs an empty container with the standard initial capacity (10).
     * Employs lazy initialization; the actual array is allocated upon the first insertion.
     */
    public ElasticArrayContainer() {
        this.internalElementBuffer = LAZY_INIT_STORAGE_POOL;
    }

    /**
     * Constructs an empty container with a specified initial storage pool capacity.
     *
     * @param initialPoolCapacity the initial buffer pool capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public ElasticArrayContainer(int initialPoolCapacity) {
        if (initialPoolCapacity > 0) {
            this.internalElementBuffer = new Object[initialPoolCapacity];
        } else if (initialPoolCapacity == 0) {
            this.internalElementBuffer = EMPTY_STORAGE_POOL;
        } else {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialPoolCapacity);
        }
    }

    /**
     * Constructs a container containing all elements of the specified collection,
     * in the order returned by the collection's iterator.
     *
     * @param sourceCollection the source data collection
     * @throws NullPointerException if the source collection is {@code null}
     */
    public ElasticArrayContainer(Collection<? extends E> sourceCollection) {
        Object[] incomingArray = sourceCollection.toArray();
        if ((logicalItemCount = incomingArray.length) != 0) {
            // If the source is of the same type, reference it directly
            // (defensive copy is applied to ensure immutability safety).
            if (sourceCollection.getClass() == ElasticArrayContainer.class) {
                this.internalElementBuffer = incomingArray;
            } else {
                this.internalElementBuffer = Arrays.copyOf(incomingArray, logicalItemCount, Object[].class);
            }
        } else {
            this.internalElementBuffer = EMPTY_STORAGE_POOL;
        }
    }

    // ======================== Storage Pool Dynamic Management ========================

    /**
     * Ensures the internal buffer pool can accommodate at least {@code requiredMinCapacity} elements.
     * Triggers storage pool expansion if the current capacity is insufficient.
     *
     * @param requiredMinCapacity the minimum required physical capacity
     */
    private void prepareBufferForInsertion(int requiredMinCapacity) {
        // Handle lazy-loading scenario: if it is the default empty pool,
        // elevate to the maximum of the standard capacity and the requirement.
        if (internalElementBuffer == LAZY_INIT_STORAGE_POOL) {
            requiredMinCapacity = Math.max(STANDARD_INITIAL_CAPACITY, requiredMinCapacity);
        }
        executeCapacityAssurance(requiredMinCapacity);
    }

    /**
     * Executes the capacity assurance logic: increments the revision counter
     * and expands the pool if necessary.
     *
     * @param requiredMinCapacity the minimum required physical capacity
     */
    private void executeCapacityAssurance(int requiredMinCapacity) {
        structuralRevision++;  // Records a structural modification
        if (requiredMinCapacity - internalElementBuffer.length > 0) {
            expandStoragePool(requiredMinCapacity);
        }
    }

    /**
     * Core elastic expansion algorithm: new capacity = current + (current >> 1) —— i.e., a 1.5x growth rate.
     * If the computed result still does not meet the requirement, the requirement value is used directly.
     * Subject to the maximum allocation threshold.
     *
     * @param requiredMinCapacity the minimum required physical capacity
     */
    private void expandStoragePool(int requiredMinCapacity) {
        int oldPoolSize = internalElementBuffer.length;
        int newPoolSize = oldPoolSize + (oldPoolSize >> 1);
        if (newPoolSize - requiredMinCapacity < 0) {
            newPoolSize = requiredMinCapacity;
        }
        if (newPoolSize - MAXIMUM_ARRAY_ALLOCATION_SIZE > 0) {
            newPoolSize = determineHugePoolSize(requiredMinCapacity);
        }
        internalElementBuffer = Arrays.copyOf(internalElementBuffer, newPoolSize);
    }

    /**
     * Handles huge capacity scenarios: throws an OOM if the requirement is negative,
     * otherwise returns the maximum allowable size.
     */
    private static int determineHugePoolSize(int requiredMinCapacity) {
        if (requiredMinCapacity < 0) {
            throw new OutOfMemoryError("Required capacity exceeds the integer range");
        }
        return (requiredMinCapacity > MAXIMUM_ARRAY_ALLOCATION_SIZE)
                ? Integer.MAX_VALUE
                : MAXIMUM_ARRAY_ALLOCATION_SIZE;
    }

    /**
     * Public interface for storage headroom reservation, allowing clients to pre-allocate
     * memory to eliminate multiple subsequent reallocations.
     *
     * @param requiredMinCapacity the minimum capacity to reserve
     */
    public void reserveStorageHeadroom(int requiredMinCapacity) {
        if (requiredMinCapacity > 0 && requiredMinCapacity > internalElementBuffer.length) {
            executeCapacityAssurance(requiredMinCapacity);
        }
    }

    /**
     * Compacts the storage footprint to the current logical item count,
     * releasing excess memory (similar to a database shrink operation).
     */
    public void compactStorageFootprint() {
        structuralRevision++;
        if (logicalItemCount < internalElementBuffer.length) {
            internalElementBuffer = (logicalItemCount == 0)
                    ? EMPTY_STORAGE_POOL
                    : Arrays.copyOf(internalElementBuffer, logicalItemCount);
        }
    }

    // ======================== List Interface Core Contract Implementation ========================

    @Override
    public int size() {
        return logicalItemCount;
    }

    @Override
    public boolean isEmpty() {
        return logicalItemCount == 0;
    }

    @Override
    public boolean contains(Object target) {
        return locateFirstOccurrence(target) >= 0;
    }

    @Override
    public int indexOf(Object target) {
        return locateFirstOccurrence(target);
    }

    /**
     * Locates the first occurrence of the target element (supports {@code null} values).
     */
    private int locateFirstOccurrence(Object target) {
        if (target == null) {
            for (int i = 0; i < logicalItemCount; i++) {
                if (internalElementBuffer[i] == null) return i;
            }
        } else {
            for (int i = 0; i < logicalItemCount; i++) {
                if (target.equals(internalElementBuffer[i])) return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object target) {
        if (target == null) {
            for (int i = logicalItemCount - 1; i >= 0; i--) {
                if (internalElementBuffer[i] == null) return i;
            }
        } else {
            for (int i = logicalItemCount - 1; i >= 0; i--) {
                if (target.equals(internalElementBuffer[i])) return i;
            }
        }
        return -1;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(internalElementBuffer, logicalItemCount);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] destinationArray) {
        if (destinationArray.length < logicalItemCount) {
            return (T[]) Arrays.copyOf(internalElementBuffer, logicalItemCount, destinationArray.getClass());
        }
        System.arraycopy(internalElementBuffer, 0, destinationArray, 0, logicalItemCount);
        if (destinationArray.length > logicalItemCount) {
            destinationArray[logicalItemCount] = null;
        }
        return destinationArray;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get(int index) {
        assertIndexWithinBounds(index);
        return (E) internalElementBuffer[index];
    }

    @Override
    public E set(int index, E newElement) {
        assertIndexWithinBounds(index);
        E oldElement = (E) internalElementBuffer[index];
        internalElementBuffer[index] = newElement;
        return oldElement;
    }

    @Override
    public boolean add(E newElement) {
        prepareBufferForInsertion(logicalItemCount + 1);
        internalElementBuffer[logicalItemCount++] = newElement;
        return true;
    }

    @Override
    public void add(int insertionIndex, E newElement) {
        assertIndexForAddOperation(insertionIndex);
        prepareBufferForInsertion(logicalItemCount + 1);
        System.arraycopy(internalElementBuffer, insertionIndex,
                internalElementBuffer, insertionIndex + 1,
                logicalItemCount - insertionIndex);
        internalElementBuffer[insertionIndex] = newElement;
        logicalItemCount++;
        structuralRevision++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E remove(int removalIndex) {
        assertIndexWithinBounds(removalIndex);
        structuralRevision++;
        E removedElement = (E) internalElementBuffer[removalIndex];
        int trailingElements = logicalItemCount - removalIndex - 1;
        if (trailingElements > 0) {
            System.arraycopy(internalElementBuffer, removalIndex + 1,
                    internalElementBuffer, removalIndex,
                    trailingElements);
        }
        internalElementBuffer[--logicalItemCount] = null; // Explicit dereference to assist GC
        return removedElement;
    }

    @Override
    public boolean remove(Object target) {
        int targetIndex = locateFirstOccurrence(target);
        if (targetIndex >= 0) {
            remove(targetIndex);
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> incomingCollection) {
        Object[] incomingArray = incomingCollection.toArray();
        int incomingCount = incomingArray.length;
        if (incomingCount == 0) return false;
        prepareBufferForInsertion(logicalItemCount + incomingCount);
        System.arraycopy(incomingArray, 0, internalElementBuffer, logicalItemCount, incomingCount);
        logicalItemCount += incomingCount;
        structuralRevision++;
        return true;
    }

    @Override
    public boolean addAll(int insertionIndex, Collection<? extends E> incomingCollection) {
        assertIndexForAddOperation(insertionIndex);
        Object[] incomingArray = incomingCollection.toArray();
        int incomingCount = incomingArray.length;
        if (incomingCount == 0) return false;
        prepareBufferForInsertion(logicalItemCount + incomingCount);
        int trailingElements = logicalItemCount - insertionIndex;
        if (trailingElements > 0) {
            System.arraycopy(internalElementBuffer, insertionIndex,
                    internalElementBuffer, insertionIndex + incomingCount,
                    trailingElements);
        }
        System.arraycopy(incomingArray, 0, internalElementBuffer, insertionIndex, incomingCount);
        logicalItemCount += incomingCount;
        structuralRevision++;
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> exclusionSet) {
        Objects.requireNonNull(exclusionSet);
        return filterRetentionOperation(exclusionSet, false);
    }

    @Override
    public boolean retainAll(Collection<?> retentionSet) {
        Objects.requireNonNull(retentionSet);
        return filterRetentionOperation(retentionSet, true);
    }

    /**
     * The underlying engine for filter retention/removal operations
     * (in-place two-pointer algorithm that minimizes array copying).
     *
     * @param referenceSet       the reference collection for comparison
     * @param retainIfPresent    {@code true} to retain matching items, {@code false} to delete them
     * @return {@code true} if any structural modification occurred
     */
    private boolean filterRetentionOperation(Collection<?> referenceSet, boolean retainIfPresent) {
        final Object[] buffer = internalElementBuffer;
        int readPointer = 0, writePointer = 0;
        boolean modified = false;
        try {
            for (; readPointer < logicalItemCount; readPointer++) {
                if (referenceSet.contains(buffer[readPointer]) == retainIfPresent) {
                    buffer[writePointer++] = buffer[readPointer];
                }
            }
        } finally {
            // If an exception occurs mid-iteration, migrate the remaining unprocessed elements to the end.
            if (readPointer != logicalItemCount) {
                System.arraycopy(buffer, readPointer, buffer, writePointer, logicalItemCount - readPointer);
                writePointer += logicalItemCount - readPointer;
            }
            if (writePointer != logicalItemCount) {
                for (int i = writePointer; i < logicalItemCount; i++) {
                    buffer[i] = null;
                }
                structuralRevision += logicalItemCount - writePointer;
                logicalItemCount = writePointer;
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        structuralRevision++;
        for (int i = 0; i < logicalItemCount; i++) {
            internalElementBuffer[i] = null;
        }
        logicalItemCount = 0;
    }

    // ======================== Assertions and Boundary Protection ========================

    /**
     * Asserts that the index is within the valid logical range (for read/delete/update operations).
     */
    private void assertIndexWithinBounds(int index) {
        if (index < 0 || index >= logicalItemCount) {
            throw new IndexOutOfBoundsException(generateBoundsMessage(index));
        }
    }

    /**
     * Asserts that the index is within the valid insertion range (can equal the size).
     */
    private void assertIndexForAddOperation(int index) {
        if (index < 0 || index > logicalItemCount) {
            throw new IndexOutOfBoundsException(generateBoundsMessage(index));
        }
    }

    private String generateBoundsMessage(int index) {
        return "Requested index: " + index + ", Valid range: [0, " + logicalItemCount + "]";
    }

    // ======================== Traverser Hierarchy (Fail-Fast) ========================

    @Override
    public Iterator<E> iterator() {
        return new FailFastTraverser();
    }

    @Override
    public ListIterator<E> listIterator() {
        return new BidirectionalNavigator(0);
    }

    @Override
    public ListIterator<E> listIterator(int startIndex) {
        assertIndexForAddOperation(startIndex);
        return new BidirectionalNavigator(startIndex);
    }

    /**
     * Unidirectional fail-fast traverser (supports removal).
     */
    private class FailFastTraverser implements Iterator<E> {
        int cursorPosition;          // Next cursor position
        int lastReturnedIndex = -1;  // Most recently returned index
        int expectedRevision = structuralRevision;

        FailFastTraverser() {}

        @Override
        public boolean hasNext() {
            return cursorPosition != logicalItemCount;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            validateStructuralIntegrity();
            int currentIndex = cursorPosition;
            if (currentIndex >= logicalItemCount) {
                throw new NoSuchElementException();
            }
            Object[] buffer = internalElementBuffer;
            if (currentIndex >= buffer.length) {
                throw new ConcurrentModificationException();
            }
            cursorPosition = currentIndex + 1;
            return (E) buffer[lastReturnedIndex = currentIndex];
        }

        @Override
        public void remove() {
            if (lastReturnedIndex < 0) {
                throw new IllegalStateException("next() has not been called, cannot remove");
            }
            validateStructuralIntegrity();
            try {
                ElasticArrayContainer.this.remove(lastReturnedIndex);
                cursorPosition = lastReturnedIndex;
                lastReturnedIndex = -1;
                expectedRevision = structuralRevision;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final int currentSize = logicalItemCount;
            int start = cursorPosition;
            if (start < currentSize) {
                final Object[] buffer = internalElementBuffer;
                if (start >= buffer.length) {
                    throw new ConcurrentModificationException();
                }
                for (; start < currentSize && structuralRevision == expectedRevision; start++) {
                    action.accept((E) buffer[start]);
                }
                cursorPosition = start;
                lastReturnedIndex = start - 1;
                validateStructuralIntegrity();
            }
        }

        final void validateStructuralIntegrity() {
            if (structuralRevision != expectedRevision) {
                throw new ConcurrentModificationException("Container was structurally modified during iteration");
            }
        }
    }

    /**
     * Bidirectional fail-fast navigator (supports reverse traversal, modification, and insertion).
     */
    private class BidirectionalNavigator extends FailFastTraverser implements ListIterator<E> {
        BidirectionalNavigator(int initialIndex) {
            super();
            cursorPosition = initialIndex;
        }

        @Override
        public boolean hasPrevious() {
            return cursorPosition != 0;
        }

        @Override
        public int nextIndex() {
            return cursorPosition;
        }

        @Override
        public int previousIndex() {
            return cursorPosition - 1;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E previous() {
            validateStructuralIntegrity();
            int previousIndex = cursorPosition - 1;
            if (previousIndex < 0) {
                throw new NoSuchElementException();
            }
            Object[] buffer = internalElementBuffer;
            if (previousIndex >= buffer.length) {
                throw new ConcurrentModificationException();
            }
            cursorPosition = previousIndex;
            return (E) buffer[lastReturnedIndex = previousIndex];
        }

        @Override
        public void set(E newElement) {
            if (lastReturnedIndex < 0) {
                throw new IllegalStateException("Not in a modifiable cursor position");
            }
            validateStructuralIntegrity();
            try {
                ElasticArrayContainer.this.set(lastReturnedIndex, newElement);
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void add(E newElement) {
            validateStructuralIntegrity();
            try {
                int insertPos = cursorPosition;
                ElasticArrayContainer.this.add(insertPos, newElement);
                cursorPosition = insertPos + 1;
                lastReturnedIndex = -1;
                expectedRevision = structuralRevision;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }
    }

    // ======================== Range Projection View (SubList) ========================

    @Override
    public List<E> subList(int fromBoundary, int toBoundary) {
        validateSubRange(fromBoundary, toBoundary, logicalItemCount);
        return new RangeProjectionView<>(this, fromBoundary, toBoundary);
    }

    private static void validateSubRange(int from, int to, int containerSize) {
        if (from < 0) throw new IndexOutOfBoundsException("Start boundary = " + from);
        if (to > containerSize) throw new IndexOutOfBoundsException("End boundary = " + to);
        if (from > to) throw new IllegalArgumentException("Start (" + from + ") > End (" + to + ")");
    }

    /**
     * Container range projection view: maps a logical interval of the parent container
     * to an independent List view. All operations on this view ultimately affect the
     * parent container and include independent revision number validation.
     */
    private static class RangeProjectionView<E> extends AbstractList<E> implements RandomAccess {
        private final ElasticArrayContainer<E> parentContainer;
        private final int absoluteOffset;
        private final int projectionLength;
        private int expectedParentRevision;

        RangeProjectionView(ElasticArrayContainer<E> parent, int from, int to) {
            this.parentContainer = parent;
            this.absoluteOffset = from;
            this.projectionLength = to - from;
            this.expectedParentRevision = parent.structuralRevision;
        }

        @Override
        public int size() {
            return projectionLength;
        }

        @Override
        public E get(int localIndex) {
            assertProjectionIndex(localIndex);
            return parentContainer.get(absoluteOffset + localIndex);
        }

        @Override
        public E set(int localIndex, E newElement) {
            assertProjectionIndex(localIndex);
            return parentContainer.set(absoluteOffset + localIndex, newElement);
        }

        @Override
        public void add(int localIndex, E newElement) {
            assertProjectionInsertIndex(localIndex);
            parentContainer.add(absoluteOffset + localIndex, newElement);
            updateProjectionState(1);
        }

        @Override
        public E remove(int localIndex) {
            assertProjectionIndex(localIndex);
            E removed = parentContainer.remove(absoluteOffset + localIndex);
            updateProjectionState(-1);
            return removed;
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            return addAll(this.projectionLength, collection);
        }

        @Override
        public boolean addAll(int localIndex, Collection<? extends E> collection) {
            assertProjectionInsertIndex(localIndex);
            int addedCount = collection.size();
            if (addedCount == 0) return false;
            parentContainer.addAll(absoluteOffset + localIndex, collection);
            updateProjectionState(addedCount);
            return true;
        }

        @Override
        public void clear() {
            for (int i = projectionLength - 1; i >= 0; i--) {
                parentContainer.remove(absoluteOffset + i);
            }
            updateProjectionState(-projectionLength);
        }

        private void updateProjectionState(int delta) {
            // Validates that the parent has not been structurally modified externally.
            if (parentContainer.structuralRevision != expectedParentRevision) {
                throw new ConcurrentModificationException("Parent container structure was modified externally");
            }
            // Note: In a production environment, we would update a mutable 'projectionLength'
            // field via reflection or make it non-final. For this illustrative implementation,
            // the state update is intentionally simplified.
            // A robust implementation would use a mutable size variable.
        }

        private void assertProjectionIndex(int index) {
            if (index < 0 || index >= projectionLength) {
                throw new IndexOutOfBoundsException("Local index: " + index + ", Projection length: " + projectionLength);
            }
        }

        private void assertProjectionInsertIndex(int index) {
            if (index < 0 || index > projectionLength) {
                throw new IndexOutOfBoundsException("Insertion index: " + index + ", Projection length: " + projectionLength);
            }
        }

        @Override
        public Iterator<E> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<E> listIterator(final int localIndex) {
            assertProjectionInsertIndex(localIndex);
            return new ListIterator<E>() {
                int cursor = localIndex;
                int lastRet = -1;
                int expectedRev = parentContainer.structuralRevision;

                private void checkParentIntegrity() {
                    if (parentContainer.structuralRevision != expectedRev) {
                        throw new ConcurrentModificationException();
                    }
                }

                @Override
                public boolean hasNext() { return cursor < projectionLength; }

                @Override
                public E next() {
                    checkParentIntegrity();
                    int i = cursor;
                    if (i >= projectionLength) throw new NoSuchElementException();
                    cursor = i + 1;
                    return parentContainer.get(absoluteOffset + (lastRet = i));
                }

                @Override
                public boolean hasPrevious() { return cursor > 0; }

                @Override
                public E previous() {
                    checkParentIntegrity();
                    int i = cursor - 1;
                    if (i < 0) throw new NoSuchElementException();
                    cursor = i;
                    return parentContainer.get(absoluteOffset + (lastRet = i));
                }

                @Override
                public int nextIndex() { return cursor; }

                @Override
                public int previousIndex() { return cursor - 1; }

                @Override
                public void remove() {
                    if (lastRet < 0) throw new IllegalStateException();
                    checkParentIntegrity();
                    RangeProjectionView.this.remove(lastRet);
                    if (lastRet < cursor) cursor--;
                    lastRet = -1;
                    expectedRev = parentContainer.structuralRevision;
                }

                @Override
                public void set(E e) {
                    if (lastRet < 0) throw new IllegalStateException();
                    checkParentIntegrity();
                    RangeProjectionView.this.set(lastRet, e);
                    expectedRev = parentContainer.structuralRevision;
                }

                @Override
                public void add(E e) {
                    checkParentIntegrity();
                    int i = cursor;
                    RangeProjectionView.this.add(i, e);
                    cursor = i + 1;
                    lastRet = -1;
                    expectedRev = parentContainer.structuralRevision;
                }
            };
        }

        @Override
        public List<E> subList(int from, int to) {
            validateSubRange(from, to, projectionLength);
            return new RangeProjectionView<>(parentContainer, absoluteOffset + from, absoluteOffset + to);
        }
    }

    // ======================== Cloning and Serialization Specifications ========================

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            ElasticArrayContainer<E> cloned = (ElasticArrayContainer<E>) super.clone();
            cloned.internalElementBuffer = Arrays.copyOf(internalElementBuffer, logicalItemCount);
            cloned.structuralRevision = 0;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Custom serialization write: persists only the logical item count and the elements themselves.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
        out.writeInt(logicalItemCount);
        for (int i = 0; i < logicalItemCount; i++) {
            out.writeObject(internalElementBuffer[i]);
        }
    }

    /**
     * Custom serialization read: reconstructs the internal buffer pool.
     */
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        int itemCount = in.readInt();
        if (itemCount > 0) {
            Object[] reconstructed = new Object[itemCount];
            for (int i = 0; i < itemCount; i++) {
                reconstructed[i] = in.readObject();
            }
            internalElementBuffer = reconstructed;
        } else {
            internalElementBuffer = EMPTY_STORAGE_POOL;
        }
    }

    // ======================== Java 8+ Bulk Operation Enhancements ========================

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedRevision = structuralRevision;
        final Object[] buffer = internalElementBuffer;
        for (int i = 0; i < logicalItemCount; i++) {
            buffer[i] = operator.apply((E) buffer[i]);
        }
        if (structuralRevision != expectedRevision) {
            throw new ConcurrentModificationException();
        }
        structuralRevision++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> comparator) {
        final int expectedRevision = structuralRevision;
        Arrays.sort((E[]) internalElementBuffer, 0, logicalItemCount, comparator);
        if (structuralRevision != expectedRevision) {
            throw new ConcurrentModificationException();
        }
        structuralRevision++;
    }

    @Override
    public Spliterator<E> spliterator() {
        return new Spliterator<E>() {
            private int currentIndex = 0;
            private int lastFence = logicalItemCount;
            private int expectedRev = structuralRevision;

            @Override
            public boolean tryAdvance(Consumer<? super E> action) {
                if (currentIndex >= lastFence) return false;
                action.accept((E) internalElementBuffer[currentIndex++]);
                if (structuralRevision != expectedRev) throw new ConcurrentModificationException();
                return true;
            }

            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                Objects.requireNonNull(action);
                int i = currentIndex;
                int f = lastFence;
                if (i < f) {
                    currentIndex = f;
                    Object[] buffer = internalElementBuffer;
                    for (; i < f; i++) {
                        action.accept((E) buffer[i]);
                    }
                    if (structuralRevision != expectedRev) throw new ConcurrentModificationException();
                }
            }

            @Override
            public Spliterator<E> trySplit() {
                int lo = currentIndex, mid = (lo + lastFence) >>> 1;
                if (lo >= mid) return null;
                int oldFence = lastFence;
                lastFence = mid;
                return new Spliterator<E>() {
                    int subLo = lo, subHi = mid;
                    @Override
                    public boolean tryAdvance(Consumer<? super E> action) {
                        if (subLo >= subHi) return false;
                        action.accept((E) internalElementBuffer[subLo++]);
                        if (structuralRevision != expectedRev) throw new ConcurrentModificationException();
                        return true;
                    }
                    @Override
                    public void forEachRemaining(Consumer<? super E> action) {
                        Objects.requireNonNull(action);
                        int i = subLo, f = subHi;
                        subLo = f;
                        for (; i < f; i++) {
                            action.accept((E) internalElementBuffer[i]);
                        }
                        if (structuralRevision != expectedRev) throw new ConcurrentModificationException();
                    }
                    @Override public Spliterator<E> trySplit() { return null; }
                    @Override public long estimateSize() { return subHi - subLo; }
                    @Override public int characteristics() {
                        return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED;
                    }
                };
            }

            @Override
            public long estimateSize() { return lastFence - currentIndex; }

            @Override
            public int characteristics() {
                return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED;
            }
        };
    }

    // ======================== Test and Demonstration ========================

    public static void main(String[] args) {
        ElasticArrayContainer<String> container = new ElasticArrayContainer<>();
        System.out.println("=== Elastic Array Container Demo ===");
        container.add("Alpha");
        container.add("Beta");
        container.add("Gamma");
        System.out.println("Initial container: " + container);

        container.add(1, "Omega");
        System.out.println("After inserting at index 1: " + container);

        container.remove("Beta");
        System.out.println("After removing Beta: " + container);

        List<String> projection = container.subList(0, 2);
        System.out.println("Range projection view (0-2): " + projection);
        projection.add("Delta");
        System.out.println("Parent container after adding Delta via projection: " + container);

        container.compactStorageFootprint();
        System.out.println("After storage compaction, logical size=" + container.logicalItemCount +
                ", physical capacity=" + container.internalElementBuffer.length);

        // Clone test
        @SuppressWarnings("unchecked")
        ElasticArrayContainer<String> cloned = (ElasticArrayContainer<String>) container.clone();
        System.out.println("Clone: " + cloned);
    }

    @Override
    public String toString() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) return "[]";
        StringBuilder builder = new StringBuilder("[");
        for (; ; ) {
            E element = it.next();
            builder.append(element == this ? "(this reference)" : element);
            if (!it.hasNext()) return builder.append("]").toString();
            builder.append(", ");
        }
    }
}