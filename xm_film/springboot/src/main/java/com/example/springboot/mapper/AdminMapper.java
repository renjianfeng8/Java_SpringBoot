package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Admin;
import org.apache.ibatis.annotations.Param;

public interface AdminMapper extends BaseMapper<Admin> {

    Admin selectByUsername(String username);

    void updatePassword(Admin admin);

    /** 吊销点：改密时写入最新令牌的 iat，早于它的令牌一律拒绝 */
    int updateRevokedBefore(@Param("id") Integer id, @Param("epochSecond") long epochSecond);

    /** 该账号的吊销点（epoch 秒）；从未吊销时为 null */
    Long selectRevokedBefore(Integer id);

}
