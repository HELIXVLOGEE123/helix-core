# HELIX Core — Coding Standards

These standards exist to keep a codebase that will be touched by many
contributors over several years internally consistent. They are enforced
partly by tooling (Gradle conventions in the root `build.gradle.kts`) and
partly by review discipline.

## 1. Module Boundaries (non-negotiable)

- An `-api` module contains **only** interfaces, sealed classes, data
  classes, enums, and functional (`fun interface`) types. No concrete
  service implementation, no `object` singletons holding mutable state, no
  I/O.
- A `-runtime` module implements exactly one `-api` module's contracts. It
  may depend on other `-api` modules to do its job, but never on another
  module's `-runtime`.
- Only `helix-platform` may depend on more than one `-runtime` module. If a
  new module ever seems to need two `-runtime` dependencies, that's a
  signal the functionality belongs in `helix-platform`'s composition root,
  not in the module.
- Every subproject applies Kotlin's `explicitApi()` mode. All public
  declarations require an explicit visibility modifier (`public`,
  `internal`) — this is what stops an implementation detail from leaking
  into a module's public surface unnoticed.

## 2. Interfaces Over Implementations

- Consumers type against `-api` interfaces exclusively:
  `ServiceRegistry`, not `DefaultServiceRegistry`; `EventBus`, not
  `DefaultEventBus`.
- Concrete implementation classes are `public` (so `helix-platform` can
  construct them) but are never imported outside their own `-runtime`
  module and `helix-platform`'s builder.
- Constructors of runtime implementations take their dependencies as
  interface-typed parameters (constructor injection). No implementation
  reaches for a global/static instance of anything.

## 3. Cross-Module Communication

- If module A needs to *ask* module B for something and get a value back:
  A resolves B's interface via `ServiceRegistry` and calls it.
- If module A needs to *announce* something happened, with zero or more
  interested listeners: A publishes a `HelixEvent` on the `EventBus`.
- There is no third pattern. Do not add static accessors, do not pass
  concrete objects between modules through constructors of unrelated
  classes, do not use reflection to reach into another module's internals.

## 4. Event Design

- Every event is an immutable `data class` implementing `HelixEvent`, with
  a `topic` string following the `helix.<subsystem>.<noun>.<verb-past-tense>`
  convention (e.g. `helix.lifecycle.component.started`,
  `helix.config.changed`).
- Events carry only the data needed to react — not entire domain objects —
  to avoid accidentally coupling subscribers to a publisher's internal
  model shape.
- Prefer `publish()` (synchronous) for anything ordering-sensitive at
  startup/shutdown; use `publishAsync()` for high-frequency or
  latency-sensitive notifications where subscriber ordering doesn't matter.

## 5. Error Handling

- Subsystems that dispatch to external/pluggable code (`EventBus` calling
  handlers, `LifecycleManager` calling components, `PluginManager` calling
  plugins, `Scheduler` running tasks) **must** catch `Throwable` at the
  dispatch boundary, log it, and (where an event exists for it) publish a
  `*FailedEvent`. A failure in one module must never propagate into and
  crash an unrelated module.
- Do not use exceptions for expected control flow (e.g. "config key not
  set" returns `null`/a default, not a thrown exception —
  `ServiceNotFoundException` is the deliberate exception here, because
  resolving an unregistered service is a wiring bug, not a normal case).
- Custom exceptions live in the `-api` module they belong to
  (`ServiceNotFoundException`, `StorageException`, etc.) so callers can
  catch them without depending on `-runtime`.

## 6. Concurrency

- Anything reachable from multiple modules concurrently (`ServiceRegistry`,
  `EventBus`, `DefaultLoggerFactory`, `DefaultConfigManager`) is built on
  `java.util.concurrent` primitives (`ConcurrentHashMap`,
  `CopyOnWriteArrayList`, `AtomicReference`) — not `synchronized` blocks
  guarding mutable collections, and not assumed single-threaded.
- Lifecycle transitions themselves (`init` → `start` → `stop` → `destroy`)
  are assumed sequential and are driven by `LifecycleManager` on a single
  calling thread; components must not assume `onStart`/`onStop` are called
  concurrently with each other, but must assume calls into their public
  interface may come from other threads once `RUNNING`.
- Prefer immutable data (`data class`, `val`, persistent collections via
  copy-on-write) over locking wherever throughput allows it.

## 7. Naming Conventions

- Module names: `helix-<subsystem>-api` / `helix-<subsystem>-runtime`.
- Package names mirror module names:
  `com.helix.core.<subsystem>.api` / `com.helix.core.<subsystem>.runtime`.
- Default implementation classes are prefixed `Default` (`DefaultEventBus`,
  `DefaultServiceRegistry`) so it's visually obvious at a glance that a
  swap-in replacement exists and is expected.
- Event classes are suffixed `Event` (`ComponentStartedEvent`,
  `ConfigChangeEvent`).

## 8. Documentation

- Every public interface and every public class in a `-runtime` module
  carries a KDoc comment explaining *why* it exists and any contract
  guarantees (thread-safety, failure isolation, ordering) — not just a
  restatement of its name.
- Non-obvious design decisions (e.g. "deny-overrides" in
  `DefaultSecurityManager`, "Kahn's algorithm" in
  `DefaultLifecycleManager`) are called out explicitly in the class-level
  KDoc so a future maintainer doesn't have to reverse-engineer intent from
  code.

## 9. Testing

- Every `-runtime` module ships its own unit tests exercising its
  implementation in isolation.
- `helix-platform` ships integration/smoke tests (see
  `HelixCoreSmokeTest`) that build a real `HelixCore` via
  `HelixCoreBuilder` with zero mocking, register test components, and
  assert on end-to-end lifecycle/event behavior. Every future feature
  module should include an equivalent smoke test as its baseline
  integration check against Core.
- Favor fakes/stubs implementing the relevant `-api` interface over mocking
  frameworks when testing consumers of a subsystem — since the interfaces
  are small and stable, hand-written fakes are usually clearer and cheaper
  to maintain than mock setup code.

## 10. Versioning and Compatibility

- `-api` modules follow semantic versioning strictly: adding a method to an
  existing interface is a breaking change for anyone who implements it
  (not just callers) and requires a major version bump, or must be added
  as a new interface / default method instead.
- `-runtime` modules can evolve freely between major versions as long as
  their `-api` contract is unchanged — this is the entire point of the
  split.
