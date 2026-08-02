# HELIX Core — Architecture

Version 0.1.0-SNAPSHOT · Lead Architect: Project HELIX

## 1. Purpose and Scope

HELIX Core is the **platform**, not a product feature. It provides the nine
foundational subsystems every future HELIX module will need, and nothing
else. It has **zero knowledge** of UI, AI, Launcher, or Voice concepts — it
does not import them, does not special-case them, and would compile and run
identically if none of them ever existed.

Everything in this repository exists to answer one question well: *how do
independently-developed modules, built over several years by different
people, cooperate safely without becoming a tangled monolith?* The answer is
Clean Architecture applied structurally, not just as a naming convention:

- **Dependencies point in one direction only** — inward, toward interfaces.
- **Every cross-module interaction is either a call through a narrow
  interface obtained from the Service Registry, or an event on the Event
  Bus.** There is no third way. There is no reaching into another module's
  internals, no shared mutable singletons, no reflection-based coupling.
- **Concrete implementations are replaceable without touching callers.**
  Every `-runtime` module could be deleted and rewritten from scratch
  without a single line changing in any consumer, because consumers only
  ever see `-api` types.

## 2. Module Map

```
helix-core/
├── settings.gradle.kts
├── build.gradle.kts                  (shared build conventions)
├── gradle/libs.versions.toml         (version catalog)
│
├── helix-logging-api/      + helix-logging-runtime/
├── helix-eventbus-api/     + helix-eventbus-runtime/
├── helix-config-api/       + helix-config-runtime/
├── helix-security-api/     + helix-security-runtime/
├── helix-storage-api/      + helix-storage-runtime/
├── helix-scheduler-api/    + helix-scheduler-runtime/
├── helix-registry-api/     + helix-registry-runtime/
├── helix-lifecycle-api/    + helix-lifecycle-runtime/
├── helix-plugin-api/       + helix-plugin-runtime/
│
└── helix-platform/                   (composition root — the ONLY module
                                        that sees every -runtime module)
```

**Rule:** an `-api` module may depend on other `-api` modules only. A
`-runtime` module may depend on its own `-api` module plus any other `-api`
modules it needs to *call* (never another module's `-runtime`). Only
`helix-platform` is permitted to depend on `-runtime` modules, and it does so
purely to wire default implementations behind interfaces — it never exposes
a runtime type publicly (see `HelixCore.kt`: every public field is typed as
an `-api` interface).

Future feature modules (`helix-launcher`, `helix-voice`, `helix-ai`,
`helix-automation`, `helix-vision`, `helix-memory`) will live in **separate
repositories or separate top-level modules that depend on `helix-platform`
only**, and integrate exclusively by implementing `HelixPlugin` and calling
`PluginManager.loadPlugin`. This is what makes the "Core must not depend on
UI/AI/Launcher/Voice" requirement true by construction rather than by
discipline.

## 3. Dependency Graph

```
                          ┌──────────────────┐
                          │  logging-api      │  (zero deps — the floor)
                          └────────▲──────────┘
                                   │
                          ┌────────┴──────────┐
                          │  eventbus-api      │
                          └────────▲──────────┘
                    ┌──────────────┼───────────────┬───────────────┐
                    │              │               │               │
           ┌────────┴──────┐┌─────┴──────┐┌────────┴───────┐┌──────┴───────┐
           │ config-api     ││security-api││  storage-api    ││scheduler-api │
           └────────┬──────┘└─────┬──────┘└────────┬───────┘└──────┬───────┘
                    │              │               │               │
                    └──────────────┴───────┬───────┴───────────────┘
                                            │
                                   ┌────────┴──────────┐
                                   │  registry-api      │
                                   └────────▲──────────┘
                                            │
                                   ┌────────┴──────────┐
                                   │  lifecycle-api     │◄── depends on
                                   └────────▲──────────┘    config-api,
                                            │                registry-api,
                                   ┌────────┴──────────┐    eventbus-api,
                                   │  plugin-api        │    logging-api
                                   └────────▲──────────┘
                                            │
                                   ┌────────┴──────────┐
                                   │  helix-platform    │  (composition root;
                                   │  (HelixCore facade)│   also depends on
                                   └────────▲──────────┘   every -runtime)
                                            │
                     ┌──────────────────────┼──────────────────────┐
                     │                      │                      │
             ┌───────┴──────┐      ┌────────┴───────┐      ┌───────┴──────┐
             │ helix-launcher│      │  helix-voice   │      │  helix-ai    │
             │  (future)     │      │   (future)     │      │  (future)    │
             └───────────────┘      └────────────────┘      └──────────────┘
```

Arrows point from dependent → dependency (standard "depends on" direction).
Note the graph is a strict DAG: `logging` has no incoming platform
dependencies, and nothing below `helix-platform` ever points back up to it.
This is enforced both structurally (Gradle module deps) and by the
`explicitApi()` compiler flag applied to every subproject, which fails the
build if an `-api` module accidentally leaks an internal type.

## 4. Runtime Communication Model

```
┌─────────────────────────────────────────────────────────────────────┐
│                            HelixCore                                 │
│                                                                        │
│   ┌───────────────┐        publish/subscribe        ┌─────────────┐ │
│   │  Component A   │ ───────────────────────────────►│  Event Bus  │ │
│   │ (e.g. Launcher)│◄───────────────────────────────  │             │ │
│   └───────┬───────┘                                   └──────┬──────┘ │
│           │ resolve<T>()                                     │        │
│           ▼                                                  ▼        │
│   ┌───────────────┐                                  ┌─────────────┐ │
│   │Service Registry│◄─────────── register<T>() ───────│ Component B │ │
│   └───────────────┘                                  │(e.g. Voice) │ │
│                                                        └─────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

Two, and only two, sanctioned communication paths:

1. **Direct call through a resolved interface** — component A asks the
   `ServiceRegistry` for a `T::class` service and calls it synchronously.
   Used when A genuinely needs a return value or a strict call contract.
2. **Event on the `EventBus`** — component A publishes a `HelixEvent`;
   zero or more subscribers react. Used for "something happened," fire-and
   -forget notifications, and anything where the publisher shouldn't know
   or care who (if anyone) is listening.

No module is ever handed a direct reference to another module's concrete
class. `LifecycleContext` and `PluginContext` are the only vehicles for
reaching the platform, and both expose interfaces exclusively.

## 5. Lifecycle Contracts

### 5.1 State Machine

```
   CREATED
      │  onInit(context)
      ▼
  INITIALIZING ──(throws)──► FAILED
      │
      ▼
  INITIALIZED
      │  onStart()
      ▼
   STARTING ──(throws)──► FAILED
      │
      ▼
   RUNNING
      │  onStop()
      ▼
   STOPPING ──(throws)──► FAILED
      │
      ▼
   STOPPED ──► (onStart() again: back to STARTING — restart is legal)
      │
      │  onDestroy()
      ▼
   DESTROYED   (terminal — no further transitions)
```

### 5.2 Contract Rules

1. `onInit` is called **exactly once**, before any other method, and is
   handed the platform's `LifecycleContext`. Components must not perform
   work that has side effects outside their own state during `onInit` —
   registration with other systems belongs in `onStart`.
2. `onStart`/`onStop` may be called **multiple times** (restart is a
   supported use case: STOPPED → STARTING is legal). `onInit` and
   `onDestroy` are each called **at most once**.
3. `onStop` and `onDestroy` **must be safe to call on a partially
   initialized or already-failed component.** `LifecycleManager` always
   attempts to stop/destroy every component during shutdown, regardless of
   whether earlier components failed, to avoid resource leaks.
4. **A thrown exception never aborts the whole boot/shutdown sequence.** The
   offending component transitions to `FAILED`, a `ComponentFailedEvent` is
   published, and `LifecycleManager` proceeds to the next component in
   order. Callers that need fail-fast semantics subscribe to
   `ComponentFailedEvent` and decide for themselves.
5. **Ordering is derived, not declared by position.** Components declare
   `dependsOn` by name; `LifecycleManager` topologically sorts
   (Kahn's algorithm) and runs `onInit`/`onStart` in that order and
   `onStop`/`onDestroy` in exact reverse. A cycle is a startup-time fatal
   error with the offending component names in the message.
6. Use `BaseLifecycleComponent` (in `helix-lifecycle-runtime`) unless a
   component has a specific reason to implement `LifecycleAware` directly —
   it enforces rules 1–3 for you via guarded state transitions.

### 5.3 Plugin Lifecycle (feature modules)

`HelixPlugin` mirrors this same contract at the integration boundary:
`onLoad(PluginContext)` / `onUnload()`. `PluginManager` additionally
validates that every plugin named in `descriptor.dependsOn` is already
loaded before calling `onLoad`, and isolates exceptions the same way
`LifecycleManager` does — a broken Voice Engine plugin cannot prevent
Launcher from loading.

## 6. Subsystem Responsibilities

| Subsystem | api module | Responsibility | Default runtime |
|---|---|---|---|
| Logging | `helix-logging-api` | Structured, level-filtered logging via pluggable sinks | `DefaultLoggerFactory` + `ConsoleLogSink` |
| Event Bus | `helix-eventbus-api` | Type- and topic-based pub/sub, sync + async dispatch, isolated handler failures | `DefaultEventBus` |
| Config | `helix-config-api` | Merges prioritized `ConfigSource`s into typed reads; emits change events | `DefaultConfigManager` (+ `EnvConfigSource`, `PropertiesFileConfigSource`) |
| Security | `helix-security-api` | Principal/permission model, deny-overrides policy evaluation, ambient security context | `DefaultSecurityManager` |
| Storage | `helix-storage-api` | Namespaced key/value persistence behind swappable providers | `DefaultStorageManager` + `InMemoryStorageProvider` |
| Scheduler | `helix-scheduler-api` | Deferred/repeating task execution, isolated task failures | `DefaultScheduler` |
| Service Registry | `helix-registry-api` | Type+name keyed directory of platform services (Dependency Inversion) | `DefaultServiceRegistry` |
| Lifecycle Manager | `helix-lifecycle-api` | Dependency-ordered init/start/stop/destroy orchestration | `DefaultLifecycleManager` |
| Plugin Manager | `helix-plugin-api` | Load/unload of feature modules through one narrow seam | `DefaultPluginManager` |

## 7. Composition Root

`helix-platform`'s `HelixCoreBuilder` is the single place default
implementations are chosen. Every subsystem can be overridden independently:

```kotlin
val core = HelixCoreBuilder()
    .withStorageManager { lf -> PostgresStorageManager(lf, dataSource) }
    .withScheduler { eb, lf -> QuartzScheduler(eb, lf) }
    .build()

core.start()   // initAll() -> startAll(), in dependency order
// ... platform runs ...
core.stop()    // stopAll() -> destroyAll(), reverse order, then scheduler.shutdown()
```

This satisfies the Open/Closed Principle at the platform level: extending
HELIX Core's capabilities (a new storage backend, a distributed event bus,
etc.) never requires modifying `helix-platform`'s source — only supplying a
new implementation of an existing `-api` interface.

## 8. Why No Feature Modules Yet

Per the current phase's scope, `helix-platform` deliberately does **not**
include a Launcher, Voice Engine, AI Engine, Automation, Vision, or Memory
module. Adding one before Core's contracts (Lifecycle, Registry, Event Bus,
Config, Logging, Plugin, Scheduler, Storage, Security) are stable would risk
those contracts being shaped by one feature's needs rather than by general
platform requirements. The `helix-plugin-*` modules exist specifically so
that, when those modules are ready, they attach without a single change to
Core.
