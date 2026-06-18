package com.finnfreitag.rescuefrogport.network;

import com.finnfreitag.rescuefrogport.network.ChainConveyorNetworkScanner.PackageOnConveyor;
import com.finnfreitag.rescuefrogport.network.ChainConveyorNetworkScanner.RescueFrogportInfo;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
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
                PackageOnConveyor rep = group.get(0);
                ItemStack representative = getBox(rep.pkg());
                if (representative != null
                        && poc.sectionId().equals(rep.sectionId())
                        && arePackagesEqual(box, representative)) {
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

    public static boolean arePackagesEqual(ItemStack box1, ItemStack box2) {
        if (box1 == box2) return true;
        if (box1.isEmpty() || box2.isEmpty()) return false;

        // 1. Compare address
        String addr1 = PackageItem.getAddress(box1);
        String addr2 = PackageItem.getAddress(box2);
        if (!Objects.equals(addr1, addr2)) return false;

        // 2. Compare contents (order independent and consolidating duplicates)
        return areContentsEqual(box1, box2);
    }

    private static boolean areContentsEqual(ItemStack box1, ItemStack box2) {
        List<ItemStack> list1 = getConsolidatedContents(box1);
        List<ItemStack> list2 = getConsolidatedContents(box2);
        if (list1.size() != list2.size()) return false;

        for (ItemStack s1 : list1) {
            boolean found = false;
            for (ItemStack s2 : list2) {
                if (ItemStack.isSameItemSameComponents(s1, s2) && s1.getCount() == s2.getCount()) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static List<ItemStack> getConsolidatedContents(ItemStack box) {
        ItemStackHandler inv = PackageItem.getContents(box);
        List<ItemStack> consolidated = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            boolean merged = false;
            for (ItemStack existing : consolidated) {
                if (ItemStack.isSameItemSameComponents(stack, existing)) {
                    existing.grow(stack.getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                consolidated.add(stack.copy());
            }
        }
        return consolidated;
    }

    private static boolean isAddressValid(String address,
                                           Set<String> validAddresses,
                                           List<RescueFrogportInfo> rescueFrogports) {
        // Rescue addresses are always valid (prevents re-rescue loops)
        if (address.equals("rescue-*") || address.startsWith("rescue-") || PackageItem.matchAddress(address, "rescue-*")) {
            return true;
        }
        for (RescueFrogportInfo info : rescueFrogports) {
            if (address.equals(info.address())) return true;
        }
        for (String registeredAddress : validAddresses) {
            if (PackageItem.matchAddress(address, registeredAddress)) {
                return true;
            }
        }
        return false;
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

        // Update the package address to the rescue address wildcard
        box.set(AllDataComponents.PACKAGE_ADDRESS, "rescue-*");

        LOGGER.debug("Readdressed conveyor package at {} to rescue wildcard address: rescue-* (nearest was {})",
                conveyor.getBlockPos(), rescue.target.address());

        // Notify the chain conveyor about the changes so client updates model and server updates routing
        conveyor.setChanged();
        BlockState state = conveyor.getBlockState();
        level.sendBlockUpdated(conveyor.getBlockPos(), state, state, 3);
    }

    private record PackageToRescue(PackageOnConveyor poc, RescueFrogportInfo target) {}
}
