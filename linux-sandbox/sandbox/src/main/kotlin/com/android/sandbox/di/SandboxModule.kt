package com.android.sandbox.di

/**
 * This module previously provided Koin DI bindings.
 * The linux-sandbox module is now DI-agnostic and accepts dependencies via constructors.
 *
 * For Hilt integration, use com.android.sandbox.SandboxModule in the feature module
 * that depends on this library.
 *
 * Example usage:
 * ```
 * val sandboxManager = LinuxSandboxManager(context)
 * val processManager = ProcessManager(sandboxManager)
 * val shellCommandTool = ShellCommandTool(sandboxManager, processManager)
 * val processManagerTool = ProcessManagerTool(sandboxManager)
 * val sandboxController = AndroidSandboxController(context)
 * ```
 */
object SandboxModule
