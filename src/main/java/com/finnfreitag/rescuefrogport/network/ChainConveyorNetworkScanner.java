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

    /** A package together with the conveyor and section it currently resides on. */
    public record PackageOnConveyor(ChainConveyorPackage pkg, ChainConveyorBlockEntity conveyor, String sectionId) {}

    /** Position, address, and the conveyor block position a Rescue Frogport is connected to. */
    public record RescueFrogportInfo(BlockPos pos, String address, BlockPos conveyorPos) {}

    private final Level level;
    private final Set<BlockPos> visitedConveyors = new HashSet<>();
    private final List<ChainConveyorBlockEntity> allConveyors = new ArrayList<>();
    private final List<PackageOnConveyor> allPackages = new ArrayList<>();
    private final Set<String> allRegisteredAddresses = new HashSet<>();
    private final List<RescueFrogportInfo> rescueFrogports = new ArrayList<>();
    private final Map<BlockPos, Set<BlockPos>> networkGraph = new HashMap<>();

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
            Set<BlockPos> neighbors = networkGraph.computeIfAbsent(current, k -> new HashSet<>());
            for (BlockPos relativeConnection : ccbe.connections) {
                BlockPos neighborPos = current.offset(relativeConnection);
                neighbors.add(neighborPos);
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
        BlockPos pos = ccbe.getBlockPos();
        String stationId = "station:" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

        for (ChainConveyorPackage pkg : ccbe.getLoopingPackages()) {
            BlockPos exitOffset = ccbe.routingTable.getExitFor(pkg.item);
            String sectionId;
            if (exitOffset != null && !exitOffset.equals(BlockPos.ZERO)) {
                BlockPos targetPos = pos.offset(exitOffset);
                if (pos.compareTo(targetPos) <= 0) {
                    sectionId = "link:" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "->" + targetPos.getX() + "," + targetPos.getY() + "," + targetPos.getZ();
                } else {
                    sectionId = "link:" + targetPos.getX() + "," + targetPos.getY() + "," + targetPos.getZ() + "->" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
                }
            } else {
                sectionId = stationId;
            }
            allPackages.add(new PackageOnConveyor(pkg, ccbe, sectionId));
        }

        for (Map.Entry<BlockPos, List<ChainConveyorPackage>> entry : ccbe.getTravellingPackages().entrySet()) {
            BlockPos relativeTarget = entry.getKey();
            BlockPos targetPos = pos.offset(relativeTarget);

            String linkId;
            if (pos.compareTo(targetPos) <= 0) {
                linkId = "link:" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "->" + targetPos.getX() + "," + targetPos.getY() + "," + targetPos.getZ();
            } else {
                linkId = "link:" + targetPos.getX() + "," + targetPos.getY() + "," + targetPos.getZ() + "->" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
            }

            for (ChainConveyorPackage pkg : entry.getValue()) {
                allPackages.add(new PackageOnConveyor(pkg, ccbe, linkId));
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
                addRescueFrogportInfo(portPos, filter, ccbe.getBlockPos());
            }
        }
        for (Map.Entry<BlockPos, ConnectedPort> entry : ccbe.travelPorts.entrySet()) {
            BlockPos relativeOffset = entry.getKey();
            ConnectedPort port = entry.getValue();
            String filter = port.filter();
            if (filter != null && filter.startsWith("rescue-")) {
                BlockPos portPos = ccbe.getBlockPos().subtract(relativeOffset);
                addRescueFrogportInfo(portPos, filter, ccbe.getBlockPos());
            }
        }
    }

    private void findAdjacentRescueFrogports(BlockPos conveyorPos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = conveyorPos.relative(dir);
            if (level.getBlockEntity(neighbor) instanceof RescueFrogportBlockEntity rfbe) {
                String addr = rfbe.getRescueAddress();
                if (addr != null && !addr.isEmpty()) {
                    addRescueFrogportInfo(neighbor, addr, conveyorPos);
                }
            }
        }
    }

    private void addRescueFrogportInfo(BlockPos pos, String address, BlockPos conveyorPos) {
        boolean alreadyFound = rescueFrogports.stream()
                .anyMatch(info -> info.pos().equals(pos));
        if (!alreadyFound) {
            rescueFrogports.add(new RescueFrogportInfo(pos, address, conveyorPos));
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
     * Finds the rescue frogport closest to the given position along the chain conveyor network path.
     */
    public @Nullable RescueFrogportInfo findNearestRescueFrogport(BlockPos startConveyorPos) {
        RescueFrogportInfo nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        for (RescueFrogportInfo info : rescueFrogports) {
            int dist = getNetworkDistance(startConveyorPos, info.conveyorPos());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = info;
            }
        }

        // Fallback to direct Manhattan if network pathfinding fails or returns unreachable
        if (nearest == null || nearestDist == Integer.MAX_VALUE) {
            nearest = null;
            nearestDist = Integer.MAX_VALUE;
            for (RescueFrogportInfo info : rescueFrogports) {
                int dist = startConveyorPos.distManhattan(info.pos());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = info;
                }
            }
        }

        return nearest;
    }

    public int getNetworkDistance(BlockPos start, BlockPos end) {
        if (start.equals(end)) return 0;

        Map<BlockPos, Integer> dists = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.dist));

        dists.put(start, 0);
        pq.add(new Node(start, 0));

        while (!pq.isEmpty()) {
            Node curr = pq.poll();
            if (curr.pos.equals(end)) {
                return curr.dist;
            }
            if (curr.dist > dists.getOrDefault(curr.pos, Integer.MAX_VALUE)) {
                continue;
            }

            Set<BlockPos> neighbors = networkGraph.get(curr.pos);
            if (neighbors != null) {
                for (BlockPos neighbor : neighbors) {
                    int edgeWeight = curr.pos.distManhattan(neighbor);
                    int newDist = curr.dist + edgeWeight;
                    if (newDist < dists.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                        dists.put(neighbor, newDist);
                        pq.add(new Node(neighbor, newDist));
                    }
                }
            }
        }
        return Integer.MAX_VALUE; // Unreachable
    }

    private record Node(BlockPos pos, int dist) {}
}
