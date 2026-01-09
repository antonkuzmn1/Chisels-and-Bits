package mod.chiselsandbits.api.change;

import com.google.common.collect.ImmutableMap;
import mod.chiselsandbits.api.change.changes.IChange;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.Deque;
import java.util.Map;

/**
 * The change tracker for tracking changes to bit blocks.
 * Currently still work in progress.
 */
public interface IChangeTracker extends IChange
{
    /**
     * Invoked when a chiseled block is updated from one state to the next.
     *
     * @param level Block o'LevelAccessor
     * @param blockPos The position of the block updated.
     * @param before The before state.
     * @param after The after state.
     */
    default void onBlockUpdated(
            final LevelAccessor level,
            final BlockPos blockPos,
            final IMultiStateSnapshot before,
            final IMultiStateSnapshot after
    ) {
        this.onBlocksUpdated(
            level,
            ImmutableMap.of(blockPos, before),
            ImmutableMap.of(blockPos, after)
        );
    }

    /**
     * Invoked when several chiseled blocks are updated from one state to the next.
     *
     * @param level Block o'LevelAccessor
     * @param beforeStates The states before the update.
     * @param afterState The states after the update.
     */
    void onBlocksUpdated(
            final LevelAccessor level,
            Map<BlockPos, IMultiStateSnapshot> beforeStates,
            final Map<BlockPos, IMultiStateSnapshot> afterState
    );

    /**
     * Gets a readonly-copy of the changes in the queue.
     * @return The changes last performed and recorded by this tracker.
     */
    Deque<IChange> getChanges();

    /**
     * Clears the tracker from all currently stored changes.
     */
    void clear();
}
