package sodacooky.txbotj.core;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 插件接口，所有插件实现该接口
 */
public interface IPlugin {

    /**
     * 获得优先级，数值越大的越早被分发到消息，从而能打断消息继续向下一个插件传递
     *
     * @return 优先级，-INT_MAX为最低优先级，+INT_MAX为最高优先级
     */
    int getPriority();

    /**
     * 获得插件名
     *
     * @return 插件名称
     */
    String getName();

    /**
     * 响应私聊消息
     *
     * @param message 消息json
     * @return 返回true时继续传递消息给下一个插件，否则当前插件为最后一个处理者
     */
    boolean onPrivateMessage(JsonNode message);

    /**
     * 响应群消息
     *
     * @param message 消息json
     * @return 返回true时继续传递消息给下一个插件，否则当前插件为最后一个处理者
     */
    public boolean onGroupMessage(JsonNode message);

}
