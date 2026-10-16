package com.cihatakyol.case_study.data.remote.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull

/**
 * Wire shape of a content item. The `type` field identifies the concrete variant.
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
sealed interface ContentItemDto {
    val id: Int
}

@Serializable
@SerialName("section")
data class SectionDto(
    override val id: Int,
    val title: String,
    @Serializable(with = ContentItemListSerializer::class)
    val items: List<ContentItemDto> = emptyList(),
) : ContentItemDto

@Serializable
@SerialName("text")
data class TextDto(override val id: Int, val content: String) : ContentItemDto

@Serializable
@SerialName("image")
data class ImageDto(override val id: Int, val src: String, val title: String) : ContentItemDto

@Serializable
@SerialName("choice")
data class ChoiceDto(
    override val id: Int,
    val content: String,
    @SerialName("response_set") val responseSet: ResponseSetDto,
) : ContentItemDto

private val KNOWN_ITEM_TYPES = setOf("section", "text", "image", "choice")

/**
 * Deserializes a list of items, dropping any entry whose `type` is not recognized so a single
 * unknown item does not fail the whole document.
 */
internal object ContentItemListSerializer :
    JsonTransformingSerializer<List<ContentItemDto>>(ListSerializer(ContentItemDto.serializer())) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val array = element as? JsonArray ?: return element
        return JsonArray(
            array.filter { item ->
                val type = ((item as? JsonObject)?.get("type") as? JsonPrimitive)?.contentOrNull
                type in KNOWN_ITEM_TYPES
            },
        )
    }
}
