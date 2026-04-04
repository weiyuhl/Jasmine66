package com.android.sandbox.core

import org.junit.Assert.*
import org.junit.Test

class SandboxStateTest {

    @Test
    fun `NotInstalled state is singleton`() {
        val state1 = SandboxState.NotInstalled
        val state2 = SandboxState.NotInstalled
        assertSame(state1, state2)
    }

    @Test
    fun `Ready state is singleton`() {
        val state1 = SandboxState.Ready
        val state2 = SandboxState.Ready
        assertSame(state1, state2)
    }

    @Test
    fun `Extracting state is singleton`() {
        val state1 = SandboxState.Extracting
        val state2 = SandboxState.Extracting
        assertSame(state1, state2)
    }

    @Test
    fun `Downloading state holds progress`() {
        val state = SandboxState.Downloading(0.5f)
        assertEquals(0.5f, state.progress, 0.001f)
    }

    @Test
    fun `Installing state holds detail message`() {
        val state = SandboxState.Installing("Installing bash...")
        assertEquals("Installing bash...", state.detail)
    }

    @Test
    fun `Installing state has empty detail by default`() {
        val state = SandboxState.Installing()
        assertEquals("", state.detail)
    }

    @Test
    fun `Error state holds message`() {
        val state = SandboxState.Error("Setup failed")
        assertEquals("Setup failed", state.message)
    }

    @Test
    fun `when expression handles all states`() {
        val states = listOf<SandboxState>(
            SandboxState.NotInstalled,
            SandboxState.Downloading(0.3f),
            SandboxState.Extracting,
            SandboxState.Installing("test"),
            SandboxState.Ready,
            SandboxState.Error("err"),
        )

        val results = states.map { state ->
            when (state) {
                is SandboxState.NotInstalled -> "not_installed"
                is SandboxState.Downloading -> "downloading_${state.progress}"
                is SandboxState.Extracting -> "extracting"
                is SandboxState.Installing -> "installing_${state.detail}"
                is SandboxState.Ready -> "ready"
                is SandboxState.Error -> "error_${state.message}"
            }
        }

        assertEquals(
            listOf(
                "not_installed",
                "downloading_0.3",
                "extracting",
                "installing_test",
                "ready",
                "error_err",
            ),
            results,
        )
    }
}
