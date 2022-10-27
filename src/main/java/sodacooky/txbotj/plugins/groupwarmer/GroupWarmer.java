package sodacooky.txbotj.plugins.groupwarmer;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.api.MessageApi;
import sodacooky.txbotj.core.IPlugin;
import sodacooky.txbotj.utils.cmdparser.CommandParser;
import sodacooky.txbotj.utils.managercheck.ManagerChecker;

import javax.annotation.Resource;
import java.util.*;

/**
 * 暖群工具
 * 在开启的群内，每4小时检测一次群消息时间，
 * 如果没有人发新的消息则发送一条暖群消息（目前固定
 */
@Component
public class GroupWarmer implements IPlugin {

    @Resource
    private MessageApi messageApi;
    @Resource
    private GroupWarmerMapper groupWarmerMapper;
    @Resource
    private ManagerChecker managerChecker;
    @Resource
    private CommandParser commandParser;

    private final Logger logger = LoggerFactory.getLogger(GroupWarmer.class);

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
        //提取参数
        List<String> blocks = commandParser.parse(content);
        //指令判断
        if (null == blocks) return true;
        //判断是否为本插件控制指令
        if (!blocks.get(0).endsWith("GroupWarmer")) return true;
        //判断参数数量
        if (blocks.size() != 3) {
            //参数不正确
            sendErrorMessage("参数个数不正确", userID);
            return false;
        }
        //权限判断
        if (!managerChecker.isManager(userID)) {
            messageApi.sendPrivateMessage(userID, "你不是机器人管理员！", 0);
            return false;
        }
        //处理
        commandProcess(blocks.get(1), Long.parseLong(blocks.get(2)), userID);
        return false;
    }

    /**
     * 实际处理命令
     *
     * @param expectedOperation 操作
     * @param operatingGroupID  操作的群
     * @param feedbackUserId    操作的用户
     */
    private void commandProcess(String expectedOperation, long operatingGroupID, long feedbackUserId) {
        String[] availableOperation = new String[]{"start", "end"};
        if (Arrays.stream(availableOperation).noneMatch(s -> s.equalsIgnoreCase(expectedOperation))) {
            //操作词错误
            sendErrorMessage("不存在的操作 " + expectedOperation, feedbackUserId);
        }
        switch (expectedOperation.toLowerCase()) {
            case "start":
                //检查是否已开启（存在）
                if (groupWarmerMapper.isExist(operatingGroupID) != 0) {
                    sendErrorMessage("群" + operatingGroupID + "已经开启", feedbackUserId);
                } else {
                    //添加记录以开启
                    groupWarmerMapper.createRecord(operatingGroupID, Calendar.getInstance().getTimeInMillis());
                    messageApi.sendPrivateMessage(feedbackUserId, "操作完成", 0);
                }
                break;
            case "stop":
                //检查是否存在
                if (groupWarmerMapper.isExist(operatingGroupID) != 1) {
                    sendErrorMessage("群" + operatingGroupID + "本没有开启", feedbackUserId);
                } else {
                    //存在意味着开启了，删除记录
                    groupWarmerMapper.removeRecord(operatingGroupID);
                    messageApi.sendPrivateMessage(feedbackUserId, "操作完成", 0);
                }
                break;
        }
    }

    /**
     * 发送代用用法的错误消息
     *
     * @param reason 错误原因提示
     * @param uid    qq
     */
    private void sendErrorMessage(String reason, long uid) {
        messageApi.sendPrivateMessage(uid, reason + "\n用法: >>GroupWarmer start/stop [group_id]", 0);
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
     * 每两小时，从数据库开启该功能的群内，检查上次消息时间，
     * 如果距离上次有人发言超过四小时，则发送消息
     */
    @Scheduled(cron = "0 0 */2 * * *")
    public void doWarm() {
        //如果当前是深夜，凌晨2点到早上7点之间，略过
        Calendar calendar = Calendar.getInstance();
        int nowHour = calendar.get(Calendar.HOUR);
        if (nowHour >= 2 && nowHour <= 7) return;
        //用于存放启用的群及其上次消息时间
        Map<Long, Long> groupLastRepeatTime = new HashMap<>();
        //读取启用的群
        List<Long> enabledGroupID = groupWarmerMapper.getEnabledGroupID();
        //读取其上次消息时间
        enabledGroupID.forEach(v -> groupLastRepeatTime.put(v, groupWarmerMapper.getLastMessageTimestamp(v)));
        //遍历判断是否需要暖
        long nowTimestamp = calendar.getTimeInMillis();
        for (Map.Entry<Long, Long> pair : groupLastRepeatTime.entrySet()) {
            long delta = nowTimestamp - pair.getValue();
            if (delta >= (1000 * 3600 * 4) - 10000) { //是否超过大概4小时没人说话？
                //上次发言时间距离现在已经4小时，需要暖
                messageApi.sendGroupMessage(pair.getKey(), "别让群友寂寞太久！", 0);
                //自己暖的也算发言时间
                groupWarmerMapper.updateLastMessageTimestamp(pair.getKey(), nowTimestamp);
                //日志
                logger.info("暖了群 {}", pair.getKey());
            }
        }

    }


}
