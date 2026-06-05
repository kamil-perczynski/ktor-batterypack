package io.github.ktor_batterypack.core.problemdetail

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProblemDetail(
    @param:JsonProperty("type")
    val type: String = "about:blank",

    @param:JsonProperty("title")
    val title: String,

    @param:JsonProperty("status")
    val status: Int,

    @param:JsonProperty("detail")
    val detail: String? = null,

    @param:JsonProperty("instance")
    val instance: String? = null,

    @param:JsonProperty("extension_data")
    @get:JsonAnyGetter
    val extensionData: Map<String, Any>? = null
)
