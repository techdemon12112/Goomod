package com.LeglessLizard.goomod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VeinTracker extends SavedData {
    private static final String NAME = "phage_vein_tracker";
    private static final Map<ServerLevel, VeinTracker> INSTANCES = new HashMap<>();

    private final Map<BlockPos, Integer> oreVeins = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> duplicationCounts = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> veinSizes = new ConcurrentHashMap<>();
    private final Set<BlockPos> playerPlacedOres = ConcurrentHashMap.newKeySet();
    private int nextVeinId = 1;

    public static final Factory<VeinTracker> FACTORY = new Factory<>(
        VeinTracker::new,
        VeinTracker::load
    );

    public static VeinTracker get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, l -> l.getDataStorage()
                .computeIfAbsent(FACTORY, NAME));
    }

    public VeinTracker() {}

    public static VeinTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        VeinTracker tracker = new VeinTracker();
        ListTag oreList = tag.getList("oreVeins", Tag.TAG_COMPOUND);
        for (Tag t : oreList) {
            CompoundTag entry = (CompoundTag) t;
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            int veinId = entry.getInt("veinId");
            tracker.oreVeins.put(pos, veinId);
        }
        ListTag countList = tag.getList("duplicationCounts", Tag.TAG_COMPOUND);
        for (Tag t : countList) {
            CompoundTag entry = (CompoundTag) t;
            int veinId = entry.getInt("veinId");
            int count = entry.getInt("count");
            tracker.duplicationCounts.put(veinId, count);
        }
        ListTag sizeList = tag.getList("veinSizes", Tag.TAG_COMPOUND);
        for (Tag t : sizeList) {
            CompoundTag entry = (CompoundTag) t;
            int veinId = entry.getInt("veinId");
            int size = entry.getInt("size");
            tracker.veinSizes.put(veinId, size);
        }
        ListTag playerList = tag.getList("playerPlacedOres", Tag.TAG_COMPOUND);
        for (Tag t : playerList) {
            CompoundTag entry = (CompoundTag) t;
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            tracker.playerPlacedOres.add(pos);
        }
        tracker.nextVeinId = tag.getInt("nextVeinId");
        return tracker;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag oreList = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : oreVeins.entrySet()) {
            CompoundTag e = new CompoundTag();
            BlockPos p = entry.getKey();
            e.putInt("x", p.getX());
            e.putInt("y", p.getY());
            e.putInt("z", p.getZ());
            e.putInt("veinId", entry.getValue());
            oreList.add(e);
        }
        tag.put("oreVeins", oreList);

        ListTag countList = new ListTag();
        for (Map.Entry<Integer, Integer> entry : duplicationCounts.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putInt("veinId", entry.getKey());
            e.putInt("count", entry.getValue());
            countList.add(e);
        }
        tag.put("duplicationCounts", countList);

        ListTag sizeList = new ListTag();
        for (Map.Entry<Integer, Integer> entry : veinSizes.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putInt("veinId", entry.getKey());
            e.putInt("size", entry.getValue());
            sizeList.add(e);
        }
        tag.put("veinSizes", sizeList);

        ListTag playerList = new ListTag();
        for (BlockPos p : playerPlacedOres) {
            CompoundTag e = new CompoundTag();
            e.putInt("x", p.getX());
            e.putInt("y", p.getY());
            e.putInt("z", p.getZ());
            playerList.add(e);
        }
        tag.put("playerPlacedOres", playerList);
        tag.putInt("nextVeinId", nextVeinId);
        return tag;
    }

    public void registerOre(BlockPos pos, int veinId) {
        oreVeins.put(pos, veinId);
        setDirty();
    }

    public void registerVeinSize(int veinId, int size) {
        veinSizes.put(veinId, size);
        setDirty();
    }

    public int getVeinId(BlockPos pos) {
        return oreVeins.getOrDefault(pos, -1);
    }

    public int getVeinSize(int veinId) {
        return veinSizes.getOrDefault(veinId, 1);
    }

    public int getDuplicationCount(int veinId) {
        return duplicationCounts.getOrDefault(veinId, 0);
    }

    public void incrementDuplicationCount(int veinId) {
        duplicationCounts.merge(veinId, 1, Integer::sum);
        setDirty();
    }

    public int getNextVeinId() {
        return nextVeinId++;
    }

    public void markPlayerPlaced(BlockPos pos) {
        playerPlacedOres.add(pos.immutable());
        setDirty();
    }

    public boolean isPlayerPlaced(BlockPos pos) {
        return playerPlacedOres.contains(pos);
    }
}
