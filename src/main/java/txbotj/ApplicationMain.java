package txbotj;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import txbotj.dispatch.HttpListener;
import txbotj.dispatch.MessageDispatcher;
import txbotj.plugins.GroupRepeater;
import txbotj.plugins.PrivateEchoMan;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class ApplicationMain {
    public static void main(String[] args) throws IOException {
        //以下代码应该是固定不变的//
        Logger logger = LoggerFactory.getLogger(ApplicationMain.class);

        //go-cqhttp的监听地址
        //ApiExecutor.address = "http://127.0.0.1:5700"; //默认值，无需

        //创建HttpServer，将Handler绑定到 "/"，端口为5700对应go-cqhttp
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(5701), 0);
        httpServer.createContext("/", new HttpListener());
        httpServer.setExecutor(null);

        //new出Plugin对象，无需保存，自动装载到MessageDispatcher里
        logger.info("Loading plugins...");
        new PrivateEchoMan();
        new GroupRepeater();
        //打印已经装载的插件
        MessageDispatcher.plugins.forEach((k, v) -> logger.info("Loaded {}", k));

        //启动HttpServer，程序阻塞
        logger.warn("Listening, type 'stop' to exit.");
        httpServer.start();

        while (true) {
            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNext() && "stop".equals(scanner.next())) break;
        }
        logger.warn("Stopping...");
        httpServer.stop(10);
    }
}
