package com.sweetmemory.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.sweetmemory.entity.Photo;

public interface PhotoMapper {

    List<Photo> listByDeleted(@Param("deleted") boolean deleted);

    Photo findById(@Param("id") long id);

    int insert(Photo photo);

    /** 动态更新：仅更新非 null 字段 */
    int updateSelective(Photo photo);

    int updateDeleted(@Param("id") long id, @Param("deleted") boolean deleted);

    int deleteById(@Param("id") long id);
}
