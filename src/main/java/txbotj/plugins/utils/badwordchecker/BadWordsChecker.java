package txbotj.plugins.utils.badwordchecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolgood.words.StringSearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 敏感词判断
 */
public class BadWordsChecker {

    public static boolean isContainsBadWords(String beChecked) {
        if (!isLoaded) {
            logger.error("Words haven't been loaded");
            return false;
        }
        return innerChecker.ContainsAny(beChecked);
    }

    public static void loadBadWords() {
        //非空什么都不坐
        if (isLoaded) return;
        isLoaded = true;
        //打开并读取
        try {
            //从classpath打开流
            InputStream resourceAsStream = BadWordsChecker.class.getClassLoader().getResourceAsStream("bad_words.txt");
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

    private final static StringSearch innerChecker = new StringSearch();
    private static boolean isLoaded = false;
    private final static Logger logger = LoggerFactory.getLogger(BadWordsChecker.class);
}
