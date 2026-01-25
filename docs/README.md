# RayTraceAntiXray - Complete Documentation

## 📖 Welcome to Complete Analysis

This folder contains **comprehensive documentation** for the **RayTraceAntiXray** Paper Spigot plugin, analyzing every aspect of its design, architecture, algorithms, and configuration.

---

## 🚀 Start Here

### 👨‍💼 If you're a **Server Administrator**
1. First read: [SUMMARY.md](SUMMARY.md) - "Quick Start Guides" section
2. Then: [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - "Configuration Reference"
3. For issues: [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - "Debugging & Troubleshooting"

### 👨‍💻 If you're a **Plugin Developer**
1. First read: [ARCHITECTURE_DOCUMENTATION.md](ARCHITECTURE_DOCUMENTATION.md) - Overview
2. Then: [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - "All Entry Points"
3. Deep dive: [ALGORITHMS.md](ALGORITHMS.md) - Algorithm details

### 👷 If you're a **System Architect**
1. First read: [ARCHITECTURE_DOCUMENTATION.md](ARCHITECTURE_DOCUMENTATION.md) - Complete
2. Then: [FLOW_DIAGRAMS.md](FLOW_DIAGRAMS.md) - All diagrams
3. Performance: [ALGORITHMS.md](ALGORITHMS.md) - "Performance Analysis"

---

## 📚 All Documentation Files

### [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md) ⭐ **START HERE**
**Master index** - Navigate all documentation, topic cross-reference, getting started paths

### [SUMMARY.md](SUMMARY.md)
**Quick reference** - Overview, key concepts, common scenarios, monitoring

- 15 sections covering plugin basics
- Perfect for quick lookups
- Admin and developer quick starts
- Command reference

### [ARCHITECTURE_DOCUMENTATION.md](ARCHITECTURE_DOCUMENTATION.md)
**System design** - Complete architecture, all components, interactions

- Plugin information & lifecycle
- Events handled (Bukkit & Network)
- 12 major components analyzed
- Threading model explained
- Paper API integration
- 150+ subsections

### [FLOW_DIAGRAMS.md](FLOW_DIAGRAMS.md)
**Visual workflows** - Sequence diagrams and algorithm flows with Mermaid

- Startup sequence
- Player join/quit flow
- Chunk loading & obfuscation
- Ray tracing execution
- 13 detailed diagrams
- 15+ Mermaid flowcharts

### [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md)
**Complete API reference** - Entrypoints, configuration, permissions, data classes

- All entry points (36+)
- Paper API integration
- Permission hierarchy
- Configuration schema
- 100+ subsections
- Debugging checklist

### [ALGORITHMS.md](ALGORITHMS.md)
**Technical deep dive** - Mathematical foundations, algorithm explanations

- Digital Differential Analyzer (DDA)
- Block Occlusion Culling
- Ray Tracing algorithm
- Chunk Obfuscation
- Performance analysis
- Concurrency guarantees

---

## 🎯 Quick Navigation

| **I want to...** | **Read this** |
|---|---|
| Understand the basic design | [SUMMARY.md](SUMMARY.md) or [ARCHITECTURE_DOCUMENTATION.md](ARCHITECTURE_DOCUMENTATION.md) |
| Configure the plugin | [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - Configuration Reference |
| See how things work visually | [FLOW_DIAGRAMS.md](FLOW_DIAGRAMS.md) |
| Learn the algorithms | [ALGORITHMS.md](ALGORITHMS.md) |
| Find all entry points | [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - All Entry Points |
| Understand permissions | [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - Permissions Reference |
| Optimize performance | [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - Performance Tuning Checklist |
| Debug issues | [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - Debugging & Troubleshooting |
| Integrate with API | [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) - Public API Summary |
| Understand threading | [ARCHITECTURE_DOCUMENTATION.md](ARCHITECTURE_DOCUMENTATION.md) - Threading Model |

---

## 🗺️ Plugin Overview

**RayTraceAntiXray** is a sophisticated anti-xray protection plugin that:

1. **Intercepts** chunk packets sent to players
2. **Hides** ore blocks using Paper Anti-Xray
3. **Ray traces** to determine visible ores
4. **Reveals** only ores the player can actually see
5. **Updates dynamically** as players move

### Key Facts

- ✅ **Async processing** - Doesn't block main thread
- ✅ **Multi-threaded** - Configurable worker threads
- ✅ **Per-player** - Custom visibility for each player
- ✅ **Dynamic** - Updates as players move
- ✅ **Efficient** - Uses DDA algorithm for O(d) ray traversal
- ✅ **Safe** - Proper synchronization, no race conditions

---

## 🏗️ Architecture at a Glance

```
┌──────────────────────────────────────┐
│     RayTraceAntiXray Plugin          │
├──────────────────────────────────────┤
│ • Main lifecycle & config management │
│ • Thread pool coordination           │
│ • Player data management            │
└──────────────┬──────────────────────┘
               │
      ┌────────┼────────┐
      │        │        │
      ↓        ↓        ↓
   Events   Network   Threads
   • Join   • Packets  • Workers
   • Quit   • Handler  • Ray trace
   • World  • Cache    • Timer
   • Init
      │        │        │
      └────────┼────────┘
               │
         ┌─────┴─────┐
         │           │
         ↓           ↓
   Obfuscation  Ray Tracing
   • Hide ores   • BlockIterator
   • Track       • Occlusion
   • Replace     • Visibility
         │           │
         └─────┬─────┘
               │
               ↓
         Updates to Client
         • Block updates
         • Per-player
         • Dynamic
```

---

## 📋 Complete File List

### Documentation (5 files)
- ✅ DOCUMENTATION_INDEX.md (this file)
- ✅ SUMMARY.md
- ✅ ARCHITECTURE_DOCUMENTATION.md
- ✅ FLOW_DIAGRAMS.md
- ✅ REFERENCE_GUIDE.md
- ✅ ALGORITHMS.md

### Source Code (23 files)
```
RayTraceAntiXray/src/main/java/com/vanillage/raytraceantixray/
├── RayTraceAntiXray.java (359 lines) - Main plugin
├── antixray/
│   ├── ChunkPacketBlockControllerAntiXray.java (1165 lines) - Core obfuscation
│   └── ChunkPacketInfoAntiXray.java - Packet info container
├── listeners/
│   ├── PlayerListener.java - Join/quit events
│   └── WorldListener.java - World init/unload events
├── tasks/
│   ├── RayTraceCallable.java (413 lines) - Ray trace execution
│   ├── RayTraceTimerTask.java - Scheduling
│   └── UpdateBukkitRunnable.java - Result application
├── net/
│   ├── DuplexPacketHandler.java (143 lines) - Packet interception
│   └── DuplexHandler.java - Base handler
├── data/
│   ├── PlayerData.java - Per-player container
│   ├── ChunkBlocks.java - Per-chunk container
│   ├── Result.java - Visibility result
│   ├── VectorialLocation.java - Camera position
│   ├── LongWrapper.java - Immutable wrapper
│   └── MutableLongWrapper.java - Mutable wrapper
├── util/
│   ├── BlockIterator.java (217 lines) - DDA algorithm
│   ├── BlockOcclusionCulling.java (239 lines) - Visibility test
│   ├── BukkitUtil.java - Version detection
│   ├── NetworkUtil.java - Network helpers
│   ├── TimeFormatter.java - Timing display
│   └── TimeSplitter.java - Time utilities
└── commands/
    └── RayTraceAntiXrayTabExecutor.java (175 lines) - Commands
```

### Resources
- plugin.yml - Plugin metadata
- config.yml - User configuration

---

## 🔑 Key Concepts

### How It Works (Simplified)

```
1. Player joins
   → Create PlayerData with eye position
   
2. Chunk packet sent
   → DuplexPacketHandler intercepts
   → ChunkPacketBlockControllerAntiXray hides ores with stone
   → Cache chunk data
   
3. Ray trace tick (every 50ms)
   → Worker thread reads cached chunks
   → For each ore: test if visible from player eye
   → Queue visibility results
   
4. Main thread tick (every 2 Bukkit ticks)
   → Apply visibility results
   → Send block updates to player
   
5. Player moves
   → Eye position updates
   → Next ray trace uses new position
   → Visibility recalculates
   → Different ores revealed/hidden
```

### Key Algorithms

1. **DDA (Digital Differential Analyzer)**
   - Efficiently traverse ray through blocks
   - O(d) complexity where d = distance
   - Used for ray traversal

2. **Block Occlusion Culling**
   - Test if block is visible from camera
   - Check if solid blocks occlude ray
   - Handle edge cases

3. **Ray Tracing**
   - For each player: test all hidden blocks
   - Determine visibility
   - Queue updates

4. **Chunk Obfuscation**
   - Layer-by-layer processing
   - Hide exposed ores
   - Track hidden blocks

---

## ⚙️ Configuration Essentials

### Basic Setup

```yaml
settings:
  anti-xray:
    ray-trace-threads: 2           # CPU parallelism
    ms-per-ray-trace-tick: 50      # Time budget

world-settings:
  default:
    anti-xray:
      ray-trace: true              # Enable/disable
      ray-trace-distance: 120.0    # View distance
```

### Performance Profiles

**Low-end:** 1 thread, 100ms, 50 blocks
**Typical:** 2 threads, 50ms, 120 blocks
**High-end:** 4+ threads, 30ms, 150 blocks

---

## 📊 Performance Metrics

```
Ray Tracing Cost:
  Per tick:        ~100K-300K block checks
  For 20 players:  ~2-6M block checks/tick
  CPU impact:      Configurable, async

Memory Usage:
  Per player:      ~20KB
  Per thread:      ~150KB
  20 players:      ~2MB total

Optimization:
  ✓ Weak references (auto-cleanup)
  ✓ ThreadLocal (zero contention)
  ✓ Async processing (main thread safe)
  ✓ DDA algorithm (O(d) traversal)
  ✓ Distance limits (skip far blocks)
```

---

## 🛠️ Getting Help

### If something isn't clear:

1. **For configuration:** See [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md#configuration-reference)
2. **For algorithms:** See [ALGORITHMS.md](ALGORITHMS.md)
3. **For architecture:** See [ARCHITECTURE_DOCUMENTATION.md](ARCHITECTURE_DOCUMENTATION.md)
4. **For flows:** See [FLOW_DIAGRAMS.md](FLOW_DIAGRAMS.md)
5. **For quick answers:** See [SUMMARY.md](SUMMARY.md)

## 📞 Quick Links

| Document | Best For |
|----------|----------|
| [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md) | Navigation |
| [SUMMARY.md](SUMMARY.md) | Quick ref |
| [ARCHITECTURE_DOCUMENTATION.md](ARCHITECTURE_DOCUMENTATION.md) | Design |
| [FLOW_DIAGRAMS.md](FLOW_DIAGRAMS.md) | Visuals |
| [REFERENCE_GUIDE.md](REFERENCE_GUIDE.md) | API/Config |
| [ALGORITHMS.md](ALGORITHMS.md) | Math/Perf |

---

## ✅ What's Documented

- [x] Plugin lifecycle (startup, shutdown, reload)
- [x] Event handling (all Bukkit & network events)
- [x] Packet interception (Netty handlers)
- [x] Ray tracing engine (complete algorithm)
- [x] Block iteration (DDA algorithm)
- [x] Visibility testing (occlusion culling)
- [x] Data structures (all containers)
- [x] Threading model (synchronization)
- [x] Configuration system (all options)
- [x] Commands (all 4 commands)
- [x] Permissions (hierarchy & checks)
- [x] Performance (analysis & tuning)
- [x] Integration points (Paper API)
- [x] Error handling (cleanup & recovery)
- [x] Memory management (weak refs, caching)
- [x] Concurrency (race conditions, guarantees)

---

**Last Updated: January 25, 2026**
> Made with the help of Github Copilot, reviewed by <i>[EuSouVoce](https://github.com/EuSouVoce)</i>

Happy reading! 📚

