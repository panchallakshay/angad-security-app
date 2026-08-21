package com.spidey.js.angad.util

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object PacketParser {

    data class DnsQuestion(val domain: String, val type: Int)

    fun parseDnsDomain(data: ByteArray, offset: Int, length: Int): DnsQuestion? {
        if (length < 12) return null
        
        val buffer = ByteBuffer.wrap(data, offset, length)
        val qdCount = buffer.getShort(4).toInt()
        if (qdCount <= 0) return null

        val domain = StringBuilder()
        var pos = 12
        
        try {
            while (pos < length) {
                val labelLen = data[offset + pos].toInt() and 0xFF
                if (labelLen == 0) {
                    pos++
                    break
                }
                if (domain.isNotEmpty()) domain.append(".")
                if (pos + 1 + labelLen > length) return null
                domain.append(String(data, offset + pos + 1, labelLen, StandardCharsets.US_ASCII))
                pos += 1 + labelLen
            }
            if (pos + 2 > length) return null
            val type = ((data[offset + pos].toInt() and 0xFF) shl 8) or (data[offset + pos + 1].toInt() and 0xFF)
            return DnsQuestion(domain.toString(), type)
        } catch (e: Exception) {
            return null
        }
    }

    fun parseSni(data: ByteArray, offset: Int, length: Int): String? {
        try {
            val buffer = ByteBuffer.wrap(data, offset, length)
            if (buffer.get().toInt() != 22) return null
            buffer.position(buffer.position() + 4)
            if (buffer.get().toInt() != 1) return null
            buffer.position(buffer.position() + 3)
            buffer.position(buffer.position() + 34)
            val sessionIdLen = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + sessionIdLen)
            val cipherSuitesLen = buffer.getShort().toInt() and 0xFFFF
            buffer.position(buffer.position() + cipherSuitesLen)
            val compressionLen = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + compressionLen)
            
            if (buffer.position() >= length) return null
            val extensionsLen = buffer.getShort().toInt() and 0xFFFF
            val extensionsEnd = buffer.position() + extensionsLen
            
            while (buffer.position() < extensionsEnd && buffer.position() < length - 4) {
                val extType = buffer.getShort().toInt() and 0xFFFF
                val extLen = buffer.getShort().toInt() and 0xFFFF
                if (extType == 0) {
                    buffer.position(buffer.position() + 2)
                    if (buffer.get().toInt() == 0) {
                        val nameLen = buffer.getShort().toInt() and 0xFFFF
                        val nameBytes = ByteArray(nameLen)
                        buffer.get(nameBytes)
                        return String(nameBytes, StandardCharsets.US_ASCII)
                    }
                } else {
                    buffer.position(buffer.position() + extLen)
                }
            }
        } catch (e: Exception) {}
        return null
    }

    fun createNxDomainResponse(queryData: ByteArray, offset: Int, length: Int): ByteArray {
        val response = queryData.copyOfRange(offset, offset + length)
        if (response.size < 12) return response
        response[2] = 0x81.toByte() // QR=1, RD=1
        response[3] = 0x83.toByte() // RA=1, RCODE=3 (NXDOMAIN)
        response[6] = 0x00.toByte() // Answer count = 0
        response[7] = 0x00.toByte()
        response[8] = 0x00.toByte() // Authority count = 0
        response[9] = 0x00.toByte()
        response[10] = 0x00.toByte() // Additional count = 0
        response[11] = 0x00.toByte()
        return response
    }

    fun createSinkholeResponse(queryData: ByteArray, offset: Int, length: Int): ByteArray {
        val base = queryData.copyOfRange(offset, offset + length)
        if (base.size < 12) return base
        val response = ByteArray(base.size + 16)
        System.arraycopy(base, 0, response, 0, base.size)
        response[2] = 0x81.toByte()
        response[3] = 0x80.toByte() // NOERROR
        response[6] = 0x00.toByte()
        response[7] = 0x01.toByte() // 1 Answer (0.0.0.0)
        
        var idx = base.size
        response[idx++] = 0xC0.toByte()
        response[idx++] = 0x0C.toByte() // Name Pointer to 0x0C
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x01.toByte() // TYPE A
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x01.toByte() // CLASS IN
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x3C.toByte() // TTL 60s
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x04.toByte() // RDLENGTH 4
        response[idx++] = 0x00.toByte() // 0.0.0.0 (Sinkhole)
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte()
        return response
    }
}
