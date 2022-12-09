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
@ChosenImplementation(true)
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
        // Path along the tree to get to the relevant leaf
        // Used to fix parent nodes after split
        Stack<InnerNode> path = new Stack<>();

        // Find LeafNode into which the key has to be inserted.
        BPlusTreeNode<?> current = rootNode;

        // Traverse path along tree to responsible child node
        while(current instanceof InnerNode){
            InnerNode innerNode  = (InnerNode)current;
            path.push(innerNode);
            current = innerNode.selectChild(key);
        }
        LeafNode leafNode  = (LeafNode) current;

        // Overwrite a key, if it already exists in leaf node and return old value
        for(int i = 0; i < leafNode.keys.length; i++){
            if(leafNode.keys[i] == key){
                ValueReference oldValue = leafNode.references[i];
                leafNode.references[i] = value;
                return oldValue;
            }
        }

        // Insert into child and check, if we need to split
        LeafNode rightLeaf = insertIntoLeaf(leafNode, key, value);

        if(rightLeaf == null){
            return value;
        }

        InnerNode currentNode = null;

        do{
            if(currentNode != null){
                currentNode = insertIntoInner(path.pop(), currentNode);
            }else {
                currentNode = insertIntoInner(path.pop(), rightLeaf);
            }
        } while(currentNode != null);

        /*
        // New key - Is there still space?
        //   leafNode.keys[pos] = key;
        //   leafNode.references[pos] = value;
        //   Don't forget to update the parent keys and so on...
        int overflowKey = 0;
        ValueReference overflowValue = null;

            InnerNode parent = path.pop();
            rightNode = insertIntoInner(parent, rightNode);
        }*/
        return value;
    }

    LeafNode insertIntoLeaf(LeafNode leaf, Integer key, ValueReference value) {
        // Case 1: Still room!
        if (!leaf.isFull()) {
            // Insert by shifting values to the right, starting from back
            int index = getKeyIndex(key, leaf.keys);
            for (int i = leaf.keys.length - 1; i > index; i--) {
                leaf.keys[i] = leaf.keys[i - 1];
                leaf.references[i] = leaf.references[i - 1];
            }

            leaf.keys[index] = key;
            leaf.references[index] = value;

            // No new leaf needed!
            return null;
        }

        // Case 2: Not enough room!
        int firstRightIndex = leaf.keys.length / 2;
        if (getKeyIndex(key, leaf.keys) > firstRightIndex) {
            firstRightIndex++;
        }
        int rightCount = leaf.keys.length - firstRightIndex;
        Entry[] rightEntries = new Entry[rightCount];
        for (int i = 0; i < rightCount; i++) {
            int leftIndex = firstRightIndex + i;
            rightEntries[i] = new Entry(leaf.keys[leftIndex],leaf.references[leftIndex]);
            leaf.keys[leftIndex] = null;
            leaf.references[leftIndex] = null;
        }
        LeafNode rightLeaf = new LeafNode(leaf.order, rightEntries);
        // Definitely enough space for insertion in both
        if (key < rightLeaf.getSmallestKey()) {
            insertIntoLeaf(leaf, key, value);
        } else {
            insertIntoLeaf(rightLeaf, key, value);
        }

        rightLeaf.nextSibling = leaf.nextSibling;
        leaf.nextSibling = rightLeaf;
        if(rootNode instanceof LeafNode){
            rootNode = new InnerNode(leaf.order, leaf, rightLeaf);
            return null;
        }
        return rightLeaf;
    }

    InnerNode insertIntoInner(InnerNode node, BPlusTreeNode<?> child) {
        Integer key = child.getSmallestKey();
        if(child instanceof InnerNode){
            child.keys[getLargestIndex(child.keys)] = null;
        }
        // Case 1: Still room!
        if (!node.isFull()) {
            // Insert by shifting values to the right, starting from back
            int index = getKeyIndex(key, node.keys);
            for (int i = node.keys.length - 1; i > index; i--) {
                node.keys[i] = node.keys[i - 1];
                node.references[i] = node.references[i - 1];
            }

            node.keys[index] = key;
            node.references[index] = child;

            // No new node needed!
            return null;
        }

        // Case 2: Not enough room!
        int firstRightIndex = node.keys.length / 2;
        if (getKeyIndex(key, node.keys) > firstRightIndex) {
            firstRightIndex++;
        }
        int rightCount = node.keys.length - firstRightIndex;
        BPlusTreeNode<?>[] rightChildren = new BPlusTreeNode<?>[rightCount];
        for (int i = 0; i < rightCount; i++) {
            int leftIndex = firstRightIndex + i;
            rightChildren[i] = node.references[leftIndex];
            node.keys[leftIndex] = null;
            node.references[leftIndex] = null;
        }
        InnerNode rightNode = new InnerNode(node.order, rightChildren);
        // Definitely enough space for insertion in both
        if (key < rightNode.getSmallestKey()) {
            insertIntoInner(node, child);
        } else {
            insertIntoInner(rightNode, child);
        }
        return node;
    }

    // Get index where this key should be inserted at
    int getKeyIndex(Integer key, Integer[] keys) {
        int index = 0;
        while (index < keys.length && keys[index] != null && keys[index] < key) {
            index++;
        }
        return index;
    }

    int getLargestIndex(Integer[] keys) {
        int index = 0;
        while (keys[index] != null) {
            index++;
        }
        return index;
    }
}

