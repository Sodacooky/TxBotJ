package sodacooky.txbotj.plugins.groupwarmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import sodacooky.txbotj.api.MessageApi;
import sodacooky.txbotj.core.IPlugin;
import sodacooky.txbotj.utils.cmdparser.CommandParser;
import sodacooky.txbotj.utils.global.GlobalValue;
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
    @Resource
    private GlobalValue globalValue;
    @Resource
    private ObjectMapper objectMapper;

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
     * @param cqMessageBody 消息json
     * @return 如果是指令会被处理，返回false；否则返回true
     */
    @Override
    public boolean onPrivateMessage(JsonNode cqMessageBody) throws Exception {
        //提取指令参数
        List<String> blocks = commandParser.parse(cqMessageBody.get("message").asText());
        //判断是否为指令
        if (null == blocks) return true;
        //判断是否为本插件控制指令
        if (!blocks.get(0).endsWith("GroupWarmer")) return true;
        //判断参数数量
        if (blocks.size() != 3) {
            throw new Exception("参数个数不正确");
        }
        //权限判断
        if (!managerChecker.isManager(cqMessageBody.get("user_id").asLong())) {
            throw new Exception("你不是机器人管理员");
        }

        //提取数据
        String expectedOperation = blocks.get(1);
        long operatingGroupID = Long.parseLong(blocks.get(2));
        //处理
        //判断操作是否存在
        String[] availableOperation = new String[]{"start", "end"};
        if (Arrays.stream(availableOperation).noneMatch(s -> s.equalsIgnoreCase(expectedOperation))) {
            throw new Exception("不存在的操作 " + expectedOperation);
        }
        //处理不同操作
        switch (expectedOperation.toLowerCase()) {
            case "start":
                //检查是否已开启（存在）
                if (groupWarmerMapper.isExist(operatingGroupID) != 0) {
                    messageApi.sendBackMessage(cqMessageBody, "群" + operatingGroupID + "已经开启", 1);
                } else {
                    //添加记录以开启
                    groupWarmerMapper.createRecord(operatingGroupID, System.currentTimeMillis());
                    messageApi.sendBackMessage(cqMessageBody, "操作完成", 1);
                }
                break;
            case "stop":
                //检查是否未开启（不存在）
                if (groupWarmerMapper.isExist(operatingGroupID) != 1) {
                    messageApi.sendBackMessage(cqMessageBody, "群" + operatingGroupID + "本没有开启", 1);
                } else {
                    //存在意味着开启了，删除记录
                    groupWarmerMapper.removeRecord(operatingGroupID);
                    messageApi.sendBackMessage(cqMessageBody, "操作完成", 1);
                }
                break;
        }
        return false;
    }


    /**
     * 收到群消息时，更新数据库中（如果有）该群的上一次消息时间
     *
     * @param cqMessageBody 消息json
     * @return 永远返回true
     */
    @Override
    public boolean onGroupMessage(JsonNode cqMessageBody) {
        //获取消息信息
        long groupID = cqMessageBody.get("group_id").asLong();
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

        //读取所有暖群句子
        String sentencesJson = globalValue.readValue("warmer_sentences");
        //转换为Json数组
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(sentencesJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Assert.isTrue(jsonNode.isArray(), "global.warmer_sentences应为Json数组！");
        //转换为List
        List<String> sentences = new ArrayList<>();
        for (JsonNode sentence : jsonNode) {
            sentences.add(sentence.asText());
        }

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
                //抽取发言内容
                String warmContent = sentences.get(new Random().nextInt(sentences.size()));
                //上次发言时间距离现在已经4小时，需要暖
                messageApi.sendGroupMessage(pair.getKey(), warmContent, 0);
                //自己暖的也算发言时间
                groupWarmerMapper.updateLastMessageTimestamp(pair.getKey(), nowTimestamp);
                //日志
                logger.info("暖了群 {}", pair.getKey());
            }
        }

    }


}
