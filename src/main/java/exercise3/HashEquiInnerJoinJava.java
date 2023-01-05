package exercise3;

import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.dbms.*;
import de.hpi.dbs2.exercise3.InnerJoinOperation;
import de.hpi.dbs2.exercise3.JoinAttributePair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Consumer;

@ChosenImplementation(true)
public class HashEquiInnerJoinJava extends InnerJoinOperation {

	public HashEquiInnerJoinJava(
		@NotNull BlockManager blockManager, int leftColumnIndex, int rightColumnIndex
	) {
		super(blockManager, new JoinAttributePair.EquiJoinAttributePair(leftColumnIndex, rightColumnIndex));
	}

	@Override
	public int estimatedIOCost(
		@NotNull Relation leftInputRelation, @NotNull Relation rightInputRelation
	) {
		return (leftInputRelation.estimatedBlockCount() + rightInputRelation.estimatedBlockCount()) * 3;
	}

	@Override
	public void join(
		@NotNull Relation leftInputRelation, @NotNull Relation rightInputRelation,
		@NotNull Relation outputRelation
	) {
		// TODO:
		//  - calculate a sensible bucket count
		int bucketCount = getBlockManager().getFreeBlocks() - 1;

		// Test wether this join can be executed min(B(S),B(R)) < (M-1)^2
		int minimumSize = Math.min(leftInputRelation.estimatedBlockCount(), rightInputRelation.estimatedBlockCount());
		if (minimumSize >= bucketCount * bucketCount) {
			throw new RelationSizeExceedsCapacityException();
		}

		// TODO:
		//  - hash relation
		Bucket[] leftBuckets = hashRelation(leftInputRelation, bucketCount);
		Bucket[] rightBuckets = hashRelation(rightInputRelation, bucketCount);

		// TODO:
		//  - join hashed blocks
		TupleAppender tupleAppender = new TupleAppender(outputRelation.getBlockOutput());
		for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
			joinBuckets(leftBuckets[bucketIndex], rightBuckets[bucketIndex],outputRelation, tupleAppender);
		}
	}

	private void joinBuckets(Bucket leftBucket, Bucket rightBucket,  Relation outputRelation, TupleAppender tupleAppender) {
		Block[] leftBlocks = leftBucket.load();
		for (int i = 0; i < rightBucket.size(); i++) {
			Block rightBlock = rightBucket.load(i);
			for (Block leftBlock : leftBlocks) {
					joinBlocks(
							leftBlock,
							rightBlock,
							outputRelation.getColumns(),
							tupleAppender
					);
			}
			getBlockManager().release(rightBlock, false);
		}
		for (int i =0; i < leftBucket.size(); i++) {
			getBlockManager().release(leftBlocks[i], false);
		}
	}

	private Bucket[] hashRelation(@NotNull Relation relation, int bucketCount) {
		Bucket buckets[] = new Bucket[bucketCount];
		for (int i = 0; i < bucketCount; i++) {
			buckets[i] = new Bucket();
		}
		for (Block blockRef : relation) {
			Block block = getBlockManager().load(blockRef);
			for (Tuple tuple : block) {
				int hash = tuple.get(getJoinAttributePair().getLeftColumnIndex()).hashCode();
				int bucketIndex = Math.floorMod(hash, bucketCount); // hash % bucketCount
				// Insert into bucket
				buckets[bucketIndex].add(tuple);
			}
			getBlockManager().release(block, false);
		}
		// Release all buckets to disk
		for (Bucket bucket : buckets) {
			bucket.release();
		}
		return buckets;
	}


	class TupleAppender implements AutoCloseable, Consumer<Tuple> {

		BlockOutput blockOutput;

		TupleAppender(BlockOutput blockOutput) {
			this.blockOutput = blockOutput;
		}

		Block outputBlock = getBlockManager().allocate(true);

		@Override
		public void accept(Tuple tuple) {
			if(outputBlock.isFull()) {
				blockOutput.move(outputBlock);
				outputBlock = getBlockManager().allocate(true);
			}
			outputBlock.append(tuple);
		}

		@Override
		public void close() {
			if(!outputBlock.isEmpty()) {
				blockOutput.move(outputBlock);
			} else {
				getBlockManager().release(outputBlock, false);
			}
		}
	}

	class Bucket {

		Block currentBlock = getBlockManager().allocate(true);
		ArrayList<Block> blocks = new ArrayList<>();

		public void add(Tuple tuple) {
			if(currentBlock.isFull()) {
				// Save to disk and remember block
				blocks.add(getBlockManager().release(currentBlock, true));
				currentBlock = getBlockManager().allocate(true);
			}
			currentBlock.append(tuple);
		}

		public void release() {
			if(!currentBlock.isEmpty()) {
				blocks.add(getBlockManager().release(currentBlock, true));
			} else {
				getBlockManager().release(currentBlock, false);
			}
		}

		public Block[] load()  {
			Block[] loaded = new Block[blocks.size()];
			for (int i = 0; i < blocks.size(); i++) {
				loaded[i] = getBlockManager().load(blocks.get(i));
			}
			return loaded;
		}

		public Block load(int index) {
			return getBlockManager().load(blocks.get(index));
		}

		public int size() {
			return blocks.size();
		}
	};
}
