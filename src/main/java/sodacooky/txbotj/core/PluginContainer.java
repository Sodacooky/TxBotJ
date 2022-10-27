package sodacooky.txbotj.core;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            log.error("没有插件");
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
        log.info("已加载下列插件：");
        System.out.println("==========");
        plugins.forEach((v) -> System.out.println("名称: " + v.getName() + ", 优先级: " + v.getPriority()));
        System.out.println("==========");
    }

    public List<IPlugin> getPlugins() {
        return this.plugins;
    }
}
