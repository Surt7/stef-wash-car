package fr.stefwashcar.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class MailBodyRenderer {
    private static final Pattern LIST_WITHOUT_BLANK =
            Pattern.compile("([^\\n])\\n((?:-|\\*|\\+|\\d+\\.)\\s+)", Pattern.MULTILINE);

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .softbreak("<br>\n")
            .build();

    private final Safelist safelist = Safelist.none()
            .addTags("p","br","strong","em","ul","ol","li","blockquote","code","pre","a")
            .addAttributes("a","href","title")
            .addProtocols("a","href","http","https","mailto");

    public Map<String,String> render(String markdown) {
        String md = markdown == null ? "" : markdown;
        md = md.replace("\r\n","\n").replace("\r","\n");
        md = LIST_WITHOUT_BLANK.matcher(md).replaceAll("$1\n\n$2");

        Node document = parser.parse(md);
        String safeHtml = Jsoup.clean(renderer.render(document), safelist);
        String text = md.replaceAll("\\n{3,}", "\n\n").trim();

        Map<String,String> result = new LinkedHashMap<>();
        result.put("html", safeHtml);
        result.put("text", text);
        return result;
    }
}
