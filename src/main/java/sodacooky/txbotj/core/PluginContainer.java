package sodacooky.txbotj.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自动加载插件包路径下的类，存放到这里
 */
@Component
public class PluginContainer {
    private List<IPlugin> plugins = new ArrayList<>();

    /**
     * 使用applicationContext加载插件Beans，并牌优先级排序
     *
     * @param applicationContext spring application context
     */
    public void loadPlugins(ApplicationContext applicationContext) {
        //通过applicationContext获取插件Beans
        Map<String, IPlugin> beansMap;
        beansMap = applicationContext.getBeansOfType(IPlugin.class);
        //判断有没有插件
        if (beansMap.isEmpty()) {
            LoggerFactory.getLogger(PluginContainer.class).error("没有插件");
            return;
        }
        //将插件存放到内部容器并按优先级排序
        //加入list中
        beansMap.forEach((k, v) -> plugins.add(v));
        //排序
        plugins = plugins.stream()
                .sorted(Comparator.comparingInt(IPlugin::getPriority).reversed())
                .collect(Collectors.toList());
        //打印消息
        Logger logger = LoggerFactory.getLogger(PluginContainer.class);
        logger.warn("============");
        logger.warn("已加载下列插件：");
        plugins.forEach((v) -> logger.warn("名称 {}\t优先级 {}", v.getName(), v.getPriority()));
        logger.warn("============");
    }

    public List<IPlugin> getPlugins() {
        return this.plugins;
    }
}
