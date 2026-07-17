import java.util.*;
import java.util.function.Function;
import java.lang.reflect.*;

/**
 * Complex & Advanced Generics Demonstration
 * Covers: type bounds, wildcards (PECS), type-safe heterogeneous container,
 * generic method inference, builder pattern, bridge methods, type erasure and reflection workarounds.
 */
public class GenericsDemo {

    // ==================== 1. Generic class with type bounds ====================
    static class Box<T extends Number & Comparable<T>> {
        private T content;

        public Box(T content) {
            this.content = content;
        }

        public T getContent() { return content; }

        // Bounds allow calling Number methods and Comparable.compareTo
        public boolean greaterThan(Box<T> other) {
            return content.compareTo(other.content) > 0;
        }

        public double doubleValue() {
            return content.doubleValue();
        }
    }

    // ==================== 2. Wildcards – PECS principle ====================
    // Producer Extends, Consumer Super
    static class CollectionUtils {
        // Producer: reads from the collection, use ? extends T
        public static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp) {
            if (coll.isEmpty()) throw new NoSuchElementException();
            Iterator<? extends T> it = coll.iterator();
            T max = it.next();
            while (it.hasNext()) {
                T next = it.next();
                if (comp.compare(next, max) > 0) {
                    max = next;
                }
            }
            return max;
        }

        // Consumer: writes into the collection, use ? super T
        public static <T> void copy(List<? super T> dest, List<? extends T> src) {
            for (T item : src) {
                dest.add(item);
            }
        }
    }

    // ==================== 3. Type-safe heterogeneous container ====================
    static class Favorites {
        private Map<Class<?>, Object> favorites = new HashMap<>();

        public <T> void put(Class<T> type, T instance) {
            favorites.put(Objects.requireNonNull(type), type.cast(instance));
        }

        public <T> T get(Class<T> type) {
            return type.cast(favorites.get(type));
        }

        // Supports generic lists (uses List internally to avoid creating a generic array)
        public <T> List<T> getList(Class<T> type) {
            // For demonstration; in practice you need extra type info
            return (List<T>) favorites.get(type);
        }
    }

    // ==================== 4. Generic methods and type inference ====================
    static class TypeInference {
        // Uses target type inference, e.g. Collections.emptyList()
        public static <T> List<T> emptyList() {
            return new ArrayList<>();
        }

        // Explicit type arguments can be given
        public static <T> T identity(T t) {
            return t;
        }

        // Complex inference: multiple type parameters constraining each other
        public static <T, R> R apply(T t, Function<T, R> func) {
            return func.apply(t);
        }
    }

    // ==================== 5. Fluent Builder with generics ====================
    static class Person {
        private String name;
        private int age;
        private String email;

        private Person(Builder builder) {
            this.name = builder.name;
            this.age = builder.age;
            this.email = builder.email;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private int age;
            private String email;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder age(int age) {
                this.age = age;
                return this;
            }

            public Builder email(String email) {
                this.email = email;
                return this;
            }

            public Person build() {
                return new Person(this);
            }
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + ", email='" + email + "'}";
        }
    }

    // ==================== 6. Bridge methods (shown via reflection) ====================
    // Implements Comparable; due to erasure, the compiler generates a bridge method
    static class MyNumber implements Comparable<MyNumber> {
        private int value;

        public MyNumber(int value) { this.value = value; }

        @Override
        public int compareTo(MyNumber o) {
            return Integer.compare(this.value, o.value);
        }

        // Print all bridge methods via reflection
        public static void printBridgeMethods() {
            for (Method m : MyNumber.class.getDeclaredMethods()) {
                if (m.isBridge()) {
                    System.out.println("Bridge method: " + m);
                }
            }
        }
    }

    // ==================== 7. Generics and exceptions (cannot throw generic, but can catch) ====================
    static class GenericException<T extends Exception> {
        // Cannot throw a generic instance, but can catch one
        public void doWork() throws T { // allowed to throw generic
            // actual throw must use a concrete type or a factory
        }

        // Factory pattern to throw generic exception
        public static <E extends Exception> void throwException(E exception) throws E {
            throw exception;
        }
    }

    // ==================== 8. Generic arrays (not recommended, but shows safe creation) ====================
    static class GenericArray<T> {
        private T[] array;

        @SuppressWarnings("unchecked")
        public GenericArray(Class<T> clazz, int size) {
            // Safe way: use Array.newInstance
            array = (T[]) Array.newInstance(clazz, size);
        }

        public void set(int index, T value) {
            array[index] = value;
        }

        public T get(int index) {
            return array[index];
        }
    }

    // ==================== 9. Generics and reflection: get runtime generic type via superclass ====================
    static abstract class TypeRef<T> {
        private final Type type;

        protected TypeRef() {
            // Get the actual type argument of the subclass
            Type superclass = getClass().getGenericSuperclass();
            if (superclass instanceof ParameterizedType) {
                this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
            } else {
                this.type = Object.class;
            }
        }

        public Type getType() {
            return type;
        }

        @SuppressWarnings("unchecked")
        public Class<T> getRawType() {
            if (type instanceof Class) {
                return (Class<T>) type;
            } else if (type instanceof ParameterizedType) {
                return (Class<T>) ((ParameterizedType) type).getRawType();
            }
            return (Class<T>) Object.class;
        }
    }

    // ==================== 10. Main demonstration ====================
    public static void main(String[] args) throws Exception {
        // 1. Bounded generic class
        Box<Integer> intBox = new Box<>(10);
        Box<Integer> intBox2 = new Box<>(20);
        System.out.println("intBox.greaterThan(intBox2)? " + intBox.greaterThan(intBox2)); // false

        // 2. PECS
        List<Integer> ints = Arrays.asList(1, 2, 3, 4);
        Comparator<Number> numComp = (a, b) -> Double.compare(a.doubleValue(), b.doubleValue());
        Number maxNum = CollectionUtils.max(ints, numComp);
        System.out.println("Max: " + maxNum); // 4

        List<Number> nums = new ArrayList<>();
        CollectionUtils.copy(nums, ints); // copies Integer into Number list
        System.out.println("Copied: " + nums);

        // 3. Heterogeneous container
        Favorites f = new Favorites();
        f.put(String.class, "Hello");
        f.put(Integer.class, 42);
        f.put(List.class, Arrays.asList("a", "b"));
        System.out.println("Favorite String: " + f.get(String.class));
        System.out.println("Favorite Integer: " + f.get(Integer.class));

        // 4. Type inference
        List<String> empty = TypeInference.emptyList(); // inferred as String
        String id = TypeInference.identity("test");
        Integer len = TypeInference.apply("hello", String::length);
        System.out.println("Identity: " + id + ", length: " + len);

        // 5. Builder
        Person person = Person.builder()
                .name("Alice")
                .age(30)
                .email("alice@example.com")
                .build();
        System.out.println(person);

        // 6. Bridge methods
        MyNumber.printBridgeMethods();

        // 7. Generic exception
        try {
            GenericException.throwException(new IllegalArgumentException("test"));
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e);
        }

        // 8. Generic array
        GenericArray<String> strArray = new GenericArray<>(String.class, 3);
        strArray.set(0, "A");
        strArray.set(1, "B");
        System.out.println("Array[0] = " + strArray.get(0));

        // 9. Runtime type retrieval
        TypeRef<List<String>> ref = new TypeRef<List<String>>() {};
        System.out.println("TypeRef raw type: " + ref.getRawType()); // interface java.util.List
        System.out.println("TypeRef full type: " + ref.getType());   // java.util.List<java.lang.String>
    }
}