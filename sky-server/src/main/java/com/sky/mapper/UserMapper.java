package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);


    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    //根据主键id获取用户user对象
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 根据动态条件统计数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
