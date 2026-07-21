package io.appdex.core.io

class InMemorySeekableChannelTest : SeekableChannelContractTest() {
    override fun channelWith(initialBytes: ByteArray): SeekableChannel =
        InMemorySeekableChannel(initialBytes)
}
