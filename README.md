# HELIX Core

A production-quality, modular platform foundation written in Kotlin, built
on Clean Architecture and SOLID principles. HELIX Core provides the nine
subsystems every future HELIX module (Launcher, Voice Engine, AI Engine,
Automation, Vision, Memory) will build on — and depends on **none** of
them.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full module map,
dependency graph, communication model, and lifecycle contracts, and
[`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md) for how to
contribute consistently.

## What's here

| Subsystem | Purpose |
|---|---|
| Lifecycle Manager | Dependency-ordered init/start/stop/destroy for every platform component |
| Service Registry | Type-safe directory of platform services (Dependency Inversion) |
| Event Bus | Type- and topic-based pub/sub — the primary decoupling mechanism |
| Configuration Manager | Merges prioritized config sources, typed reads, change notifications |
| Logging Framework | Pluggable-sink structured logging |
| Plugin Manager | The one sanctioned integration seam for future feature modules |
| Scheduler | Deferred / repeating task execution abstraction |
| Storage Abstraction | Namespaced key/value persistence behind swappable providers |
| Security Abstraction | Principal/permission model with pluggable authorization policies |

Each subsystem is split into an `-api` module (pure contracts) and a
`-runtime` module (default implementation), so any subsystem can be
swapped out in a specific deployment without touching a single consumer.
`helix-platform` is the composition root that assembles the default set
into a single `HelixCore` facade.

## What's deliberately NOT here

No Launcher, Voice, AI, Automation, Vision, or Memory code. No application
features. This phase builds the stable floor those modules will stand on —
see §8 of the architecture doc for why that ordering matters.

## Building

```bash
./gradlew build
```

## Quick start

```kotlin
import com.helix.platform.HelixCoreBuilder
import com.helix.core.lifecycle.api.LifecycleAware
import com.helix.core.lifecycle.api.LifecycleContext
import com.helix.core.lifecycle.api.LifecycleState

class MyComponent : LifecycleAware {
    override var state = LifecycleState.CREATED
    override fun onInit(context: LifecycleContext) { /* ... */ }
    override fun onStart() { /* ... */ }
    override fun onStop() { /* ... */ }
    override fun onDestroy() { /* ... */ }
}

fun main() {
    val core = HelixCoreBuilder().build()
    core.lifecycleManager.register("my-component", MyComponent())
    core.start()
    // ... platform is running ...
    core.stop()
}
```

## Extending HELIX Core (for future feature modules)

Don't. Instead, implement `com.helix.core.plugin.api.HelixPlugin` in your
own module, depend on `helix-platform` only, and call
`core.pluginManager.loadPlugin(myPlugin)`. This is the only supported
integration path — see `docs/ARCHITECTURE.md` §5.3.
