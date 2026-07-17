import java.util.Iterator;
import java.util.List;

/**
 * Enterprise-grade file system simulation engine demonstrating the advanced capabilities
 * of {@link ElasticArrayContainer}, including dynamic resizing, fail-fast iterators,
 * range projection views, and recursive tree traversal algorithms.
 * <p>
 * This simulation models a hierarchical storage system composed of directories and files.
 * Each directory maintains its child nodes (subdirectories or files) within an
 * {@link ElasticArrayContainer}, showcasing the container's utility in nested data structures
 * with dynamic growth and efficient random access.
 * </p>
 */
public class FileSystemSimulation {

    // -------------------- Abstract Node Definition --------------------

    /**
     * Abstract base class representing a node within the file system hierarchy.
     */
    static abstract class FileSystemNode {
        protected final String nodeName;

        public FileSystemNode(String name) {
            this.nodeName = name;
        }

        public String getName() {
            return nodeName;
        }

        /**
         * Computes the total storage space occupied by this node (in bytes).
         * For a file, returns its own size; for a directory, recursively sums
         * the sizes of all child nodes.
         *
         * @return total size in bytes
         */
        public abstract long computeTotalStorageConsumption();

        /**
         * Determines the depth of this node within the tree (root depth = 0).
         * Implemented recursively in directories by adding 1 to the maximum
         * child depth.
         *
         * @return depth level
         */
        public abstract int determineDepth();

        /**
         * Renders the node and its descendants as a hierarchical tree structure.
         *
         * @param indentation prefix string for visual hierarchy
         */
        public abstract void renderTree(String indentation);

        @Override
        public String toString() {
            return nodeName + " (storage=" + computeTotalStorageConsumption() + " bytes)";
        }
    }

    /**
     * File node — a leaf node containing a fixed size.
     */
    static class FileNode extends FileSystemNode {
        private final long fileSize; // size in bytes

        public FileNode(String name, long size) {
            super(name);
            this.fileSize = size;
        }

        @Override
        public long computeTotalStorageConsumption() {
            return fileSize;
        }

        @Override
        public int determineDepth() {
            return 0; // leaf node has no children
        }

        @Override
        public void renderTree(String indentation) {
            System.out.println(indentation + "📄 " + nodeName + " (" + fileSize + " bytes)");
        }
    }

    /**
     * Directory node — an internal node that maintains a dynamic list of child nodes
     * using an {@link ElasticArrayContainer}.
     */
    static class DirectoryNode extends FileSystemNode {
        // Core storage: ElasticArrayContainer for child nodes (auto-resizing, random access)
        private final ElasticArrayContainer<FileSystemNode> childNodes = new ElasticArrayContainer<>();

        public DirectoryNode(String name) {
            super(name);
        }

        /**
         * Appends a child node to this directory.
         *
         * @param child the child node (file or subdirectory)
         * @return this directory (facilitates fluent builder pattern)
         */
        public DirectoryNode attachChild(FileSystemNode child) {
            childNodes.add(child);
            return this;
        }

        /**
         * Returns a read-only projection view of the child nodes (via subList).
         */
        public List<FileSystemNode> acquireReadOnlyChildView() {
            return childNodes.subList(0, childNodes.size());
        }

        /**
         * Recursively computes the total storage consumption:
         * sum of all child nodes' sizes.
         */
        @Override
        public long computeTotalStorageConsumption() {
            long aggregate = 0;
            for (FileSystemNode child : childNodes) {
                aggregate += child.computeTotalStorageConsumption();
            }
            return aggregate;
        }

        /**
         * Recursively determines the depth of this directory:
         * 1 + maximum depth among children; if no children, depth = 1.
         */
        @Override
        public int determineDepth() {
            if (childNodes.isEmpty()) {
                return 1;
            }
            int maxChildDepth = 0;
            for (FileSystemNode child : childNodes) {
                int childDepth = child.determineDepth();
                if (childDepth > maxChildDepth) {
                    maxChildDepth = childDepth;
                }
            }
            return 1 + maxChildDepth;
        }

        /**
         * Recursively prints the directory tree with proper indentation.
         */
        @Override
        public void renderTree(String indentation) {
            System.out.println(indentation + "📁 " + nodeName + " (total: " + computeTotalStorageConsumption() + " bytes)");
            String childIndent = indentation + "  ";
            Iterator<FileSystemNode> iterator = childNodes.iterator();
            while (iterator.hasNext()) {
                FileSystemNode child = iterator.next();
                child.renderTree(childIndent);
            }
        }

        /**
         * Demonstrates sorting capability: sorts child nodes by name lexicographically.
         */
        public void sortChildNodesByNaming() {
            childNodes.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            System.out.println("📋 Child nodes have been sorted by name.");
        }

        /**
         * Demonstrates the replaceAll operation: transforms all file names to uppercase.
         */
        public void transformFileNamesToUpperCase() {
            childNodes.replaceAll(node -> {
                if (node instanceof FileNode) {
                    // Replace with a new FileNode preserving size (immutable design)
                    return new FileNode(node.getName().toUpperCase(), ((FileNode) node).fileSize);
                }
                return node; // directories remain unchanged
            });
            System.out.println("🔠 All file names have been converted to uppercase.");
        }
    }

    // -------------------- Recursive Utility Methods --------------------

    /**
     * Recursively searches for nodes whose names contain the given keyword,
     * and collects them into the provided result container.
     *
     * @param root      root directory of the search scope
     * @param keyword   search keyword (case-sensitive)
     * @param results   container to accumulate matching nodes (demonstrates container as parameter)
     */
    public static void recursiveKeywordSearch(DirectoryNode root, String keyword,
                                              ElasticArrayContainer<FileSystemNode> results) {
        // Check the root directory itself
        if (root.getName().contains(keyword)) {
            results.add(root);
        }
        // Recurse into all child nodes
        for (FileSystemNode child : root.childNodes) {
            if (child instanceof DirectoryNode) {
                recursiveKeywordSearch((DirectoryNode) child, keyword, results);
            } else if (child instanceof FileNode) {
                if (child.getName().contains(keyword)) {
                    results.add(child);
                }
            }
        }
    }

    /**
     * Convenience wrapper to compute total storage of the entire file system.
     */
    public static long computeTotalStorageConsumption(DirectoryNode root) {
        return root.computeTotalStorageConsumption();
    }

    // -------------------- Main Demonstration --------------------

    public static void main(String[] args) {
        System.out.println("=== File System Simulation Demo ===");
        System.out.println("Building a sample directory tree...\n");

        // Fluent construction of the directory tree
        DirectoryNode root = new DirectoryNode("root");

        DirectoryNode docs = new DirectoryNode("Documents");
        docs.attachChild(new FileNode("Resume.docx", 120_000))
                .attachChild(new FileNode("Budget.xlsx", 85_000))
                .attachChild(new FileNode("Notes.txt", 2_500));

        DirectoryNode pics = new DirectoryNode("Pictures");
        pics.attachChild(new FileNode("Vacation.jpg", 2_400_000))
                .attachChild(new FileNode("Family.png", 3_100_000));

        DirectoryNode music = new DirectoryNode("Music");
        music.attachChild(new FileNode("Song1.mp3", 5_000_000))
                .attachChild(new FileNode("Song2.mp3", 6_200_000));

        root.attachChild(docs)
                .attachChild(pics)
                .attachChild(music)
                .attachChild(new FileNode("readme.txt", 1_000));

        // Trigger dynamic resizing by adding a large number of files to one directory
        System.out.println("Adding numerous temporary files to Documents to trigger automatic expansion...");
        for (int i = 0; i < 25; i++) {
            docs.attachChild(new FileNode("temp_file_" + i + ".log", 512));
        }
        System.out.println("Documents now contains " + docs.childNodes.size() + " child items.\n");

        // 1. Recursive tree rendering
        System.out.println("--- Recursive Tree Rendering ---");
        root.renderTree("");

        // 2. Recursive total size calculation
        System.out.println("\n--- Recursive Storage Consumption ---");
        long total = computeTotalStorageConsumption(root);
        System.out.println("Total file system size: " + total + " bytes (" + (total / 1024) + " KB)");

        // 3. Recursive search for nodes containing "Song"
        System.out.println("\n--- Recursive Keyword Search ('Song') ---");
        ElasticArrayContainer<FileSystemNode> searchResults = new ElasticArrayContainer<>();
        recursiveKeywordSearch(root, "Song", searchResults);
        System.out.println("Found " + searchResults.size() + " matching node(s):");
        for (FileSystemNode node : searchResults) {
            System.out.println("  " + node);
        }

        // 4. Sub-list view demonstration
        System.out.println("\n--- Range Projection View (first 2 children of Pictures) ---");
        List<FileSystemNode> subView = pics.acquireReadOnlyChildView().subList(0, Math.min(2, pics.childNodes.size()));
        System.out.println("First two children of Pictures:");
        for (FileSystemNode node : subView) {
            System.out.println("  " + node.getName());
        }

        // 5. Sorting and transformation
        System.out.println("\n--- Sorting and Transformation ---");
        docs.sortChildNodesByNaming();
        docs.transformFileNamesToUpperCase();
        System.out.println("Documents directory after sorting and uppercase conversion:");
        docs.renderTree("  ");

        // 6. Storage compaction and cloning
        System.out.println("\n--- Storage Compaction and Cloning ---");
        DirectoryNode archive = new DirectoryNode("Archive");
        for (int i = 0; i < 100; i++) {
            archive.attachChild(new FileNode("arch_" + i + ".zip", 1024));
        }
        System.out.println("Archive child count: " + archive.childNodes.size() +
                ", current physical capacity: " + archive.childNodes.internalElementBuffer.length);
        archive.childNodes.compactStorageFootprint();
        System.out.println("After compaction, physical capacity: " + archive.childNodes.internalElementBuffer.length);

        @SuppressWarnings("unchecked")
        ElasticArrayContainer<FileSystemNode> clonedChildren =
                (ElasticArrayContainer<FileSystemNode>) archive.childNodes.clone();
        System.out.println("Cloned child container size: " + clonedChildren.size());

        System.out.println("\n=== Demonstration Complete ===");
    }
}