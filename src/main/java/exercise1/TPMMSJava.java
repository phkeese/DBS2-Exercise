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
        throw new UnsupportedOperationException("TODO");
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


        // Get iterators for each list's blocks
        var nextBlocks = Lists.transform(partitions, new Function<List<Block>, Iterator<Block>>() {
            @Override
            public Iterator<Block> apply(List<Block> input) {
                return input.iterator();
            }
        });

        var currentBlocks = Lists.transform(nextBlocks, new Function<Iterator<Block>, Block>() {
            @Override
            public Block apply(Iterator<Block> input) {
                Block block = input.next();
                return bm.load(block);
            }
        });

        var nextTuples = Lists.transform(currentBlocks, new Function<Block, Iterator<Tuple>>() {
            @Override
            public Iterator<Tuple> apply(Block input) {
                return input.iterator();
            }
        });
        Tuple currentTuples[] = new Tuple[partitions.size()];
        // Fill current tuples
        for (int i = 0; i < partitions.size(); i++) {
            currentTuples[i] = nextTuples.get(i).next();
        }

        // Phase 2: Merge!

        boolean hasTuple = false;
        Block outputBlock = bm.allocate(true);
        do{
            hasTuple = false;

            // Find smallest value
            int smallest = 0;
            for (int i = 1; i < partitions.size(); i++) {
                Tuple here = currentTuples[i];
                if (here != null && relation.getColumns().getColumnComparator(getSortColumnIndex()).compare(here, currentTuples[smallest]) < 0) {
                    smallest = i;
                }
            }

            // Write smallest to output & write out if full
            outputBlock.append(currentTuples[smallest]);
            if (outputBlock.isFull()) {
                output.output(outputBlock);
            }

            /* Idee: Wir kapseln eine Partition in einer neuen Klasse mit Methoden
             * sort() -> lädt alle Blöcke, sortiert diese, speichert auf Platte
             * iterator() -> gibt einen Iterator zurück, der automatisch Blöcke lädt/freigibt
             *
             * Wichtig: Wir verwalten dann eine Liste aus Iteratoren und löschen einen Iterator, sobald er keine Tupel
             * mehr hat. Damit sparen wir uns viele Edge-Cases mit null und alten Daten
             */

            // Read next tuple
            if (nextTuples.get(smallest).hasNext()) {
                currentTuples[smallest] = nextTuples.get(smallest).next();
            } else {
                // Block end, try to get new block
                // Release old block
                Block oldBlock = currentBlocks.get(smallest);
                bm.release(oldBlock, false);
                // Get new block
                Iterator<Block> nextBlockIterator = nextBlocks.get(smallest);
                if (nextBlockIterator.hasNext()) {
                    // Available, load into memory and get new tuples
                    Block newBlock = nextBlockIterator.next();
                    newBlock = bm.load(newBlock);
                    currentBlocks.set(smallest, newBlock);
                    nextTuples.set(smallest, newBlock.iterator());
                    currentTuples[smallest] = nextTuples.get(smallest).next();
                } else {
                    currentTuples[smallest] = null;
                }
            }
        }while(hasTuple);

        throw new UnsupportedOperationException("TODO");
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
}
