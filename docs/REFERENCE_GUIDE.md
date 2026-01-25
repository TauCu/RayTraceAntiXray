# RayTraceAntiXray - Complete Reference & Index

## All Entry Points

### Plugin Lifecycle

| Entrypoint | Class | Method | Trigger | Purpose |
|-----------|-------|--------|---------|---------|
| **Startup** | [RayTraceAntiXray](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/RayTraceAntiXray.java#L53) | `onEnable()` | Server starts, plugin loads | Initialize thread pool, listeners, config |
| **Shutdown** | [RayTraceAntiXray](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/RayTraceAntiXray.java#L97) | `onDisable()` | Server stops | Graceful cleanup, resource release |
| **Reload** | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L137) | `onCommand() [reload]` | Admin runs /raytraceantixray reload | Restart plugin (onDisable → onEnable) |

### Event Handlers

| Event | Handler | File | Priority | Purpose |
|-------|---------|------|----------|---------|
| `PlayerJoinEvent` | [PlayerListener](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/listeners/PlayerListener.java#L22) | PlayerListener.java | **LOWEST** | Initialize player tracking early |
| `PlayerQuitEvent` | [PlayerListener](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/listeners/PlayerListener.java#L35) | PlayerListener.java | NORMAL | Clean up player data |
| `WorldInitEvent` | [WorldListener](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/listeners/WorldListener.java#L26) | WorldListener.java | NORMAL | Install custom chunk controller |
| `WorldUnloadEvent` | [WorldListener](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/listeners/WorldListener.java#L30) | WorldListener.java | NORMAL | Restore original controller |

### Network Packet Interception

| Packet Type | Handler | File | Method | Purpose |
|-------------|---------|------|--------|---------|
| `ClientboundLevelChunkWithLightPacket` | [DuplexPacketHandler](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/net/DuplexPacketHandler.java#L38) | DuplexPacketHandler.java | `handle()` | Cache chunk data, schedule ray trace |
| `ClientboundForgetLevelChunkPacket` | [DuplexPacketHandler](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/net/DuplexPacketHandler.java#L82) | DuplexPacketHandler.java | `handle()` | Remove unloaded chunk from cache |
| `ClientboundRespawnPacket` | [DuplexPacketHandler](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/net/DuplexPacketHandler.java#L89) | DuplexPacketHandler.java | `handle()` | Clear chunks on respawn |

### Command Handlers

| Command | Handler | File | Method | Trigger |
|---------|---------|------|--------|---------|
| `/raytraceantixray timings on` | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L97) | RayTraceAntiXrayTabExecutor.java | `onCommand()` | Player with permission |
| `/raytraceantixray timings off` | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L106) | RayTraceAntiXrayTabExecutor.java | `onCommand()` | Player with permission |
| `/raytraceantixray reload` | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L137) | RayTraceAntiXrayTabExecutor.java | `onCommand()` | Player with permission |
| `/raytraceantixray reloadchunks` | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L147) | RayTraceAntiXrayTabExecutor.java | `onCommand()` | Player with permission |

### Scheduled Tasks

| Task | File | Interval | Thread | Purpose |
|------|------|----------|--------|---------|
| [RayTraceTimerTask](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/tasks/RayTraceTimerTask.java) | RayTraceTimerTask.java | `ms-per-ray-trace-tick` ms | Timer (daemon) | Coordinate ray trace ticks |
| [UpdateBukkitRunnable](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/tasks/UpdateBukkitRunnable.java) | UpdateBukkitRunnable.java | `update-ticks` ticks | Main thread | Apply ray trace results |

---

## Paper Integration Points

### ChunkPacketBlockController Interface Override

**File:** [ChunkPacketBlockControllerAntiXray.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/antixray/ChunkPacketBlockControllerAntiXray.java)

```java
// Override these Paper interface methods:

BlockState[] getPresetBlockStates(Level level, ChunkPos chunkPos, int chunkSectionY)
  → Returns stone/deepslate for obfuscation

boolean shouldModify(ServerPlayer player, LevelChunk chunk)
  → Checks paper.antixray.bypass permission

ChunkPacketInfoAntiXray getChunkPacketInfo(
    ClientboundLevelChunkWithLightPacket chunkPacket,
    LevelChunk chunk)
  → Creates ChunkPacketInfoAntiXray for processing

void modifyBlocks(ClientboundLevelChunkWithLightPacket chunkPacket,
    ChunkPacketInfo<BlockState> chunkPacketInfo)
  → Main obfuscation entry point
```

### Reflection Usage

```java
// No public API for this, requires reflection:
Field field = Level.class.getDeclaredField("chunkPacketBlockController");
field.setAccessible(true);
field.set(serverLevel, newController);  // Replace with custom implementation
```

### Region Scheduler (Folia Compatibility)

```java
// For async chunk modification:
Bukkit.getRegionScheduler().execute(
    plugin,
    chunk.getLevel().getWorld(),
    x, z,
    () -> modifyBlocks(chunkPacket, chunkPacketInfo)
);
```

---

## Permissions Reference

### Permission Hierarchy

```
raytraceantixray.*
├── raytraceantixray.command.*
│   ├── raytraceantixray.command.raytraceantixray
│   │   └── raytraceantixray.command.raytraceantixray.timings
│   │       ├── raytraceantixray.command.raytraceantixray.timings.on
│   │       └── raytraceantixray.command.raytraceantixray.timings.off
│   ├── raytraceantixray.command.reload
│   └── raytraceantixray.command.reloadchunks
└── paper.antixray.bypass  (bypass obfuscation - from Paper)
```

### Permission Details

| Permission | Scope | Checked By | Effect |
|-----------|-------|-----------|--------|
| `raytraceantixray.command.raytraceantixray` | Command | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L62) | Access /raytraceantixray timings |
| `raytraceantixray.command.raytraceantixray.timings.on` | Command | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L97) | Execute timings on |
| `raytraceantixray.command.raytraceantixray.timings.off` | Command | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L106) | Execute timings off |
| `raytraceantixray.command.reload` | Command | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L137) | Reload configuration |
| `raytraceantixray.command.reloadchunks` | Command | [RayTraceAntiXrayTabExecutor](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java#L147) | Reload chunks for players |
| `paper.antixray.bypass` | Anti-Xray | [ChunkPacketBlockControllerAntiXray](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/antixray/ChunkPacketBlockControllerAntiXray.java#L220) | Skip all obfuscation |

---

## Configuration Reference

### Complete Schema

```yaml
# plugin-name: RayTraceAntiXray
# version: 1.17.5
# api-version: 1.21.11

settings:
  anti-xray:
    # Scheduling & Performance
    update-ticks: 2                           # Type: long, Range: 1+
                                               # Bukkit tick interval for result application
                                               # Higher = less frequent updates (less CPU)
                                               # Lower = more responsive (more CPU)
    
    ms-per-ray-trace-tick: 50                 # Type: long, Range: 1+
                                               # Milliseconds per ray trace iteration
                                               # Higher = more time per tick (less lag)
                                               # Lower = tighter budget (more lag risk)
    
    ray-trace-threads: 2                      # Type: int, Range: 1+
                                               # Worker threads for ray tracing
                                               # Higher = more parallelism (more CPU)
                                               # = Recommended: CPU cores / 2

world-settings:
  default:                                    # Fallback for all worlds
    anti-xray:
      # Ray Tracing Control
      ray-trace: true                         # Type: boolean, Default: true
                                               # Enable/disable entire ray tracing
      
      ray-trace-third-person: false           # Type: boolean, Default: false
                                               # Include third-person camera modes
                                               # More expensive (2x the calculation)
      
      ray-trace-distance: 120.0               # Type: double, Range: 0+, Default: 120
                                               # Maximum block distance to ray trace
                                               # Higher = more expensive, detects further
      
      # Block Selection
      max-ray-trace-block-count-per-chunk: 100 # Type: int, Range: 0+, Default: 100
                                               # Maximum hidden blocks per chunk
                                               # Counting from bottom upward
                                               # 0 = no limit
      
      ray-trace-blocks: []                    # Type: List<String>, Default: empty
                                               # Blocks to ray trace
                                               # Empty = use Paper's hidden-blocks
                                               # Example: [diamond_ore, deepslate_diamond_ore]
      
      # Re-hiding Behavior
      rehide-blocks: false                    # Type: boolean, Default: false
                                               # Hide blocks that leave player view
                                               # false = once revealed, stays revealed
                                               # true = hide when out of view
      
      rehide-distance: .inf                   # Type: double, Range: 0+, Default: .inf
                                               # Distance threshold for re-hiding
                                               # .inf = never rehide
                                               # Used only if rehide-blocks: true
      
      bypass-rehide-blocks: []                # Type: List<String>, Default: empty
                                               # Blocks that never get rehidden
                                               # Example: [diamond_ore, structure_block]

  # Example per-world override:
  lobby:
    anti-xray:
      ray-trace: false                        # Disable for this world
      
  pvp-arena:
    anti-xray:
      ray-trace-distance: 30.0                # Shorter distance for PvP
      ray-trace-third-person: true            # Enable third-person
      max-ray-trace-block-count-per-chunk: 20
```

### Setting Fallback Hierarchy

```
Config Loading Order:
1. Check world-settings.<world_name>.anti-xray.<setting>
2. If not found: Check world-settings.default.anti-xray.<setting>
3. If not found: Use hardcoded default

Example:
ray-trace-distance lookup for "mining_world":
  → world-settings.mining_world.anti-xray.ray-trace-distance
    → world-settings.default.anti-xray.ray-trace-distance
      → 120.0 (hardcoded default)
```

### Recommended Configurations

**Conservative (Low-End Servers)**
```yaml
settings:
  anti-xray:
    ray-trace-threads: 1
    ms-per-ray-trace-tick: 100
world-settings:
  default:
    anti-xray:
      ray-trace-distance: 50.0
      max-ray-trace-block-count-per-chunk: 30
      ray-trace-third-person: false
```

**Balanced (Typical Servers)**
```yaml
settings:
  anti-xray:
    ray-trace-threads: 2
    ms-per-ray-trace-tick: 50
world-settings:
  default:
    anti-xray:
      ray-trace-distance: 120.0
      max-ray-trace-block-count-per-chunk: 100
      ray-trace-third-person: false
```

**Aggressive (High-End Servers)**
```yaml
settings:
  anti-xray:
    ray-trace-threads: 4
    ms-per-ray-trace-tick: 30
world-settings:
  default:
    anti-xray:
      ray-trace-distance: 150.0
      max-ray-trace-block-count-per-chunk: 200
      ray-trace-third-person: true
```

---

## Block Types Reference

### Special Block State Handling

```java
// Blocks are NOT obfuscated (always shown):
- air                   // Causes unnecessary updates
- spawner               // Contains data
- barrier               // Creative mode block
- shulker_box          // Contains data
- slime_block           // Affects velocity
- mangrove_roots        // Partial block
- lava (if lavaObscures config) // Optional obscuration
```

### Solid Block Classification

```java
// blockState.isRedstoneConductor() && other checks
// Determines if block occludes visibility

Examples:
- Stone: TRUE (solid, occludes)
- Ore: TRUE (solid, occludes)
- Air: FALSE (transparent)
- Glass: FALSE (transparent)
- Slab: FALSE (doesn't fully occlude)
- Stairs: FALSE (doesn't fully occlude)
```

### Engine Modes (Paper Config)

```yaml
# server.properties / paper-global.yml:
anticheat:
  anti-xray:
    engine-mode: 1  # REQUIRED: HIDE mode
                    # Modes 2 & 3 not compatible with ray tracing
    
    hidden-blocks:  # Paper's list used if config empty
      - diamond_ore
      - deepslate_diamond_ore
      - emerald_ore
      - deepslate_emerald_ore
      - gold_ore
      - deepslate_gold_ore
```

---

## Data Classes Reference

### RayTraceAntiXray (Main Plugin)

**File:** [RayTraceAntiXray.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/RayTraceAntiXray.java)

```java
public final class RayTraceAntiXray extends JavaPlugin {
    // Plugin state
    private volatile boolean running;
    private volatile boolean timingsEnabled;
    
    // Threading
    private ExecutorService executorService;      // Ray trace workers
    private Timer timer;                           // Scheduling
    
    // Data management
    private ConcurrentMap<ClientboundLevelChunkWithLightPacket, ChunkBlocks>
        packetChunkBlocksCache;                   // Weak-key chunk cache
    private ConcurrentMap<UUID, PlayerData> playerData; // Per-player data
    
    // Configuration
    private long updateTicks;                     // Result application interval
    
    // Lifecycle
    public void onEnable() { ... }
    public void onDisable() { ... }
    
    // Public API
    public PlayerData tryCreatePlayerDataFor(Player player) { ... }
    public PlayerData createPlayerDataFor(Player player, Location location) { ... }
    public void reload() { ... }
    public void reloadChunks(Iterable<Player> players) { ... }
    
    // Accessors
    public boolean isRunning() { ... }
    public boolean isEnabled(World world) { ... }
    public ConcurrentMap<UUID, PlayerData> getPlayerData() { ... }
    public ExecutorService getExecutorService() { ... }
}
```

### PlayerData (Per-Player Container)

**File:** [PlayerData.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/PlayerData.java)

```java
public final class PlayerData implements Callable<Object> {
    // Chunk tracking
    private final ConcurrentMap<LongWrapper, ChunkBlocks> chunks;
    
    // Results
    private final Queue<Result> results;
    
    // Camera state
    private volatile VectorialLocation[] locations; // 1 or 3 positions
    
    // Task management
    private Callable<?> callable;                 // RayTraceCallable
    private DuplexPacketHandler packetHandler;    // Network handler
    
    // Callable delegation
    public Object call() throws Exception { return callable.call(); }
}
```

### ChunkBlocks (Per-Chunk Container)

**File:** [ChunkBlocks.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/ChunkBlocks.java)

```java
public final class ChunkBlocks {
    // Chunk reference
    private final Reference<LevelChunk> chunk;    // WeakReference
    
    // Identifier
    private final LongWrapper key;                // ChunkPos as long
    
    // Visibility state
    private final Map<BlockPos, Boolean> blocks;  // true = hidden, false = visible
    
    // Accessors
    public LevelChunk getChunk() { return chunk.get(); }
    public Map<BlockPos, Boolean> getBlocks() { return blocks; }
}
```

### Result (Visibility Update)

**File:** [Result.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/Result.java)

```java
public final class Result {
    private final ChunkBlocks chunkBlocks;        // Which chunk
    private final BlockPos block;                 // Which block
    private final boolean visible;                // true = reveal, false = hide
    
    // Accessors
    public boolean isVisible() { return visible; }
    public BlockPos getBlock() { return block; }
    public ChunkBlocks getChunkBlocks() { return chunkBlocks; }
}
```

### VectorialLocation (Camera Position)

**File:** [VectorialLocation.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/VectorialLocation.java)

```java
public final class VectorialLocation {
    private Vector vector;                        // Position (x, y, z)
    private Vector direction;                     // Camera direction (normalized)
    private World world;                          // World reference
    
    // Accessors
    public Vector getVector() { return vector; }
    public Vector getDirection() { return direction; }
    public World getWorld() { return world; }
}
```

---

## Public API Summary

### Main Plugin Methods

```java
// Lifecycle
RayTraceAntiXray.onEnable()                     // Plugin startup
RayTraceAntiXray.onDisable()                    // Plugin shutdown
RayTraceAntiXray.reload()                       // Config reload

// Player Management
RayTraceAntiXray.tryCreatePlayerDataFor(Player)
  → PlayerData or null (skip NPC)

RayTraceAntiXray.createPlayerDataFor(Player, Location)
  → PlayerData

RayTraceAntiXray.reloadChunks(Iterable<Player>)
  → void (force chunk reload)

// State Query
RayTraceAntiXray.isRunning()
  → boolean

RayTraceAntiXray.isEnabled(World)
  → boolean

RayTraceAntiXray.isTimingsEnabled()
  → boolean

RayTraceAntiXray.setTimingsEnabled(boolean)
  → void

// Data Access
RayTraceAntiXray.getPlayerData()
  → ConcurrentMap<UUID, PlayerData>

RayTraceAntiXray.getPacketChunkBlocksCache()
  → ConcurrentMap<ClientboundLevelChunkWithLightPacket, ChunkBlocks>

RayTraceAntiXray.getExecutorService()
  → ExecutorService

RayTraceAntiXray.getUpdateTicks()
  → long
```

---

## File Index by Category

### Entry Points & Initialization

| File | Lines | Purpose |
|------|-------|---------|
| [RayTraceAntiXray.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/RayTraceAntiXray.java) | 359 | Main plugin class, lifecycle |

### Event Handling

| File | Lines | Purpose |
|------|-------|---------|
| [PlayerListener.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/listeners/PlayerListener.java) | ~50 | PlayerJoin/Quit events |
| [WorldListener.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/listeners/WorldListener.java) | ~80 | WorldInit/Unload events |

### Network & Packets

| File | Lines | Purpose |
|------|-------|---------|
| [DuplexPacketHandler.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/net/DuplexPacketHandler.java) | 143 | Netty packet interception |
| [DuplexHandler.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/util/DuplexHandler.java) | ~50 | Base packet handler |

### Anti-Xray Engine

| File | Lines | Purpose |
|------|-------|---------|
| [ChunkPacketBlockControllerAntiXray.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/antixray/ChunkPacketBlockControllerAntiXray.java) | 1165 | Chunk obfuscation engine |
| [ChunkPacketInfoAntiXray.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/antixray/ChunkPacketInfoAntiXray.java) | ~30 | Packet info container |

### Ray Tracing Core

| File | Lines | Purpose |
|------|-------|---------|
| [RayTraceCallable.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/tasks/RayTraceCallable.java) | 413 | Ray tracing algorithm |
| [BlockIterator.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/util/BlockIterator.java) | 217 | DDA ray traversal |
| [BlockOcclusionCulling.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/util/BlockOcclusionCulling.java) | 239 | Visibility testing |

### Scheduling & Tasks

| File | Lines | Purpose |
|------|-------|---------|
| [RayTraceTimerTask.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/tasks/RayTraceTimerTask.java) | ~60 | Timer-based tick scheduling |
| [UpdateBukkitRunnable.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/tasks/UpdateBukkitRunnable.java) | ~50 | Result application on main thread |

### Data Classes

| File | Lines | Purpose |
|------|-------|---------|
| [PlayerData.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/PlayerData.java) | ~60 | Per-player container |
| [ChunkBlocks.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/ChunkBlocks.java) | ~30 | Per-chunk container |
| [Result.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/Result.java) | ~20 | Visibility update result |
| [VectorialLocation.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/VectorialLocation.java) | ~30 | Camera position & direction |
| [LongWrapper.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/LongWrapper.java) | ~30 | Immutable long wrapper |
| [MutableLongWrapper.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/data/MutableLongWrapper.java) | ~30 | Mutable long wrapper |

### Utilities

| File | Lines | Purpose |
|------|-------|---------|
| [BukkitUtil.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/util/BukkitUtil.java) | ~30 | Bukkit version detection |
| [NetworkUtil.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/util/NetworkUtil.java) | ~30 | Network utilities |
| [TimeFormatter.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/util/TimeFormatter.java) | ~50 | Timing display formatting |
| [TimeSplitter.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/util/TimeSplitter.java) | ~30 | Time interval splitting |

### Commands

| File | Lines | Purpose |
|------|-------|---------|
| [RayTraceAntiXrayTabExecutor.java](RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/commands/RayTraceAntiXrayTabExecutor.java) | 175 | Command execution & tab completion |

### Configuration

| File | Purpose |
|------|---------|
| [plugin.yml](RayTraceAntiXray/src/main/resources/plugin.yml) | Plugin metadata |
| [config.yml](RayTraceAntiXray/src/main/resources/config.yml) | User configuration |
| [README.txt](RayTraceAntiXray/src/main/resources/README.txt) | User documentation |

---

## Thread Safety & Synchronization

### Concurrent Collections Used

```java
ConcurrentHashMap<K, V>
  - PlayerData.chunks
  - PlayerData (itself)
  - Plugin.playerData

ConcurrentMap<K, V> via MapMaker
  - Plugin.packetChunkBlocksCache (WeakKeys)

ConcurrentLinkedQueue<E>
  - PlayerData.results

WeakReference<T>
  - ChunkBlocks.chunk (auto-cleanup)

ThreadLocal<T>
  - ChunkPacketBlockControllerAntiXray.presetBlockStateBits
  - ChunkPacketBlockControllerAntiXray.SOLID
  - ChunkPacketBlockControllerAntiXray.OBFUSCATE
  - ChunkPacketBlockControllerAntiXray.TRACE
  - ChunkPacketBlockControllerAntiXray.BLOCK_ENTITY
  - ChunkPacketBlockControllerAntiXray.CURRENT
  - ChunkPacketBlockControllerAntiXray.NEXT
  - ChunkPacketBlockControllerAntiXray.NEXT_NEXT
  - ChunkPacketBlockControllerAntiXray.TRACE_CACHE
  - ChunkPacketBlockControllerAntiXray.BLOCK_ENTITY_CACHE
```

### Synchronization Patterns

```java
// Memory visibility
volatile boolean running;
volatile boolean timingsEnabled;
volatile VectorialLocation[] locations;  // PlayerData

// Barrier synchronization
ExecutorService.invokeAll()  // RayTraceTimerTask
  - Blocks until ALL tasks complete
  - Provides tick synchronization

// Atomic operations
ConcurrentMap.putIfAbsent()
ConcurrentMap.remove(key, value)
  - Atomic compare-and-swap

// Thread-local isolation
ThreadLocal<T>
  - Each thread has its own instance
  - Eliminates synchronization overhead
```

---

## Performance Tuning Checklist

### CPU Optimization

- [ ] Adjust `ray-trace-threads` to CPU core count / 2
- [ ] Increase `ms-per-ray-trace-tick` for lower CPU usage
- [ ] Reduce `ray-trace-distance` for shorter view range
- [ ] Lower `max-ray-trace-block-count-per-chunk`
- [ ] Disable `ray-trace-third-person` if not needed

### Memory Optimization

- [ ] Monitor `packetChunkBlocksCache` size (weak keys auto-cleanup)
- [ ] Reduce `max-ray-trace-block-count-per-chunk` if memory heavy
- [ ] Keep ThreadLocal arrays properly sized

### Responsiveness Optimization

- [ ] Decrease `update-ticks` for faster result application
- [ ] Decrease `ms-per-ray-trace-tick` for tighter loop
- [ ] Enable `rehide-blocks` for immediate state updates

### Correctness Verification

- [ ] Verify Paper Anti-Xray `engine-mode: 1` enabled
- [ ] Confirm `ray-trace: true` for desired worlds
- [ ] Test `paper.antixray.bypass` permission bypass
- [ ] Validate block list configuration

---

## Debugging & Troubleshooting

### Enabling Timings

```bash
/raytraceantixray timings on
# Console will show:
# "X.XXX ms avg per raytrace tick" (every second)
```

### Checking if Plugin Active

```java
// In code:
RayTraceAntiXray plugin = ...;
if (plugin.isRunning()) {
    // Plugin is active
    PlayerData pd = plugin.getPlayerData().get(playerUUID);
    if (pd != null) {
        // Player is being ray traced
    }
}
```

### Common Issues

**Blocks not hiding**
- Check Paper Anti-Xray `engine-mode: 1` enabled
- Verify `ray-trace: true` in config for world
- Ensure blocks in `ray-trace-blocks` or Paper's `hidden-blocks`

**Blocks not revealing**
- Check `ray-trace-distance` is sufficient
- Verify `rayTraceDistance` in memory (debug)
- Test with `ray-trace-third-person: true`

**High CPU usage**
- Reduce `ray-trace-threads`
- Increase `ms-per-ray-trace-tick`
- Lower `ray-trace-distance`
- Disable `ray-trace-third-person`

**Lag spikes**
- Check timer task completion time
- Verify all worker threads finishing
- Monitor ExecutorService queue

