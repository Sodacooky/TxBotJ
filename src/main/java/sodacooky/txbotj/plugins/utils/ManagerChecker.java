package sodacooky.txbotj.plugins.utils;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

/**
 * 管理者身份检查工具
 */
@Component
@Mapper
public interface ManagerChecker {

    /**
     * 判断其是否是机器人管理者
     *
     * @param user_id qq号
     * @return 命中数量，即1为管理员，0为非管理员
     */
    @Select("select count(*) from manager where user_id=#{uid}")
    int isManager(@Param("uid") long user_id);
}
