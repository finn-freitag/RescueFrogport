package com.finnfreitag.rescuefrogport.network;

import com.finnfreitag.rescuefrogport.network.ChainConveyorNetworkScanner.PackageOnConveyor;
import com.finnfreitag.rescuefrogport.network.ChainConveyorNetworkScanner.RescueFrogportInfo;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.*;

/**
 * Implements the three rescue rules:
 * <ol>
 *   <li>Packages without an address -> rescue to nearest rescue frogport</li>
 *   <li>Packages with an address that has no matching receiver -> rescue</li>
 *   <li>When &gt; threshold identical packages exist -> rescue excess</li>
 * </ol>
 * Rescued packages are readdressed to the nearest rescue frogport, so the conveyor
 * routes them to the frogport, which then catches them.
 */
public class PackageRescueHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void processNetwork(Level level,
                                       ChainConveyorNetworkScanner scanner,
                                       int congestionThreshold) {

        List<RescueFrogportInfo> rescueFrogports = scanner.getRescueFrogports();
        if (rescueFrogports.isEmpty()) return;

        Set<String> validAddresses = scanner.getAllRegisteredAddresses();
        List<PackageOnConveyor> allPackages = scanner.getAllPackages();

        // Packages scheduled for rescue readdressing
        List<PackageToRescue> toRescue = new ArrayList<>();

        // ------------------------------------------------------------------
        // Rule 1 & 2: unaddressed / invalid-address packages
        // ------------------------------------------------------------------
        for (PackageOnConveyor poc : allPackages) {
            ItemStack box = getBox(poc.pkg());
            if (box == null || box.isEmpty()) continue;

            String address = getAddress(box);

            if (address == null || address.isEmpty()) {
                // Rule 1 - no address at all
                RescueFrogportInfo nearest = scanner.findNearestRescueFrogport(
                        poc.conveyor().getBlockPos());
                if (nearest != null) {
                    toRescue.add(new PackageToRescue(poc, nearest));
                }
            } else if (!isAddressValid(address, validAddresses, rescueFrogports)) {
                // Rule 2 - address doesn't match any receiver
                RescueFrogportInfo nearest = scanner.findNearestRescueFrogport(
                        poc.conveyor().getBlockPos());
                if (nearest != null) {
                    toRescue.add(new PackageToRescue(poc, nearest));
                }
            }
        }

        // ------------------------------------------------------------------
        // Rule 3: congestion detection (identical-package buildup)
        // ------------------------------------------------------------------
        Set<ChainConveyorPackage> alreadyScheduled = new HashSet<>();
        for (PackageToRescue p : toRescue) alreadyScheduled.add(p.poc.pkg());

        List<List<PackageOnConveyor>> groups = new ArrayList<>();
        for (PackageOnConveyor poc : allPackages) {
            if (alreadyScheduled.contains(poc.pkg())) continue;
            ItemStack box = getBox(poc.pkg());
            if (box == null || box.isEmpty()) continue;

            boolean placed = false;
            for (List<PackageOnConveyor> group : groups) {
                ItemStack representative = getBox(group.get(0).pkg());
                if (representative != null
                        && ItemStack.isSameItemSameComponents(box, representative)) {
                    group.add(poc);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<PackageOnConveyor> newGroup = new ArrayList<>();
                newGroup.add(poc);
                groups.add(newGroup);
            }
        }

        for (List<PackageOnConveyor> group : groups) {
            if (group.size() > congestionThreshold) {
                int excess = group.size() - congestionThreshold;
                // Remove from the tail of the group (least recently added)
                for (int i = 0; i < excess; i++) {
                    PackageOnConveyor poc = group.get(group.size() - 1 - i);
                    RescueFrogportInfo nearest = scanner.findNearestRescueFrogport(
                            poc.conveyor().getBlockPos());
                    if (nearest != null) {
                        toRescue.add(new PackageToRescue(poc, nearest));
                    }
                }
            }
        }

        // ------------------------------------------------------------------
        // Execute all rescues by readdressing packages
        // ------------------------------------------------------------------
        for (PackageToRescue rescue : toRescue) {
            executeRescue(level, rescue);
        }
    }

    private static boolean isAddressValid(String address,
                                           Set<String> validAddresses,
                                           List<RescueFrogportInfo> rescueFrogports) {
        // Rescue addresses are always valid (prevents re-rescue loops)
        for (RescueFrogportInfo info : rescueFrogports) {
            if (address.equals(info.address())) return true;
        }
        return validAddresses.contains(address);
    }

    private static String getAddress(ItemStack box) {
        try {
            return box.get(AllDataComponents.PACKAGE_ADDRESS);
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack getBox(ChainConveyorPackage pkg) {
        try {
            return pkg.item;
        } catch (Exception e) {
            LOGGER.warn("Could not access ChainConveyorPackage.item: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    private static void executeRescue(Level level, PackageToRescue rescue) {
        ChainConveyorBlockEntity conveyor = rescue.poc.conveyor();
        ChainConveyorPackage pkg = rescue.poc.pkg();
        ItemStack box = getBox(pkg);
        if (box == null || box.isEmpty()) return;

        // Update the package address to the rescue address
        box.set(AllDataComponents.PACKAGE_ADDRESS, rescue.target.address());

        LOGGER.debug("Readdressed conveyor package at {} to rescue address: {}",
                conveyor.getBlockPos(), rescue.target.address());

        // Notify the chain conveyor about the changes so client updates model and server updates routing
        conveyor.setChanged();
        BlockState state = conveyor.getBlockState();
        level.sendBlockUpdated(conveyor.getBlockPos(), state, state, 3);
    }

    private record PackageToRescue(PackageOnConveyor poc, RescueFrogportInfo target) {}
}
