package exercise2;

import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.exercise2.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

/**
 * This is the B+-Tree implementation you will work on.
 * Your task is to implement the insert-operation.
 *
 */
@ChosenImplementation(false)
public class BPlusTreeJava extends AbstractBPlusTree {
    public BPlusTreeJava(int order) {
        super(order);
    }

    public BPlusTreeJava(BPlusTreeNode<?> rootNode) {
        super(rootNode);
    }

    @Nullable
    @Override
    public ValueReference insert(@NotNull Integer key, @NotNull ValueReference value) {
        // Find LeafNode in which the key has to be inserted.
        //   It is a good idea to track the "path" to the LeafNode in a Stack or something alike.

        Stack<InnerNode> path = new Stack<>();
        BPlusTreeNode<?> node = rootNode;
        while (node instanceof InnerNode) {
            // Find appropriate child node
            InnerNode asInnerNode = (InnerNode) node;
            path.push(asInnerNode);
            node = asInnerNode.selectChild(key);
        }

        // Does the key already exist? Overwrite!
        //   leafNode.references[pos] = value;
        //   But remember return the old value!

        // node is LeafNode now
        LeafNode leaf = (LeafNode) node;
        // Find position and overwrite, if possible
        for (int i = 0; i < leaf.keys.length; i++) {
            if (leaf.keys[i].equals(key)) {
                ValueReference oldValue = leaf.references[i];
                leaf.references[i] = value;
                return oldValue;
            }
        }

        // New key - Is there still space?
        //   leafNode.keys[pos] = key;
        //   leafNode.references[pos] = value;
        //   Don't forget to update the parent keys and so on...
        if (!leaf.isFull()) {
            // Find index of new key
            int index = 0;
            while (leaf.keys[index] <= key) {
                index++;
            }
            // Shift everything right of that once slot over
            for (int i = leaf.keys.length - 1; i > index; i--) {
                leaf.keys[i] = leaf.keys[i - 1];
                leaf.references[i] = leaf.references[i - 1];
            }

            // Insert new value
            leaf.keys[index] = key;
            leaf.references[index] = value;

            return value;
        }

        // Otherwise
        //   Split the LeafNode in two!

        //   Is parent node root?
        //     update rootNode = ... // will have only one key
        //   Was node instanceof LeafNode?
        //     update parentNode.keys[?] = ...
        //   Don't forget to update the parent keys and so on...

        // Check out the exercise slides for a flow chart of this logic.
        // If you feel stuck, try to draw what you want to do and
        // check out Ex2Main for playing around with the tree by e.g. printing or debugging it.
        // Also check out all the methods on BPlusTreeNode and how they are implemented or
        // the tests in BPlusTreeNodeTests and BPlusTreeTests!
    }

    // Insert into leaf node
    // If space: do nothing
    // If not: split, insert, return right child
    LeafNode insert(LeafNode leaf, Integer key, ValueReference value) {
        if (!leaf.isFull()) {
            // Find index of new key
            int index = 0;
            while (leaf.keys[index] <= key) {
                index++;
            }
            // Shift everything right of that once slot over
            for (int i = leaf.keys.length - 1; i > index; i--) {
                leaf.keys[i] = leaf.keys[i - 1];
                leaf.references[i] = leaf.references[i - 1];
            }

            // Insert new value
            leaf.keys[index] = key;
            leaf.references[index] = value;

            // No new node created, no right child
            return null;
        }

        // Need to split
        int halfSize = leaf.keys.length / 2;
        int leftCount = halfSize;
        int rightCount = leaf.n - leftCount;

        Entry[] entries = new Entry[rightCount];
        for (int i = 0; i < rightCount; i++) {
            int readIndex = leftCount + i;
            int writeIndex = i;

            Entry entry = new Entry(leaf.keys[readIndex], leaf.references[readIndex]);
            entries[i] = entry;
            leaf.keys[readIndex] = null;
            leaf.references[readIndex] = null;
        }

        LeafNode rightLeaf = new LeafNode(leaf.order, entries);

        // Fix up neighbors
        rightLeaf.nextSibling = leaf.nextSibling;
        leaf.nextSibling = rightLeaf;

        // Actually insert value
        // Just split, must have space
        if (key < rightLeaf.getSmallestKey()) {
            insert(leaf, key, value);
        } else {
            insert(rightLeaf, key, value);
        }

        // Signal split to caller
        return rightLeaf;
    }

    InnerNode insert(InnerNode node, int key, BPlusTreeNode<?> child) {
        if (!node.isFull()) {
            // Find index of new reference
            int index = 0;
            while (node.keys[index] <= key) {
                // TODO: Need to find the correct reference to insert into
                // Should we pass along the insert key?
                index++;
            }
            // Shift everything right of that once slot over
            for (int i = node.keys.length - 1; i > index; i--) {
                node.keys[i] = node.keys[i - 1];
                node.references[i] = node.references[i - 1];
            }

            // Insert new value
            node.keys[index] = key;
            node.references[index] = value;

            // No new node created, no right child
            return null;
        }

        // Need to split
        int halfSize = leaf.keys.length / 2;
        int leftCount = halfSize;
        int rightCount = leaf.n - leftCount;

        Entry[] entries = new Entry[rightCount];
        for (int i = 0; i < rightCount; i++) {
            int readIndex = leftCount + i;
            int writeIndex = i;

            Entry entry = new Entry(leaf.keys[readIndex], leaf.references[readIndex]);
            entries[i] = entry;
            leaf.keys[readIndex] = null;
            leaf.references[readIndex] = null;
        }

        LeafNode rightLeaf = new LeafNode(leaf.order, entries);

        // Fix up neighbors
        rightLeaf.nextSibling = leaf.nextSibling;
        leaf.nextSibling = rightLeaf;

        // Actually insert value
        // Just split, must have space
        if (key < rightLeaf.getSmallestKey()) {
            insert(leaf, key, value);
        } else {
            insert(rightLeaf, key, value);
        }

        // Signal split to caller
        return rightLeaf;
    }
}

