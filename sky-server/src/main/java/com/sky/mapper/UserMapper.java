package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}
