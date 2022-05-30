package txbotj.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import txbotj.dispatch.MessageDispatcher;

/**
 * 继承这个类，类的默认构造函数将会把自身添加到消息分发器
 */
public abstract class Plugin {

    //将自身保存到MessageDispatcher
    protected Plugin(String pluginName) {
        MessageDispatcher.plugins.put(pluginName, this);
    }

    public boolean onGroupMessage(JsonNode message) {
        return true;
    }

    public boolean onPrivateMessage(JsonNode message) {
        return true;
    }

    public boolean onOtherMessage(JsonNode message) {
        return true;
    }

}
