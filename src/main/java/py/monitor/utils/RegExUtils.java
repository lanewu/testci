package py.monitor.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExUtils {

    public static String matchString(String str, String regex) {
        String subStr = new String();
        Pattern p = Pattern.compile(regex);
        Matcher mtc = p.matcher(str);
        if (mtc.find()) {
            subStr = mtc.group();
        } else {
            subStr = "";
        }
        return subStr;
    }
}
