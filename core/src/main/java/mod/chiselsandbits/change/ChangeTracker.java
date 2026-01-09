package mod.chiselsandbits.change;

import com.communi.suggestu.scena.core.registries.IPlatformRegistryManager;
import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.change.changes.IChange;
import mod.chiselsandbits.api.change.changes.IllegalChangeAttempt;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.INBTSerializable;
import mod.chiselsandbits.change.changes.BitChange;
import mod.chiselsandbits.change.changes.CombinedChange;
import mod.chiselsandbits.multistate.snapshot.LazilyDecodingSingleBlockMultiStateSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ChangeTracker implements IChangeTracker {
    protected final Player player;
    protected final LinkedList<CombinedChange> changes = new LinkedList<>();
    protected int currentIndex = 0;

    private static final Logger LOGGER = LogManager.getLogger();

    public ChangeTracker() {
        this.player = null;
    }

    public ChangeTracker(final Player player) {
        this.player = player;
    }

    public void reset() {
        changes.clear();
        sendUpdate();
    }

    @Override
    public void onBlocksUpdated(
            final LevelAccessor level,
            final Map<BlockPos, IMultiStateSnapshot> beforeStates,
            final Map<BlockPos, IMultiStateSnapshot> afterState
    ) {
        boolean isServerThread = "Server thread".equals(Thread.currentThread().getName());
        if (isServerThread) {
//            LOGGER.info("onBlocksUpdated: is server side");
            int size = StateEntrySize.current().getBitsPerBlockSide();
            BlockPos pos = null;
            IBlockInformation[][][] matrix = new IBlockInformation[size][size][size];
            for (Map.Entry<BlockPos, IMultiStateSnapshot> entry : afterState.entrySet()) {
                pos = entry.getKey();
                IMultiStateSnapshot snapshot = entry.getValue();

                if (snapshot instanceof LazilyDecodingSingleBlockMultiStateSnapshot lazySnapshot) {
                    lazySnapshot.stream().forEach(e -> {
                        Vec3 startPoint = e.getStartPoint();
                        int x = (int) (startPoint.x() * size);
                        int y = (int) (startPoint.y() * size);
                        int z = (int) (startPoint.z() * size);
                        matrix[y][x][z] = e.getBlockInformation();
                    });
                }
            }

            Set<Block> sN = new HashSet<>(Set.of(Blocks.AIR));
            Set<Block> sB = new HashSet<>(Set.of(
                    Blocks.STRIPPED_ACACIA_WOOD,
                    Blocks.STRIPPED_ACACIA_LOG,
                    Blocks.STRIPPED_SPRUCE_WOOD,
                    Blocks.STRIPPED_SPRUCE_LOG,
                    Blocks.STRIPPED_BIRCH_WOOD,
                    Blocks.STRIPPED_BIRCH_LOG,
                    Blocks.STRIPPED_JUNGLE_WOOD,
                    Blocks.STRIPPED_JUNGLE_LOG,
                    Blocks.STRIPPED_DARK_OAK_WOOD,
                    Blocks.STRIPPED_DARK_OAK_LOG,
                    Blocks.STRIPPED_OAK_WOOD,
                    Blocks.STRIPPED_OAK_LOG,
                    Blocks.STRIPPED_CHERRY_WOOD,
                    Blocks.STRIPPED_CHERRY_LOG,
                    Blocks.STRIPPED_MANGROVE_WOOD,
                    Blocks.STRIPPED_MANGROVE_LOG
            ));

            List<List<List<Set<Block>>>> patternBowl = List.of(
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN)
                    ),
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN)
                    ),
                    List.of(
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB)
                    ),
                    List.of(
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB)
                    )
            );

            List<List<List<Set<Block>>>> patternStickN = List.of(
                    List.of(
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB)
                    ),
                    List.of(
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB)
                    )
            );

            List<List<List<Set<Block>>>> patternStickE = List.of(
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN),
                            List.of(sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN)
                    ),
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN),
                            List.of(sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN)
                    )
            );

            List<List<List<Set<Block>>>> patternStickS = List.of(
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN)
                    ),
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN),
                            List.of(sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN)
                    )
            );

            List<List<List<Set<Block>>>> patternStickW = List.of(
                    List.of(
                            List.of(sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB)
                    ),
                    List.of(
                            List.of(sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sB, sB, sB, sB, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sB, sB, sB, sB, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sB, sB, sB)
                    )
            );

            List<List<List<Set<Block>>>> patternMaracaN = List.of(
                    List.of(
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN)
                    ),
                    List.of(
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN)
                    ),
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN),
                            List.of(sN, sB, sB, sB, sB, sN),
                            List.of(sN, sB, sB, sB, sB, sN),
                            List.of(sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN)
                    )
            );

            List<List<List<Set<Block>>>> patternMaracaE = List.of(
                    List.of(
                            List.of(sB, sB, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN)
                    ),
                    List.of(
                            List.of(sB, sB, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN),
                            List.of(sB, sB, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN)
                    ),
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN)
                    )
            );

            List<List<List<Set<Block>>>> patternMaracaS = List.of(
                    List.of(
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB)
                    ),
                    List.of(
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sN, sN, sB, sB, sN, sN),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB)
                    ),
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN),
                            List.of(sN, sB, sB, sB, sB, sN),
                            List.of(sN, sB, sB, sB, sB, sN),
                            List.of(sN, sB, sB, sB, sB, sN),
                            List.of(sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN)
                    )
            );

            List<List<List<Set<Block>>>> patternMaracaW = List.of(
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sB, sB)
                    ),
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sB, sB),
                            List.of(sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sB, sB)
                    ),
                    List.of(
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sB, sB, sB, sB, sN),
                            List.of(sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN, sN)
                    )
            );

            ItemStack rewardBowl = new ItemStack(Items.BOWL);
            ItemStack rewardStick = new ItemStack(Items.STICK);
            ItemStack rewardMaraca = new ItemStack(Items.DIAMOND); // ribbits:maraca

            Optional<Item> itemOpt = IPlatformRegistryManager.getInstance()
                    .getItemRegistry()
                    .getValue(new ResourceLocation("ribbits", "maraca"));
            if (itemOpt.isPresent()) {
                rewardMaraca = new ItemStack(itemOpt.get());
            }

            Map<List<List<List<Set<Block>>>>, ItemStack> patterns = new HashMap<>();
            patterns.put(patternBowl, rewardBowl);
            patterns.put(patternStickN, rewardStick);
            patterns.put(patternStickE, rewardStick);
            patterns.put(patternStickS, rewardStick);
            patterns.put(patternStickW, rewardStick);
            patterns.put(patternMaracaN, rewardMaraca);
            patterns.put(patternMaracaE, rewardMaraca);
            patterns.put(patternMaracaS, rewardMaraca);
            patterns.put(patternMaracaW, rewardMaraca);

            for (Map.Entry<List<List<List<Set<Block>>>>, ItemStack> entry : patterns.entrySet()) {
                List<List<List<Set<Block>>>> pattern = entry.getKey();
                ItemStack rewardItem = entry.getValue();

                int patternSizeY = pattern.size();
                int patternSizeX = pattern.get(0).size();
                int patternSizeZ = pattern.get(0).get(0).size();

                boolean found = false;

                int patternStartX = 0;
                int patternStartY = 0;
                int patternStartZ = 0;

//                int startY = 0;
                for (int startY = 0; startY <= size - patternSizeY; startY++) {
                    for (int startX = 0; startX <= size - patternSizeX; startX++) {
                        for (int startZ = 0; startZ <= size - patternSizeZ; startZ++) {
                            boolean match = true;

//                            System.out.println("START");

                            for (int py = 0; py < patternSizeY; py++) {
                                for (int px = 0; px < patternSizeX; px++) {
                                    for (int pz = 0; pz < patternSizeZ; pz++) {
                                        IBlockInformation info = matrix[startY + py][startX + px][startZ + pz];
                                        Block block = info.getBlockState().getBlock();

//                                        if (py == 0) {
//                                            StringBuilder str =  new StringBuilder();
//                                            str.append("X").append(startX + px).append(":Z").append(startZ + pz)
//                                                    .append(" - ").append(
//                                                            pattern.get(py).get(px).get(pz).size()
//                                                    )
//                                                    .append(" - ").append(
//                                                            block
//                                                    )
//                                                    .append(" - ").append(
//                                                            pattern.get(py).get(px).get(pz).contains(block)
//                                                    );
//                                            System.out.println(str);
//                                        }
                                        match = pattern.get(py).get(px).get(pz).contains(block);

                                        if (!match) break;
                                    }
                                    if (!match) break;
                                }
                                if (!match) break;
                            }

//                            System.out.println("END");

                            if (match) {
                                patternStartX = startX;
                                patternStartY = startY;
                                patternStartZ = startZ;
//                                System.out.println("SUCCESS! Pattern found at X:" + patternStartX + " Y:" + patternStartY + " Z:" + patternStartZ);
                                found = true;
                                break;
                            }
                        }
                    }
                }

                if (found) {
                    Player player = Minecraft.getInstance().player;
                    if (player != null) {
//                        player.sendSystemMessage(Component.literal("SUCCESS! Pattern found at X:" + patternStartX + " Y:" + patternStartY + " Z:" + patternStartZ));
                        if (pos != null && level instanceof Level castLevel) {
                            double x = pos.getX() + 0.5;
                            double y = pos.getY() + 1.0;
                            double z = pos.getZ() + 0.5;

                            System.out.println(level);
                            System.out.println(castLevel);
                            System.out.println(x + " " + y + " " + z);
                            System.out.println(rewardItem);

                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            ItemEntity entity = new ItemEntity(castLevel, x, y, z, rewardItem);
                            level.addFreshEntity(entity);
                        }
                    }
                }
            }
        }

        if (!beforeStates.keySet().containsAll(afterState.keySet()) || !afterState.keySet().containsAll(beforeStates.keySet()))
            throw new IllegalArgumentException("Initial States and Target States reference difference block positions");

        changes.addFirst(
                new CombinedChange(
                        beforeStates.entrySet().stream()
                                .map(e -> new BitChange(
                                        e.getKey(),
                                        e.getValue(),
                                        afterState.get(e.getKey())
                                ))
                                .collect(Collectors.toSet())
                )
        );

        currentIndex = 0;

        final int maxSize = IChiselsAndBitsAPI.getInstance().getConfiguration().getServer().getChangeTrackerSize().get();
        if (changes.size() > maxSize) {
            while (changes.size() > maxSize) {
                changes.removeLast();
            }
        }
        sendUpdate();
    }

    @Override
    public Deque<IChange> getChanges() {
        return new LinkedList<>(changes);
    }

    @Override
    public void clear() {
        changes.clear();
        sendUpdate();
    }

    public Optional<IChange> getCurrentUndo() {
        if (getChanges().size() <= currentIndex || currentIndex < 0) {
            return Optional.empty();
        }

        return Optional.of(changes.get(currentIndex));
    }

    public Optional<IChange> getCurrentRedo() {
        if (getChanges().size() < currentIndex || currentIndex < 1) {
            return Optional.empty();
        }

        return Optional.of(changes.get(currentIndex - 1));
    }

    @Override
    public boolean canUndo(final Player player) {
        return getCurrentUndo().map(c -> c.canUndo(player)).orElse(false);
    }

    @Override
    public boolean canRedo(final Player player) {
        return getCurrentRedo().map(c -> c.canRedo(player)).orElse(false);
    }

    @Override
    public void undo(final Player player) throws IllegalChangeAttempt {
        if (!canUndo(player))
            throw new IllegalChangeAttempt();

        if (getCurrentUndo().isPresent()) {
            final IChange change = getCurrentUndo().get();
            change.undo(player);
            currentIndex = Math.min(changes.size(), currentIndex + 1);
            sendUpdate();
        }
    }

    @Override
    public void redo(final Player player) throws IllegalChangeAttempt {
        if (!canRedo(player))
            throw new IllegalChangeAttempt();

        if (getCurrentRedo().isPresent()) {
            final IChange change = getCurrentRedo().get();
            change.redo(player);
            currentIndex = Math.max(0, currentIndex - 1);
            sendUpdate();
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.put("changes", this.changes.stream().map(INBTSerializable::serializeNBT).collect(Collectors.toCollection(ListTag::new)));
        tag.putInt("index", this.currentIndex);
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag nbt) {
        this.changes.clear();
        this.changes.addAll(nbt.getList("changes", Tag.TAG_COMPOUND).stream().map(CombinedChange::new).toList());
        this.currentIndex = nbt.getInt("index");
    }

    private void sendUpdate() {
        if (player != null && player instanceof ServerPlayer serverPlayer && !serverPlayer.level().isClientSide()) {
            ChangeTrackerSyncManager.getInstance().add(this, serverPlayer);
        }
    }
}
