package br.univesp.pi.expedientes.core.network.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

object BigDecimalAsStringSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalAsString", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) = encoder.encodeString(value.toPlainString())

    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: LocalDate,
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

typealias Decimal =
    @Serializable(with = BigDecimalAsStringSerializer::class)
    BigDecimal

typealias Timestamp =
    @Serializable(with = InstantSerializer::class)
    Instant

typealias Day =
    @Serializable(with = LocalDateSerializer::class)
    LocalDate
