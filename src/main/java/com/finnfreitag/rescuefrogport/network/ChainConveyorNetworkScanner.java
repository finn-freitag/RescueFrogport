package com.finnfreitag.rescuefrogport.network;

import com.finnfreitag.rescuefrogport.block.RescueFrogportBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity.ConnectedPort;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRoutingTable.RoutingTableEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Traverses a connected chain conveyor network starting from a given block
 * and collects all packages, registered port addresses, and rescue frogports.
 */
public class ChainConveyorNetworkScanner {

    /** A package together with the conveyor it currently resides on. */
    public record PackageOnConveyor(ChainConveyorPackage pkg, ChainConveyorBlockEntity conveyor) {}

    /** Position and address of a Rescue Frogport found on the network. */
    public record RescueFrogportInfo(BlockPos pos, String address) {}

    private final Level level;
    private final Set<BlockPos> visitedConveyors = new HashSet<>();
    private final List<ChainConveyorBlockEntity> allConveyors = new ArrayList<>();
    private final List<PackageOnConveyor> allPackages = new ArrayList<>();
    private final Set<String> allRegisteredAddresses = new HashSet<>();
    private final List<RescueFrogportInfo> rescueFrogports = new ArrayList<>();

    public ChainConveyorNetworkScanner(Level level, BlockPos startPos) {
        this.level = level;
        scan(startPos);
    }

    // ------------------------------------------------------------------
    // Network traversal (BFS through chain connections)
    // ------------------------------------------------------------------

    private void scan(BlockPos startPos) {
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visitedConveyors.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockEntity be = level.getBlockEntity(current);

            if (!(be instanceof ChainConveyorBlockEntity ccbe)) continue;

            allConveyors.add(ccbe);

            collectPackages(ccbe);
            collectAddresses(ccbe);
            collectConnectedRescueFrogports(ccbe);
            findAdjacentRescueFrogports(current);

            // Traverse chain connections.
            for (BlockPos relativeConnection : ccbe.connections) {
                BlockPos neighborPos = current.offset(relativeConnection);
                if (visitedConveyors.add(neighborPos)) {
                    queue.add(neighborPos);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Data collection helpers
    // ------------------------------------------------------------------

    private void collectPackages(ChainConveyorBlockEntity ccbe) {
        for (ChainConveyorPackage pkg : ccbe.getLoopingPackages()) {
            allPackages.add(new PackageOnConveyor(pkg, ccbe));
        }
        for (List<ChainConveyorPackage> travelList : ccbe.getTravellingPackages().values()) {
            for (ChainConveyorPackage pkg : travelList) {
                allPackages.add(new PackageOnConveyor(pkg, ccbe));
            }
        }
    }

    private void collectAddresses(ChainConveyorBlockEntity ccbe) {
        // From routing table entries
        for (RoutingTableEntry entry : ccbe.routingTable.entriesByDistance) {
            String port = entry.port();
            if (port != null && !port.isEmpty()) {
                allRegisteredAddresses.add(port);
            }
        }
        // From locally connected loop ports
        for (ConnectedPort port : ccbe.loopPorts.values()) {
            if (port.filter() != null && !port.filter().isEmpty()) {
                allRegisteredAddresses.add(port.filter());
            }
        }
        // From locally connected travel ports
        for (ConnectedPort port : ccbe.travelPorts.values()) {
            if (port.filter() != null && !port.filter().isEmpty()) {
                allRegisteredAddresses.add(port.filter());
            }
        }
    }

    private void collectConnectedRescueFrogports(ChainConveyorBlockEntity ccbe) {
        for (Map.Entry<BlockPos, ConnectedPort> entry : ccbe.loopPorts.entrySet()) {
            BlockPos relativeOffset = entry.getKey();
            ConnectedPort port = entry.getValue();
            String filter = port.filter();
            if (filter != null && filter.startsWith("rescue-")) {
                BlockPos portPos = ccbe.getBlockPos().subtract(relativeOffset);
                addRescueFrogportInfo(portPos, filter);
            }
        }
        for (Map.Entry<BlockPos, ConnectedPort> entry : ccbe.travelPorts.entrySet()) {
            BlockPos relativeOffset = entry.getKey();
            ConnectedPort port = entry.getValue();
            String filter = port.filter();
            if (filter != null && filter.startsWith("rescue-")) {
                BlockPos portPos = ccbe.getBlockPos().subtract(relativeOffset);
                addRescueFrogportInfo(portPos, filter);
            }
        }
    }

    private void findAdjacentRescueFrogports(BlockPos conveyorPos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = conveyorPos.relative(dir);
            if (level.getBlockEntity(neighbor) instanceof RescueFrogportBlockEntity rfbe) {
                String addr = rfbe.getRescueAddress();
                if (addr != null && !addr.isEmpty()) {
                    addRescueFrogportInfo(neighbor, addr);
                }
            }
        }
    }

    private void addRescueFrogportInfo(BlockPos pos, String address) {
        boolean alreadyFound = rescueFrogports.stream()
                .anyMatch(info -> info.pos().equals(pos));
        if (!alreadyFound) {
            rescueFrogports.add(new RescueFrogportInfo(pos, address));
        }
    }

    // ------------------------------------------------------------------
    // Public accessors
    // ------------------------------------------------------------------

    public List<ChainConveyorBlockEntity> getAllConveyors() {
        return Collections.unmodifiableList(allConveyors);
    }

    public List<PackageOnConveyor> getAllPackages() {
        return Collections.unmodifiableList(allPackages);
    }

    public Set<String> getAllRegisteredAddresses() {
        return Collections.unmodifiableSet(allRegisteredAddresses);
    }

    public List<RescueFrogportInfo> getRescueFrogports() {
        return Collections.unmodifiableList(rescueFrogports);
    }

    /**
     * Finds the rescue frogport closest (Manhattan distance) to the given position.
     */
    public @Nullable RescueFrogportInfo findNearestRescueFrogport(BlockPos pos) {
        RescueFrogportInfo nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        for (RescueFrogportInfo info : rescueFrogports) {
            int dist = pos.distManhattan(info.pos());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = info;
            }
        }
        return nearest;
    }
}
