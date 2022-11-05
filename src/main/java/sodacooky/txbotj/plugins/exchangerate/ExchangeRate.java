package sodacooky.txbotj.plugins.exchangerate;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.api.MessageApi;
import sodacooky.txbotj.core.HttpSender;
import sodacooky.txbotj.core.IPlugin;
import sodacooky.txbotj.utils.cmdparser.CommandParser;
import sodacooky.txbotj.utils.global.GlobalValue;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * 汇率查询工具
 */
@Component
public class ExchangeRate implements IPlugin {

    //就不引入什么OKHttp了，直接用自己封装的手搓吧
    @Resource
    private HttpSender httpSender;

    //需要记录API访问次数以限制，以及获取apikey
    @Resource
    private GlobalValue globalValue;

    //有点画蛇添足？
    @Resource
    private CommandParser commandParser;

    @Resource
    private MessageApi messageApi;

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String getName() {
        return "ExchangeRate";
    }

    @Override
    public boolean onPrivateMessage(JsonNode cqMessageBody) {
        return perform(cqMessageBody, false);
    }

    @Override
    public boolean onGroupMessage(JsonNode cqMessageBody) {
        return perform(cqMessageBody, true);
    }


    /**
     * 执行Api查询操作并发送结果消息字符串
     */
    private boolean perform(JsonNode message, boolean isGroup) {
        //获取消息信息
        String content = message.get("message").asText();
        Long goalId = null;
        if (isGroup) {
            goalId = message.get("group_id").asLong();
        } else {
            goalId = message.get("user_id").asLong();
        }

        //从数据库读取今日已使用次数
        if (Integer.parseInt(globalValue.readValue("tianapi_count")) >= 100) {
            messageApi.sendErrorMessage(message, "今日API已达到限额");
            return false;
        }

        //尝试将指令拆分为块
        List<String> blocks = commandParser.parse(content);
        //是否为指令
        if (null == blocks) return true;
        //是否当前插件
        if (!blocks.get(0).endsWith("ExchangeRate")) return true;
        //参数数量是否正确
        if (4 != blocks.size()) {
            messageApi.sendErrorMessage(message, "参数数量错误");
            return false;
        }

        //提取参数
        String fromCode = blocks.get(1);
        String toCode = blocks.get(2);
        Integer fromAmount = Integer.parseInt(blocks.get(3));

        //执行
        Double toAmount = performApiQuery(fromCode, toCode, fromAmount);

        //更新使用次数
        globalValue.setValue("tianapi_count",
                Integer.toString(
                        Integer.parseInt(
                                globalValue.readValue("tianapi_count")
                        ) + 1
                )
        );

        //构建消息
        String msg = buildResultString(fromCode, toCode, fromAmount, toAmount);

        //发送消息
        messageApi.sendBackMessage(message, msg, 1);
        return false;
    }

    /**
     * 根据结果的Double对象生成结果消息字符串
     *
     * @param fromCode   来源货币编号
     * @param toCode     目标货币编号
     * @param fromAmount 来源货币数量
     * @param toAmount   目标货币数量
     * @return 结果消息字符串，如果result为null那么会是错误消息
     */
    private String buildResultString(String fromCode, String toCode, Integer fromAmount, Double toAmount) {
        //判断结果
        StringBuilder stringBuilder = new StringBuilder();
        if (null == toAmount) {
            //构建出错提示
            stringBuilder.append("查询API错误，请检查货币代号等参数。");
        } else {
            //构建结果
            stringBuilder.append(fromAmount)
                    .append(" ")
                    .append(fromCode)
                    .append(" => ")
                    .append(toAmount)
                    .append(" ")
                    .append(toCode);
        }
        return stringBuilder.toString();
        //Example: 1 CNY => 7.23456 USD
    }

    /**
     * 调用API查询
     *
     * @param fromCode   来源货币编号
     * @param toCode     目标货币编号
     * @param fromAmount 来源货币数量
     * @return 结果，如果出错返回null
     */
    private Double performApiQuery(String fromCode, String toCode, Integer fromAmount) {
        //从数据库读取apikey
        String apiKey = globalValue.readValue("tianapi_key");
        //构建参数
        Map<String, String> params = new HashMap<>();
        params.put("key", apiKey);
        params.put("fromcoin", fromCode);
        params.put("tocoin", toCode);
        params.put("money", Integer.toString(fromAmount));
        //构建url
        String url = httpSender.buildCustomUrl("http://api.tianapi.com/fxrate", "index", params);
        //调用
        Future<JsonNode> future = httpSender.queueRequest(url, 0);
        try {
            //等待
            JsonNode jsonNode = future.get();
            //结果
            if (200 != jsonNode.get("code").asInt()) return null;
            //结构符合预期
            if (!jsonNode.get("newslist").isArray()) return null;
            //提取
            return jsonNode.get("newslist").get(0).get("money").asDouble();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 每天0点更新Api已使用次数
     */
    @Scheduled(cron = "* * 0 * * *")
    private void refreshApiCount() {
        globalValue.setValue("tianapi_count", "0");
    }


}
