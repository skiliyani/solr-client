import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("\\+source:(.*)\\s");
        Matcher matcher = pattern.matcher("+source:healthgrades +spname:cardiology");

        if(matcher.find()) {
            System.out.println(matcher.group(1));
        }

        Pattern pattern2 = Pattern.compile("^Provider_Taxonomies:(.*)$");
        Matcher matcher2 = pattern2.matcher("Provider_Taxonomies:obstetrics/gynecology");

        if(matcher2.find()) {
            System.out.println(matcher2.group(1).replaceAll("\\W", "_"));
        }
    }
}
