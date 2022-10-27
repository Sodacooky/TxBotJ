package sodacooky.txbotj.utils.cmdparser;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CommandParser {
    /**
     * 解析命令行
     *
     * @param cmdline 整行
     * @return 当不是命令前缀开头、不是命令返回null，否则返回所有块
     */
    public List<String> parse(String cmdline) {
        if (!cmdline.startsWith(">>")) return null;
        return Arrays.asList(cmdline.split(" "));
    }
}
