package sodacooky.txbotj.plugins.groupwarmer;

import org.apache.ibatis.annotations.*;

import java.util.List;


/**
 * -- auto-generated definition
 * create table group_warmer
 * (
 * group_id               INTEGER not null
 * constraint group_warmer_pk
 * primary key,
 * last_message_timestamp INTEGER default 0 not null
 * );
 */
@Mapper
public interface GroupWarmerMapper {

    /**
     * 查询指定群是否有记录，即是否存在
     *
     * @param group_id 群号
     * @return 1如果有记录，0不存在
     */
    @Select("select count(*) from group_warmer where group_id=#{gid}")
    int isExist(@Param("gid") long group_id);

    /**
     * 添加新纪录，注意，添加新纪录意味着对该群启用该功能
     *
     * @param group_id      群号
     * @param now_timestamp 当前的时间，防止刚刚创建3小时内无人发言而触发
     */
    @Insert("insert into group_warmer values (#{gid},#{ts})")
    void createRecord(@Param("gid") long group_id, @Param("ts") long now_timestamp);


    /**
     * 移除记录，注意，移除记录意味着对该群关闭功能
     *
     * @param group_id 群号
     */
    @Delete("delete from group_warmer where group_id=#{gid}")
    void removeRecord(@Param("gid") long group_id);

    /**
     * 获取上次该群发言时间
     *
     * @param group_id 群号
     * @return 时间戳，毫秒
     */
    @Select("select last_message_timestamp from group_warmer where group_id=#{gid}")
    long getLastMessageTimestamp(@Param("gid") long group_id);

    /**
     * 更新上次发言时间
     *
     * @param group_id  群号
     * @param timestamp 新的时间戳，毫秒
     */
    @Update("update group_warmer set last_message_timestamp=#{ts} where group_id=#{gid}")
    void updateLastMessageTimestamp(@Param("gid") long group_id, @Param("ts") long timestamp);

    /**
     * 获取数据库中记录的（也就是开启了的）群号
     *
     * @return 启用该功能的群号列表
     */
    @Select("select group_id from group_warmer")
    List<Long> getEnabledGroupID();
}
