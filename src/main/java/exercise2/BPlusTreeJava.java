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
        //   It is a good idea to track the "path" to the LeafNode in a Stack or something alike.
        Stack<InnerNode> path = new Stack<>();

        // Find LeafNode in which the key has to be inserted.
        BPlusTreeNode<?> current = rootNode;

        while(current.references.length == current.order){
            InnerNode innerNode  = (InnerNode)current;
            path.push(innerNode);
            current = innerNode.selectChild(key);
        }
        LeafNode leafNode  = (LeafNode) current;

        // Does the key already exist? Overwrite!
        //   leafNode.references[pos] = value;
        //   But remember return the old value!

        for(int i = 0; i < leafNode.keys.length; i++){
            if(leafNode.keys[i] == key){
                ValueReference oldValue = leafNode.references[i];
                leafNode.references[i] = value;
                return oldValue;
            }
        }

        // New key - Is there still space?
        //   leafNode.keys[pos] = key;
        //   leafNode.references[pos] = value;
        //   Don't forget to update the parent keys and so on...

        for(int i = 0; i < leafNode.keys.length; i++){
            // insertion sort from back
            if(leafNode.keys[i] == null){
                for (int j = i - 1; leafNode.keys[j] > key; j--){
                    leafNode.keys[i] = leafNode.keys[j];
                    i--;
                }
                leafNode.keys[i] = key;
                leafNode.references[i] = value;
                return value;
            }
        }

        // Otherwise
        //   Split the LeafNode in two!

        int halfCount = leafNode.n/2;
        Entry[] rightEntries = new AbstractBPlusTree.Entry[halfCount];
        int middleIndex = halfCount + leafNode.n % 2;
        for (int i = 0; i < halfCount; i++) {
            rightEntries[i] = new Entry(leafNode.keys[i + middleIndex], leafNode.references[i + middleIndex]);
            leafNode.keys[i + middleIndex] = null;
            leafNode.references[i + middleIndex] = null;
        }
        LeafNode rightLeafNode = new LeafNode(leafNode.order, rightEntries);
        rightLeafNode.nextSibling = leafNode.nextSibling;
        leafNode.nextSibling = rightLeafNode;
        rightLeafNode.keys[halfCount] = key;
        rightLeafNode.references[halfCount] = value;

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
        return value;
    }

    BPlusTreeNode<?> addChild(InnerNode parent, BPlusTreeNode<?> child){

    }
}

