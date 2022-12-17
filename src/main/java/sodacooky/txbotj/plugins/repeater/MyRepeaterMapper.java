package sodacooky.txbotj.plugins.repeater;

import org.apache.ibatis.annotations.*;

/**
 * -- auto-generated definition
 * create table repeater
 * (
 * group_id              INTEGER not null
 * constraint pri_key
 * primary key,
 * last_repeat_timestamp INTEGER,
 * last_message_content  TEXT
 * );
 */
@Mapper
public interface MyRepeaterMapper {

    /**
     * 看是否有该群的记录，应该获取前先判断一次
     *
     * @param group_id 群号
     * @return 返回大于0（数量）则为存在，为0则不存在
     */
    @Select("select count(*) from repeater where group_id = #{group_id}")
    int isExist(@Param("group_id") long group_id);

    /**
     * 添加新纪录
     *
     * @param group_id 群号
     */
    @Insert("insert into repeater (group_id) values(#{group_id})")
    void createNew(@Param("group_id") long group_id);

    /**
     * 获取上次复读时间戳
     *
     * @param group_id 群号
     * @return 时间戳，毫秒
     */
    @Select("select last_repeat_timestamp from repeater where group_id = #{group_id}")
    long getLastRepeatTimestamp(@Param("group_id") long group_id);

    /**
     * 获取该群上次消息的内容
     *
     * @param group_id 群号
     * @return 消息内容，可能被设置为[Don't Repeat]，表示不希望有被复读的可能
     */
    @Select("select last_message_content from repeater where group_id = #{group_id}")
    String getLastMessageContent(@Param("group_id") long group_id);

    /**
     * 更新上次复读时间戳
     *
     * @param group_id  群号
     * @param timestamp 新的时间戳
     */
    @Update("update repeater set last_repeat_timestamp = #{ts} where group_id = #{group_id}")
    void updateLastRepeatTimestamp(@Param("group_id") long group_id, @Param("ts") long timestamp);

    /**
     * 更新该群的上一条小希
     *
     * @param group_id 群号
     * @param content  新的内容
     */
    @Update("update repeater set last_message_content = #{ctn} where group_id = #{gid}")
    void updateLastMessageContent(@Param("gid") long group_id, @Param("ctn") String content);

}
