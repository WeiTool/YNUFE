package com.ynufe.utils.wlan

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TEA @Inject constructor() {
    fun encode(str: String, key: String): ByteArray {
        if (str.isEmpty()) return byteArrayOf()

        // 数据预处理
        val strUint32: MutableList<Long> = toUint32Words(str, true).toMutableList()
        val keyUint32: MutableList<Long> = toUint32Words(key, false).toMutableList()

        // key 补全长度到 4
        while (keyUint32.size < 4) {
            keyUint32.add(0L)
        }

        // 定义 n (最后一个元素的索引)
        val n: Int = strUint32.size - 1
        if (str.isEmpty()) return byteArrayOf()

        // 初始化加密参数
        var strUint32Last: Long = strUint32[n]
        var y: Long

        // 定义黄金分割比
        val goldenRatio = 0x9E3779B9L
        // 定义取值范围
        val range: Int = 6 + 52 / (n + 1)
        // 定义累加器
        var accumulate = 0L

        // 主加密循环
        repeat (range) {
            // 累加黄金分割比并保持 32 位范围
            accumulate = (accumulate + goldenRatio) and 0xFFFFFFFFL

            // 计算密钥索引偏移量 (e)
            val keyIndexOffset: Int = ((accumulate ushr 2) and 3).toInt()

            // 内部循环：处理从 0 到 n-1 的元素
            for (p in 0..<n) {
                y = strUint32[p + 1]

                // 第一部分：基于位移的混合
                var mixValue: Long = (strUint32Last ushr 5) xor (y shl 2)

                // 第二部分：加入 y 的位移和 accumulate 的异或
                mixValue = (mixValue + ((y ushr 3) xor (strUint32Last shl 4) xor (accumulate xor y))) and 0xFFFFFFFFL

                // 第三部分：加入 Key 和 z 的异或 (对应 Java: m += k.get((p & 3) ^ e) ^ z)
                mixValue = (mixValue + (keyUint32[(p and 3) xor keyIndexOffset] xor strUint32Last)) and 0xFFFFFFFFL

                val newVal : Long =  (strUint32[p] + mixValue) and 0xFFFFFFFFL
                strUint32[p] = newVal
                strUint32Last = newVal
            }

            // 处理最后一个元素 n
            y = strUint32[0]
            // 第一步：初始值
            var mixValue: Long = (strUint32Last ushr 5) xor (y shl 2)

            // 第二步：中间部分的混合
            mixValue = (mixValue + ((y ushr 3) xor (strUint32Last shl 4) xor (accumulate xor y))) and 0xFFFFFFFFL

            // 第三步：密钥部分的混合
            // 先计算正确的 key 索引 (Int 类型)
            val keyIndex = (n and 3) xor keyIndexOffset
            // 取出 key 之后先和 strUint32Last 异或，再加到 mixValue 上
            mixValue = (mixValue + (keyUint32[keyIndex] xor strUint32Last)) and 0xFFFFFFFFL

            val newVal = (strUint32[n] + mixValue) and 0xFFFFFFFFL
            strUint32[n] = newVal
            strUint32Last = newVal

        }

        // 转换为二进制数据
        return toBytes(strUint32, false)
    }

    fun toUint32Words(input: String, addLength: Boolean): List<Long> {
        // 获取字符串的长度
        val inputLength = input.length
        // 定义一个Long数组存储words
        val words = arrayListOf<Long>()

        // 4个为一组将字符串转换成对应的ASCII码表
        for (i in 0..<inputLength step 4) {
            // 定义一个value值
            var value = 0L
            // 定义4个字节根据输入字符串的索引分别放入对应ASCII码表
            for (j in 0..3) {
                // 定义字节索引
                val charIndex = i + j
                // 把对应ASCII码表的数字放入value当中
                val charCode = input.getOrNull(charIndex)?.code ?: 0
                // 确保每一个字符串从低位数占满8个
                value = value or ((charCode.toLong() and 0xFF) shl (8 * j))
            }
            // 截断，确保只获取32位
            words.add(value and 0xFFFFFFFFL)
        }

        // 添加字符串长度
        if (addLength) {
            words.add(inputLength.toLong())
        }

        return words
    }

    fun toBytes(words: List<Long>, truncate: Boolean): ByteArray {
        // 处理长度标记和数据列表
        var dataList = words
        var originalLength = 0

        if (truncate && dataList.isNotEmpty()) {
            // 获取最后一个元素作为长度
            originalLength = dataList.last().toInt()
            // 从数组当中去除这个长度，但是数组长度不变
            dataList = dataList.dropLast(1)
        }

        // 分配 ByteBuffer 并设置小端序
        // 每个 Long 对应 4 字节的 Int
        val buffer = ByteBuffer.allocate(dataList.size * 4).apply {
            order(ByteOrder.LITTLE_ENDIAN)
        }

        // 将每个 Long 转换为 32位 Int 并放入 buffer
        for (num in dataList) {
            buffer.putInt((num and 0xFFFFFFFFL).toInt())
        }

        // 获取结果并进行可能的截断
        val result = buffer.array()
        return if (truncate) {
            // 确保不会因为 originalLength 错误而导致数组越界
            val end = originalLength.coerceAtMost(result.size)
            result.copyOfRange(0, end)
        } else {
            // 返回字符串但是后面会多空字符
            result
        }
    }
}