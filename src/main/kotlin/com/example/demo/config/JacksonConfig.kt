package com.example.demo.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.google.cloud.Timestamp
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException

@JsonComponent
class TimestampSerializer : JsonSerializer<Timestamp>() {
    @Throws(IOException::class)
    override fun serialize(value: Timestamp, gen: JsonGenerator, serializers: SerializerProvider) {
        // Serialize com.google.cloud.Timestamp as Long (epoch milliseconds)
        gen.writeNumber(value.toDate().time)
    }
}
