package fr.stefwashcar.service;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class MarkdownRenderer {
    private static final Pattern LIST_WITHOUT_BLANK =
            Pattern.compile("([^\\n])\\n((?:-|\\*|\\+|\\d+\\.)\\s+)", Pattern.MULTILINE);

    private final Parser parser;
    private final HtmlRenderer renderer;

    private final Safelist safelist = Safelist.none()
            .addTags("p","br","strong","em","ul","ol","li","blockquote","code","pre","a",
                    "table","thead","tbody","tr","th","td","del")
            .addAttributes("a","href","title")
            .addProtocols("a","href","http","https","mailto");

    public MarkdownRenderer() {
        List<Extension> extensions = List.of(
                TablesExtension.create(),
                StrikethroughExtension.create()
        );
        parser = Parser.builder().extensions(extensions).build();
        renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).build();
    }

    public String toHtml(String markdown) {
        String md = normalize(markdown);
        if (md.isBlank()) return "";
        md = LIST_WITHOUT_BLANK.matcher(md).replaceAll("$1\n\n$2");
        Node document = parser.parse(md);
        return Jsoup.clean(renderer.render(document), safelist);
    }

    public String toText(String markdown) {
        return normalize(markdown).replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String normalize(String value) {
        String s = value == null ? "" : value;
        s = s.replace("\r\n","\n").replace("\r","\n");
        s = s.replaceAll("[\\p{Cc}&&[^\\t\\n]]", "");
        return s.trim();
    }
}
