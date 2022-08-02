package sodacooky.txbotj.plugins.groupwarmer;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.api.MessageApi;
import sodacooky.txbotj.core.IPlugin;
import sodacooky.txbotj.plugins.utils.ManagerChecker;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 暖群工具
 * 在开启的群内，每4小时检测一次群消息时间，
 * 如果没有人发新的消息则发送一条暖群消息（目前固定
 */
@Component
public class GroupWarmer implements IPlugin {

    @Autowired
    private MessageApi messageApi;
    @Autowired
    private GroupWarmerMapper groupWarmerMapper;
    @Autowired
    private ManagerChecker managerChecker;

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE; //优先级高，要记录监听每一条消息
    }

    @Override
    public String getName() {
        return "GroupWarmer";
    }

    /**
     * 私聊处理机器人管理员的指令
     *
     * @param message 消息json
     * @return 如果是指令会被处理，返回false；否则返回true
     */
    @Override
    public boolean onPrivateMessage(JsonNode message) {
        //获取消息信息
        String content = message.get("message").asText();
        long userID = message.get("user_id").asLong();
        //指令判断
        //判断是否为控制指令
        if (!content.startsWith(">>")) return true;
        //判断是否为本插件控制指令
        if (!content.startsWith(">>GroupWarmer")) return true;
        //权限判断
        if (managerChecker.isManager(userID) != 1) {
            messageApi.sendPrivateMessage(userID, "你不是机器人管理员！");
            return false;
        }
        //处理
        processCommand(content, userID);
        return false;
    }

    /**
     * 实际处理指令
     *
     * @param ctn 内容
     * @param uid qq
     */
    private void processCommand(String ctn, long uid) {
        //提取参数
        String[] blocks = ctn.split(" ");
        //判断参数数量
        if (blocks.length != 3) {
            //参数不正确
            sendErrorMessage("参数个数不正确", uid);
            return;
        }
        //获取操作动作和操作群号
        String expectedOperation = blocks[1];
        long operatingGroupID = Long.parseLong(blocks[2]);
        //判断动作
        if (expectedOperation.equalsIgnoreCase("start")) {              //启用
            //检查是否存在
            if (groupWarmerMapper.isExist(operatingGroupID) != 0) {
                sendErrorMessage("群" + operatingGroupID + "已经开启", uid);
            } else {
                //添加记录以开启
                groupWarmerMapper.createRecord(operatingGroupID, Calendar.getInstance().getTimeInMillis());
                messageApi.sendPrivateMessage(uid, "操作完成");
            }
        } else if (expectedOperation.equalsIgnoreCase("stop")) {        //停用
            //检查是否存在
            if (groupWarmerMapper.isExist(operatingGroupID) != 1) {
                sendErrorMessage("群" + operatingGroupID + "本没有开启", uid);
            } else {
                //存在意味着开启了，删除记录
                groupWarmerMapper.removeRecord(operatingGroupID);
                messageApi.sendPrivateMessage(uid, "操作完成");
            }
        } else {                                                                    //意外
            //操作词错误
            sendErrorMessage("不存在的操作 " + expectedOperation, uid);
        }

    }

    /**
     * 发送代用用法的错误消息
     *
     * @param reason 错误原因提示
     * @param uid    qq
     */
    private void sendErrorMessage(String reason, long uid) {
        messageApi.sendPrivateMessage(uid, reason + "\n用法: >>GroupWarmer start/stop [group_id]");
    }


    /**
     * 收到群消息时，更新数据库中（如果有）该群的上一次消息时间
     *
     * @param message 消息json
     * @return 永远返回true
     */
    @Override
    public boolean onGroupMessage(JsonNode message) {
        //获取消息信息
        long groupID = message.get("group_id").asLong();
        //判断是否有记录，不存在记录的忽略
        if (groupWarmerMapper.isExist(groupID) == 1) {
            //存在记录，更新时间
            groupWarmerMapper.updateLastMessageTimestamp(groupID, Calendar.getInstance().getTimeInMillis());
        }
        return true;
    }


    /**
     * 每四小时，从数据库开启该功能的群内，检查上次消息时间，
     * 如果超过四小时，则发送消息
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void doWarm() {
        //用于存放启用的群及其上次消息时间
        Map<Long, Long> task = new HashMap<>();
        //读取启用的群
        List<Long> enabledGroupID = groupWarmerMapper.getEnabledGroupID();
        //读取其上次消息时间
        enabledGroupID.forEach(v -> task.put(v, groupWarmerMapper.getLastMessageTimestamp(v)));
        //遍历判断是否需要暖
        long nowTimestamp = Calendar.getInstance().getTimeInMillis();
        Logger logger = LoggerFactory.getLogger(GroupWarmer.class);
        for (Map.Entry<Long, Long> pair : task.entrySet()) {
            long delta = nowTimestamp - pair.getValue();
            if (delta > 1000 * 60 * 60 * 4) {
                //上次发言时间距离现在已经超过4小时，需要暖
                messageApi.sendGroupMessage(pair.getKey(), "别让群友寂寞太久！");
                //更新消息时间
                groupWarmerMapper.updateLastMessageTimestamp(pair.getKey(), nowTimestamp);
                //日志
                logger.info("暖了群 {}", pair.getKey());
            }
        }

    }


}
