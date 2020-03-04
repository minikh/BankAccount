package test.bank.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider

fun <T> getResponseAs(mapper: ObjectMapper, content: String?, field: String, `class`: Class<T>): T =
    JsonPath
        .using(Configuration.builder().mappingProvider(JacksonMappingProvider(mapper)).build())
        .parse(content)
        .read(field, `class`)
