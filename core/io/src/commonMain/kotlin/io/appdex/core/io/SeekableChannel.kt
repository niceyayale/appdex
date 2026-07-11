package io.appdex.core.io

/**
 * 可随机寻址的字节通道。读 / 写共享一个游标。
 *
 * 所有方法非线程安全,调用方自行同步。
 *
 * 注:计划原写为 `interface SeekableChannel : Closeable`,但 Kotlin 2.0.21 的
 * commonMain stdlib 尚未提供 `kotlin.Closeable`(该接口自 Kotlin 2.1.0 才进入
 * common stdlib)。故直接在接口内声明 `fun close()`,行为等价,不引入额外文件。
 */
interface SeekableChannel {

    /** 内容总字节数。 */
    val size: Long

    /**
     * 将游标移到 [newPos]。负数或超出 [size] 抛 [IllegalArgumentException]。
     * 写入后调用方应自行决定是否重新读 size。
     */
    fun position(newPos: Long)

    /**
     * 从当前游标读取最多 [len] 字节到 [buf]。
     * @return 实际读取的字节数;到达末尾返回 0(EOF)。
     */
    fun read(buf: ByteArray, len: Int): Int

    /**
     * 从 [buf] 的 [off] 偏移写入 [len] 字节,游标后移。
     * 写入位置超过当前 size 时,文件按需扩展。
     */
    fun write(buf: ByteArray, off: Int, len: Int)

    /** 关闭通道,释放底层资源。关闭后再调用任意方法行为未定义。 */
    fun close()
}
