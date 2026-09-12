package com.sweetmemory.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** JSON 与日期解析小工具 */
final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static List<Map<String, Object>> parseMapList(String json) throws Exception {
        return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private JsonUtil() {
    }
}
