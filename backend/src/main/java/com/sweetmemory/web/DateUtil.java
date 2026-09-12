package com.sweetmemory.web;

import java.time.LocalDate;

/** 日期解析小工具：支持 "yyyy-MM-dd" 与 ISO 带时间格式 */
final class DateUtil {

    static LocalDate parseDate(Object o) {
        if (o == null) return LocalDate.now();
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) return LocalDate.now();
        if (s.length() >= 10) s = s.substring(0, 10); // "2024-05-01T..." → "2024-05-01"
        return LocalDate.parse(s);
    }

    private DateUtil() {
    }
}
