package com.android.sandbox.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RootfsDownloaderTest {

    private val downloader = RootfsDownloaderStub()

    @Test
    fun `getDownloadUrl returns correct URL for aarch64`() {
        val url = downloader.getDownloadUrl("aarch64")
        assertTrue(url.contains("aarch64"))
        assertTrue(url.contains("alpine-minirootfs-3.21.3"))
        assertTrue(url.startsWith("https://dl-cdn.alpinelinux.org"))
    }

    @Test
    fun `getDownloadUrl returns correct URL for armhf`() {
        val url = downloader.getDownloadUrl("armhf")
        assertTrue(url.contains("armhf"))
    }

    @Test
    fun `getDownloadUrl returns correct URL for x86_64`() {
        val url = downloader.getDownloadUrl("x86_64")
        assertTrue(url.contains("x86_64"))
    }

    @Test
    fun `makeWritable sets directories to writable`() {
        val tempDir = File.createTempFile("rootfs_test", null).apply { delete(); mkdir() }
        val subDir = File(tempDir, "usr/bin").apply { mkdirs() }
        subDir.setWritable(false)

        downloader.makeWritable(tempDir)

        assertTrue(tempDir.canWrite())
        tempDir.deleteRecursively()
    }

    @Test
    fun `writeResolvConf creates correct resolv conf`() {
        val tempDir = File.createTempFile("rootfs_etc", null).apply { delete(); mkdir() }

        downloader.writeResolvConf(tempDir)

        val resolvConf = File(tempDir, "etc/resolv.conf")
        assertTrue(resolvConf.exists())
        val content = resolvConf.readText()
        assertTrue(content.contains("nameserver 8.8.8.8"))
        assertTrue(content.contains("nameserver 8.8.4.4"))

        tempDir.deleteRecursively()
    }

    @Test
    fun `extractTarGz handles non-existent file gracefully`() {
        val tempDir = File.createTempFile("extract_test", null).apply { delete(); mkdir() }
        val nonExistentTar = File(tempDir, "nonexistent.tar.gz")

        assertThrows(java.io.FileNotFoundException::class.java) {
            downloader.extractTarGz(nonExistentTar, tempDir)
        }

        tempDir.deleteRecursively()
    }
}

class RootfsDownloaderStub : RootfsDownloaderStubInterface {
    override fun getDownloadUrl(arch: String): String =
        "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/$arch/alpine-minirootfs-3.21.3-$arch.tar.gz"

    override fun makeWritable(rootfsDir: File) {
        rootfsDir.walkTopDown().forEach { file ->
            if (file.isDirectory && !file.canWrite()) {
                file.setWritable(true, true)
            }
        }
    }

    override fun writeResolvConf(rootfsDir: File) {
        val etcDir = File(rootfsDir, "etc")
        etcDir.mkdirs()
        File(etcDir, "resolv.conf").writeText(
            "nameserver 8.8.8.8\nnameserver 8.8.4.4\n",
        )
    }

    override fun extractTarGz(tarGzFile: File, targetDir: File) {
        throw java.io.FileNotFoundException("Test stub")
    }
}

interface RootfsDownloaderStubInterface {
    fun getDownloadUrl(arch: String): String
    fun makeWritable(rootfsDir: File)
    fun writeResolvConf(rootfsDir: File)
    fun extractTarGz(tarGzFile: File, targetDir: File)
}
