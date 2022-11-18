package exercise1;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.dbms.*;
import de.hpi.dbs2.dbms.utils.BlockSorter;
import de.hpi.dbs2.exercise1.SortOperation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

@ChosenImplementation(true)
public class TPMMSJava extends SortOperation {
    public TPMMSJava(@NotNull BlockManager manager, int sortColumnIndex) {
        super(manager, sortColumnIndex);
    }

    @Override
    public int estimatedIOCost(@NotNull Relation relation) {
        //throw new UnsupportedOperationException("TODO");
        return 0;
    }

    @Override
    public void sort(@NotNull Relation relation, @NotNull BlockOutput output) {
        // Phase 1: Sort all Lists
        /*
        * Listen erstellen
        * Für jede Liste:
        *   * Liste in Hauptspeicher laden
        *   * Liste sortieren
        *   * Liste zurück auf Platte schreiben
        * */
        BlockManager bm = getBlockManager();
        int partitionSize = bm.getFreeBlocks();
        int totalRelationSize = relation.getEstimatedSize();

        // Check if available space is sufficient
        if(totalRelationSize > partitionSize  * partitionSize)
            throw new RelationSizeExceedsCapacityException();

        int listCount = totalRelationSize / partitionSize + (totalRelationSize % partitionSize > 0 ? 1 : 0);
        List<Block> blockList = Lists.newArrayList(relation.iterator());
        List<List<Block>> partitions = Lists.partition(blockList, partitionSize);

        // Sort each list and store back on disk
        partitions.forEach(new Consumer<List<Block>>() {
            @Override
            public void accept(List<Block> blocks) {
                // Load list into memory
                loadAll(blocks, bm);

                // Sort
                BlockSorter.INSTANCE.sort(
                        relation,
                        blocks,
                        relation.getColumns().getColumnComparator(getSortColumnIndex())
                );

                // Write back to disk
                saveAll(blocks, bm);
            }
        });

        // Phase 2: Merge!

        List<PartitionState> states = Lists.newArrayList();
        for(int i = 0; i < partitions.size(); i++){
            states.add(new PartitionState(partitions.get(i)));
        }

        Block outputBlock = bm.allocate(true);
        while (!states.isEmpty()){
            PartitionState smallest = states.get(0);
            for(int i = 1; i < states.size(); i++){
                if(relation.getColumns().getColumnComparator(getSortColumnIndex())
                        .compare(states.get(i).currentTuple, smallest.currentTuple) < 0){
                    smallest = states.get(i);
                }
            }
            outputBlock.append(smallest.currentTuple);
            if(outputBlock.isFull()){
                output.output(outputBlock);
            }
            if(smallest.hasNext()){
                smallest.next();
            } else {
                smallest.release();
                states.remove(smallest);
            }
        }
        if(!outputBlock.isEmpty()){
            output.output(outputBlock);
        }
        bm.release(outputBlock, false);

    }

    private static void saveAll(@NotNull List<Block> blocks, BlockManager bm) {
        for (int i = 0; i < blocks.size(); i++) {
            Block ramBlock = blocks.get(i);
            Block diskBlock = bm.release(ramBlock, true);
            blocks.set(i, diskBlock);
        }
    }

    private static void loadAll(@NotNull List<Block> blocks, BlockManager bm) {
        for (int i = 0; i < blocks.size(); i++) {
            blocks.set(i, bm.load(blocks.get(i)));
        }
    }

    class PartitionState {
        Block currentBlock;
        Iterator<Block> blockIterator;
        Tuple currentTuple;
        Iterator<Tuple> tupleIterator;

        public PartitionState(List<Block> partition){
            blockIterator = partition.iterator();
            if(blockIterator.hasNext()){
                currentBlock = blockIterator.next();
                currentBlock = getBlockManager().load(currentBlock);
                tupleIterator = currentBlock.iterator();
                currentTuple = tupleIterator.next();
            } else {
                currentBlock = null;
                currentTuple = null;
                tupleIterator = null;
            }
        }
        public boolean hasNext(){
            return tupleIterator.hasNext() || blockIterator.hasNext();
        }

        public Tuple next(){
            if(tupleIterator.hasNext()){
                return currentTuple = tupleIterator.next();
            } else {
                getBlockManager().release(currentBlock, false);
                currentBlock = blockIterator.next();
                currentBlock = getBlockManager().load(currentBlock);
                tupleIterator = currentBlock.iterator();
                currentTuple = tupleIterator.next();
            }
            return null;
        }

        public void release(){
            if(currentBlock != null){
                getBlockManager().release(currentBlock, false);
            }
        }

    }
}
