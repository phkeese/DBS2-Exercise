package exercise1;

import com.google.common.collect.Lists;
import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.dbms.Block;
import de.hpi.dbs2.dbms.BlockManager;
import de.hpi.dbs2.dbms.BlockOutput;
import de.hpi.dbs2.dbms.Relation;
import de.hpi.dbs2.dbms.utils.BlockSorter;
import de.hpi.dbs2.exercise1.SortOperation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        //Phase 1: Sort all Lists
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

        //check if available space is sufficient
        if(totalRelationSize > partitionSize  * partitionSize)
            throw new RelationSizeExceedsCapacityException();

        int listCount = totalRelationSize / partitionSize + (totalRelationSize % partitionSize > 0 ? 1 : 0);
        List<Block> blockList = Lists.newArrayList(relation.iterator());
        List<List<Block>> lists = Lists.partition(blockList, partitionSize);


        //Phase 2: Merge!
        throw new UnsupportedOperationException("TODO");
    }
}
