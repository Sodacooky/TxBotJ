package sodacooky.txbotj.utils.badwords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sodacooky.txbotj.utils.global.GlobalValue;
import toolgood.words.StringSearch;

import java.util.Arrays;
import java.util.List;

@Component
public class BadWordsChecker {

    private final StringSearch innerChecker; //敏感词匹配工具类

    @Autowired
    public BadWordsChecker(GlobalValue globalValue) {
        //获取工具类
        innerChecker = new StringSearch();
        Logger logger = LoggerFactory.getLogger(BadWordsChecker.class);
        //从数据库加载敏感词
        String badWords = globalValue.readValue("bad_words");
        if (null == badWords) {
            logger.error("未在数据库global.bad_words中读取到敏感词。");
            return;
        }
        //设置到库
        List<String> badWordsList = Arrays.asList(badWords.split("\n"));
        innerChecker.SetKeywords(badWordsList);
        //打印数量
        logger.info("已读取 {} 个敏感词。", badWordsList.size());
    }

    public boolean isContainsBadWords(String beChecked) {
        return innerChecker.ContainsAny(beChecked);
    }

}
