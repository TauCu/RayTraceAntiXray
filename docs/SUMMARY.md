# RayTraceAntiXray - Complete Analysis Summary

## Document Overview

This comprehensive analysis documents the **RayTraceAntiXray** Paper Spigot plugin, including:

- **ARCHITECTURE_DOCUMENTATION.md** - Main architecture, components, lifecycle
- **FLOW_DIAGRAMS.md** - Detailed sequence and flow diagrams with Mermaid
- **REFERENCE_GUIDE.md** - API reference, configuration, entrypoints
- **ALGORITHMS.md** - Mathematical foundations, algorithm explanations
- **THIS FILE** - Quick reference summary

---

## Quick Start Guides

### For Server Admins

1. **Installation**
   - Ensure Paper Anti-Xray is enabled with `engine-mode: 1`
   - Drop plugin JAR in `/plugins` directory
   - Restart server or use `/reload`

2. **Configuration** (config.yml)
   - Adjust `ray-trace-threads` based on CPU cores
   - Set `ray-trace-distance` to view distance (default 120)
   - Enable/disable per world in `world-settings`

3. **Monitoring**
   - Use `/raytraceantixray timings on` to see performance
   - Watch for lag spikes in timing output
   - Adjust settings if timing > ms-per-ray-trace-tick

### For Developers

1. **Plugin Hook Points**
   - `PlayerJoinEvent` (LOWEST) - Data initialization
   - `PlayerQuitEvent` - Data cleanup
   - `WorldInitEvent` - Controller installation
   - `/raytraceantixray reload` - Config reload

2. **Custom Integration**
   ```java
   RayTraceAntiXray plugin = getServer().getPluginManager()
       .getPlugin("RayTraceAntiXray");
   if (plugin.isRunning()) {
       PlayerData data = plugin.getPlayerData().get(playerUUID);
       // Access ray trace data
   }
   ```

3. **Performance Tuning**
   - See REFERENCE_GUIDE.md "Performance Tuning Checklist"
   - See ALGORITHMS.md "Performance Analysis"

---

## Core Concepts

### What It Does

**Problem:** Minecraft X-ray clients reveal all ore blocks regardless of occlusion
**Solution:** Use server-side ray tracing to:
1. Initially hide all ores with stone (via Paper Anti-Xray)
2. Ray trace to determine which ores player can actually see
3. Reveal only the visible ores to that specific client
4. As player moves, dynamically update visible blocks

### How It Works - High Level

```
Client connects
  ↓
DuplexPacketHandler intercepts chunk packets
  ↓
ChunkPacketBlockControllerAntiXray obfuscates (hides ores)
  ↓
DuplexPacketHandler caches chunk data
  ↓
RayTraceTimerTask schedules ray trace on worker threads
  ↓
RayTraceCallable ray traces each player's visible blocks
  ↓
Results queued
  ↓
UpdateBukkitRunnable applies results on main thread
  ↓
Block updates sent to client (reveal visible ores)
```

### Key Insight: Per-Player Updates

Each player gets custom block updates based on their position and camera angle. Players on opposite sides of a wall see different ore blocks revealed.

---

## Architecture Summary

### Thread Model

```
Main Thread (Bukkit)
  ├─ Event handlers
  ├─ DuplexPacketHandler.write()
  ├─ UpdateBukkitRunnable results
  └─ Command execution

Timer Thread (Daemon)
  ├─ Schedule ray trace ticks
  └─ invokeAll() barrier sync

Worker Threads (N, from config)
  ├─ RayTraceCallable execution
  ├─ BlockIterator ray traversal
  └─ BlockOcclusionCulling visibility
```

### Data Flow

```
Chunk Packet
  ↓
DuplexPacketHandler (intercept)
  ↓
ChunkPacketBlockControllerAntiXray (obfuscate)
  ↓
ChunkBlocks cache (weak-key)
  ↓
RayTraceCallable (per player)
  ↓
Result queue (per player)
  ↓
UpdateBukkitRunnable (main thread)
  ↓
ClientboundBlockUpdatePacket
  ↓
Player Client
```

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| **RayTraceAntiXray** | Main plugin class | Lifecycle, config, threading |
| **DuplexPacketHandler** | Network handler | Packet interception |
| **ChunkPacketBlockControllerAntiXray** | Obfuscation engine | Hide blocks initially |
| **RayTraceCallable** | Ray trace algorithm | Determine visibility |
| **BlockIterator** | DDA algorithm | Traverse ray through blocks |
| **BlockOcclusionCulling** | Visibility tester | Test if ray is occluded |
| **UpdateBukkitRunnable** | Result applicator | Send updates to clients |

---

## Configuration

### Essential Settings

```yaml
settings:
  anti-xray:
    ray-trace-threads: 2              # CPU parallelism
    ms-per-ray-trace-tick: 50         # Time budget

world-settings:
  default:
    anti-xray:
      ray-trace: true                 # Enable/disable
      ray-trace-distance: 120.0       # View distance
      max-ray-trace-block-count-per-chunk: 100
```

### Recommended Profiles

**Low CPU:** 1 thread, 100ms budget, 50 block distance
**Typical:** 2 threads, 50ms budget, 120 block distance
**High CPU:** 4+ threads, 30ms budget, 150 block distance

---

## Permissions

```
raytraceantixray.command.raytraceantixray
  ├─ timings on/off (control profiling)
paper.antixray.bypass
  └─ Skip all obfuscation (bypass anti-xray entirely)
```

---

## Commands

```
/raytraceantixray timings [on|off]
  Enable/disable console profiling output

/raytraceantixray reload
  Reload configuration without restart

/raytraceantixray reloadchunks [*|player1 player2...]
  Force chunk reload for specific players
```

---

## File Structure

```
RayTraceAntiXray/
├── src/main/java/com/vanillage/raytraceantixray/
│   ├── RayTraceAntiXray.java          [359 lines] Main plugin
│   ├── antixray/
│   │   ├── ChunkPacketBlockControllerAntiXray.java [1165] Obfuscation
│   │   └── ChunkPacketInfoAntiXray.java
│   ├── listeners/
│   │   ├── PlayerListener.java        [~50] Player events
│   │   └── WorldListener.java         [~80] World events
│   ├── tasks/
│   │   ├── RayTraceCallable.java      [413] Ray tracing
│   │   ├── RayTraceTimerTask.java     [~60] Scheduling
│   │   └── UpdateBukkitRunnable.java  [~50] Result application
│   ├── net/
│   │   ├── DuplexPacketHandler.java   [143] Packet interception
│   │   └── DuplexHandler.java         [~50] Base handler
│   ├── data/
│   │   ├── PlayerData.java            [~60] Player container
│   │   ├── ChunkBlocks.java           [~30] Chunk container
│   │   ├── Result.java                [~20] Result object
│   │   ├── VectorialLocation.java     [~30] Camera position
│   │   └── [Wrappers].java
│   ├── util/
│   │   ├── BlockIterator.java         [217] DDA algorithm
│   │   ├── BlockOcclusionCulling.java [239] Visibility test
│   │   ├── BukkitUtil.java
│   │   ├── NetworkUtil.java
│   │   └── [Utilities].java
│   └── commands/
│       └── RayTraceAntiXrayTabExecutor.java [175] Commands
├── src/main/resources/
│   ├── plugin.yml                     Plugin metadata
│   ├── config.yml                     Configuration
│   └── README.txt                     User guide
├── build.gradle.kts                   Build system
└── README.md                          Project readme
```

---

## Key Algorithms

### 1. Digital Differential Analyzer (DDA)

**File:** BlockIterator.java

Efficiently traverse a 3D grid along a ray:
- Used by ray traversal engine
- O(d) time complexity where d = distance
- Handles all 8 octants
- No floating-point math in loop

**Reference:** Amanatides & Woo (1987)

### 2. Block Occlusion Culling

**File:** BlockOcclusionCulling.java

Test if a block is visible from camera:
- Traverse ray using DDA
- Check solid blocks for occlusion
- Handle edge cases (rays near block edges)
- Return visibility boolean

### 3. Ray Tracing

**File:** RayTraceCallable.java

For each player and chunk:
1. Iterate hidden blocks
2. Calculate distance
3. Test visibility (up to 3 camera angles)
4. Queue visibility changes
5. Update cache

### 4. Chunk Obfuscation

**File:** ChunkPacketBlockControllerAntiXray.java

Layer-by-layer obfuscation:
1. Read original block data
2. Determine which blocks are exposed
3. Replace with stone/deepslate
4. Track hidden blocks for ray tracing
5. Write back to packet

---

## Performance Characteristics

### Time Complexity

```
Per ray trace tick:
  Setup: O(1)
  Chunk iteration: O(C) where C ≈ 64
  Block iteration: O(B) where B ≈ 100-1000
  Visibility test: O(D) where D ≈ 120 (ray distance)
  Total: O(C × B × D) ≈ 100K-300K block checks

Multi-threaded:
  20 players / 2 threads ≈ 10 players per thread
  Total: O(10 × 100K-300K) per tick
```

### Space Complexity

```
Per player: ≈ 20KB
Per worker thread: ≈ 150KB (ThreadLocal arrays)
Per chunk: ≈ 1KB (+ weak reference)

Total for 20 players, 2 threads:
  ≈ 400KB + 300KB + 1.3MB = ≈ 2MB
```

### Optimization Techniques

1. **Weak References** - Automatic cache cleanup when chunks unload
2. **ThreadLocal** - Zero-contention per-thread storage
3. **ConcurrentCollections** - Lock-free data structures
4. **Distance Limits** - Skip rays beyond rayTraceDistance
5. **Block Iterator Cache** - Reuse traversal objects
6. **Async Processing** - Offload from main thread

---

## Threading Model

### Thread Synchronization

```
Main Thread
  ├─ Send chunk packets (DuplexPacketHandler)
  ├─ Schedule ray traces (RayTraceTimerTask)
  └─ Apply results (UpdateBukkitRunnable)

Timer Thread
  └─ invokeAll() ← synchronization barrier
       │
       ├─ Worker 1: Player A ray trace
       ├─ Worker 2: Player B, C, D ray trace
       └─ Worker N: Player X, Y ray trace
       
Once all complete:
  → Results ready
  → Main thread applies
```

### Race Condition Handling

**World Change During Async Tracing:**
- DuplexPacketHandler detects world mismatch
- Discards results for old world

**Chunk Unload During Tracing:**
- WeakReference.get() returns null
- Ray trace safely skips

**Player Data Update:**
- volatile VectorialLocation[] ensures visibility
- Worst case: one tick of stale position (acceptable)

---

## Common Scenarios

### Scenario 1: Player Joins Server

1. `PlayerJoinEvent` (LOWEST priority)
2. Create `PlayerData` with eye location
3. Create `RayTraceCallable`
4. Create `DuplexPacketHandler` and attach to channel
5. Store in `playerData` map
6. Next chunk packet triggers ray trace

### Scenario 2: Chunk Loads

1. Server creates chunk packet
2. `DuplexPacketHandler.write()` intercepts
3. `ChunkPacketBlockControllerAntiXray.obfuscate()` hides ores
4. Cache created with hidden blocks
5. `PlayerData.chunks` updated
6. Next ray trace iteration processes new chunk

### Scenario 3: Player Moves

1. Player eye position updates
2. Next `RayTraceTimerTask` tick:
   - `getLocations()` returns new position
   - `rayTrace()` uses new position
   - Different blocks now visible
   - Results queued
3. `UpdateBukkitRunnable` applies changes
4. Client sees blocks revealed/hidden dynamically

### Scenario 4: Server Shutdown

1. `onDisable()` called
2. Nested try-finally ensures cleanup:
   - Detach all packet handlers
   - Cancel timer
   - Shutdown executor service
   - Restore original controllers
   - Clear caches
3. Plugin fully disabled

---

## Monitoring & Debugging

### Enable Performance Profiling

```bash
/raytraceantixray timings on
# Console output every second:
# "123.456 ms avg per raytrace tick."
```

### Check if Plugin Active

```java
RayTraceAntiXray plugin = (RayTraceAntiXray) Bukkit
    .getPluginManager().getPlugin("RayTraceAntiXray");
if (plugin != null && plugin.isRunning()) {
    // Plugin is active
    boolean enabled = plugin.isEnabled(world);
}
```

### Common Issues

| Issue | Check |
|-------|-------|
| Blocks not hiding | Paper Anti-Xray `engine-mode: 1` enabled? |
| Blocks not revealing | `ray-trace: true` in config? |
| High CPU usage | Reduce threads, increase time budget |
| Memory leak | Weak references should auto-cleanup |

---

## Integration with Paper API

### ChunkPacketBlockController

Implemented by `ChunkPacketBlockControllerAntiXray`:
- `getPresetBlockStates()` - Returns stone variants
- `shouldModify()` - Checks bypass permission
- `getChunkPacketInfo()` - Creates async processor
- `modifyBlocks()` - Main obfuscation call

### Reflection Usage

Used to replace controller (no public API):
```java
Field field = Level.class.getDeclaredField("chunkPacketBlockController");
field.setAccessible(true);
field.set(serverLevel, newController);
```

### Region Scheduler (Folia Compatible)

For async chunk modification:
```java
Bukkit.getRegionScheduler().execute(plugin, world, x, z, () -> {...});
```

---

## Future Enhancement Ideas

1. **Adaptive Threading** - Dynamically adjust thread count based on load
2. **Block Type Optimization** - Different algorithms for different blocks
3. **Client-side Hints** - Send visibility predictions to reduce packets
4. **Multi-level Caching** - Cache visibility results for repeated positions
5. **GPU Acceleration** - Use GPU for ray tracing (future)
6. **Machine Learning** - Predict visibility for common positions

---

## References & Resources

### Academic Papers

- **Amanatides & Woo (1987)** - "A Fast Voxel Traversal Algorithm for Ray Tracing"
  - DDA algorithm foundation
  - Grid traversal mathematics

### Game Engine References

- **Minecraft Block Rendering** - Block state system, rendering
- **Minecraft Networking** - Chunk packet format
- **Paper API** - Anti-Xray engine, ChunkPacketBlockController

### Optimization Techniques

- **ThreadLocal** - Per-thread data, zero-contention
- **WeakReference** - Automatic cache cleanup
- **ConcurrentCollections** - Lock-free concurrent data structures
- **Netty** - Network event handling

---

## Document Navigation

| Document | Purpose |
|----------|---------|
| **ARCHITECTURE_DOCUMENTATION.md** | Full system design and components |
| **FLOW_DIAGRAMS.md** | Visual flow diagrams and sequences |
| **REFERENCE_GUIDE.md** | API reference and configuration |
| **ALGORITHMS.md** | Algorithm explanations and math |
| **THIS FILE** | Quick reference summary |

---

## Conclusion

**RayTraceAntiXray** is a sophisticated anti-xray plugin that:

✅ Protects against X-ray clients using server-side ray tracing
✅ Scales with configurable threads and distance limits
✅ Maintains thread safety with concurrent collections
✅ Integrates seamlessly with Paper Anti-Xray engine
✅ Dynamically updates visible blocks as players move
✅ Performs per-player visibility calculations

The architecture balances **performance** (async processing, efficient algorithms) with **correctness** (proper synchronization, graceful error handling) to provide effective anti-xray protection without impacting server performance.

---

**Last Updated: January 25, 2026**
> Made with the help of Github Copilot, reviewed by <i>[EuSouVoce](https://github.com/EuSouVoce)</i>
