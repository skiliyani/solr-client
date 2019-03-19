import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

public class TestUnicode {

  public static final String UNICODE_REGEX = "[\\\\]{2}(u([a-f]|[0-9]){4})";

  public static void main(String[] args) throws UnsupportedEncodingException {

    String s = args[0]; // the text to unescape is the first program arg

    UnicodeUnescaper unicodeUnescaper = new UnicodeUnescaper();
    
    String fixedCodepoints = s.replaceAll(UNICODE_REGEX, "\\\\$1");
    fixedCodepoints = unicodeUnescaper.translate(fixedCodepoints);

    System.out.println("Original: " + s);
    System.out.println("Unescaped: " + fixedCodepoints);

  }
}
