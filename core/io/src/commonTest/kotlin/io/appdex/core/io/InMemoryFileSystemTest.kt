package io.appdex.core.io

class InMemoryFileSystemTest : FileSystemContractTest() {
    override fun fs(): FileSystem = InMemoryFileSystem()
}
