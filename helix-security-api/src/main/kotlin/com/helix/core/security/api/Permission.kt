package com.helix.core.security.api

/** A single, dot-namespaced capability, e.g. "storage.write", "voice.microphone.access". */
public data class Permission(val name: String)
