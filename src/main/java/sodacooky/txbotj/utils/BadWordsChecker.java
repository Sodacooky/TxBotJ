package sodacooky.txbotj.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import toolgood.words.StringSearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class BadWordsChecker {

    private final StringSearch innerChecker; //敏感词匹配工具类

    public BadWordsChecker() {
        //获取工具类
        innerChecker = new StringSearch();
        Logger logger = LoggerFactory.getLogger(BadWordsChecker.class);
        //加载敏感词库
        try {
            //从classpath打开流，读取bad_words.txt
            InputStream resourceAsStream = BadWordsChecker.class.getClassLoader().getResourceAsStream("bad_words.txt");
            Assert.notNull(resourceAsStream, "请检查敏感词是否存在");
            //转化为buffered，一次读一行
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
            //读取到List
            List<String> badWords = new ArrayList<>();
            while (bufferedReader.ready()) badWords.add(bufferedReader.readLine());
            logger.info("Words loading completed, building tree....");
            //设置给StringSearch工具
            innerChecker.SetKeywords(badWords);
            logger.info("Words tree building completed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isContainsBadWords(String beChecked) {
        return innerChecker.ContainsAny(beChecked);
    }

}
