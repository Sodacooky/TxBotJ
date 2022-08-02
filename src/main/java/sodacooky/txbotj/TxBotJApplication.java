package sodacooky.txbotj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import sodacooky.txbotj.core.PluginContainer;

@SpringBootApplication
@EnableScheduling
public class TxBotJApplication {

    public static void main(String[] args) {
        //启动Spring相关
        ApplicationContext ctx = SpringApplication.run(TxBotJApplication.class, args);
        //加载插件
        ctx.getBean(PluginContainer.class).loadPlugins(ctx);
    }

}
