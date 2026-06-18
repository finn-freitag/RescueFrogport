<img src="/icon_create_style.png" alt="Rescue Frogport Icon" width="256" height="256">

# Rescue Frogport

**Rescue Frogport** is a lightweight addon mod for **Create** (NeoForge) that solves package network deadlocks, invalid address delivery failures, and conveyor line clogging. It automatically scans your chain conveyor networks and redirects unroutable or congested packages to dedicated **Rescue Frogports** acting as overflow and dead-letter depots.

---

## Why You Need It

In a complex Create logistics setup, conveyor networks can easily grind to a halt due to:
* **Dead Letter Buildup**: Players removing or renaming destination Frogports, leaving active packages circling the network forever.
* **Unaddressed Cargo**: Misconfigured packagers producing packages without addresses that fill up conveyor links.
* **System Congestion**: A sudden surge of identical cargo piling up on a single section of track, locking up all other traffic.

**Rescue Frogport** resolves these issues natively. It detects loops, links, and sprockets, monitors package states, and safely routes problem cargo out of the main lines into dedicated 18-slot rescue ports (can be emptied like normal frogports).

---

## The Three Rescue Rules
Each Rescue Frogport scans its connected conveyor network every second (configurable) and applies three rescue procedures:
1. **Unaddressed Packages**: Any package traversing the network without an address is automatically readdressed and routed to the nearest Rescue Frogport.
2. **Invalid Address Rescue**: If a package is addressed to a destination that does not exist or is no longer connected to the conveyor network, it is routed to the nearest Rescue Frogport. (Fully supports wildcard matching, e.g., packages addressed to `"target15"` will *not* be rescued if a wildcard receiver like `"target*"` is present).
3. **Congestion & Clog Control**: If more than a configured threshold (default: 10) of identical packages (same address and same consolidated contents, ignoring box style/color and unique order IDs) queue up on a single conveyor section (sprocket loop or link), the excess packages are readdressed and routed to the nearest Rescue Frogport.

---

## Crafting

The Rescue Frogport can be crafted via a shapeless crafting recipe:

| Input | Output |
| :--- | :--- |
| 1x Create Package Frogport (`create:package_frogport`) + 1x Create Electron Tube (`create:electron_tube`) | 1x **Rescue Frogport** |

---

## Configuration

Settings can be customized server-side in your world's config folder under `config/rescuefrogport-server.toml`:

```toml
[server]
	# Maximum number of identical packages (same contents and same address)
	# allowed in a single chain conveyor section before excess packages are rescued.
	# Range: 1 ~ 1000 (Default: 10)
	congestionThreshold = 10

	# How often (in game ticks) each rescue frogport scans its chain conveyor network.
	# 20 ticks = 1 second. Lower values are more responsive but use more CPU.
	# Range: 1 ~ 200 (Default: 20)
	scanIntervalTicks = 20
```

---

## License
This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

## Contributing
Contributions are welcome! Feel free to open issues or pull requests.

## Asset & Texture Licensing Notice

All original textures, models, and assets of the Package Frogport and Electron Tube are the sole property of the **Create Mod Team** (All Rights Reserved). 

This mod **does not package, copy, or redistribute** any assets from the Create Mod. All visual components (such as the base frog model and electron tube textures) are referenced dynamically and loaded from your local, original Create Mod JAR file at runtime.
