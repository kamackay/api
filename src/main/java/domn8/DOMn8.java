package domn8;

import domn8.nodes.HeadNode;
import domn8.nodes.HtmlBody;
import domn8.nodes.HtmlPage;
import domn8.nodes.MetaTag;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.dom.DOMAttribute;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static domn8.util.Utils.listOf;
import static domn8.util.Utils.millisToReadableTime;


@Slf4j
public class DOMn8 {

    private static final Charset utf8 = StandardCharsets.UTF_8;

    public static <M> Document<M> generic(
            final Class<M> modelClass,
            final Function<M, HtmlBody> modeler,
            final String title) {
        return new Document<M>(m ->
                new HtmlPage(new HtmlPage.HtmlConfig(),
                        HeadNode.head(new HeadNode.HeadConfig()
                                        .stylesheets(
                                                "/css/main.css",
                                                "/css/bootstrap.min.css")
                                        .iconUrl("/favicon.ico")
                                        .title(title),
                                listOf(
                                        MetaTag.meta("charset", "UTF-8"),
                                        MetaTag.meta("name", "google")
                                                .add("content", "notranslate"),
                                        MetaTag.meta("http-equiv", "Content-Language")
                                                .add("content", "en"),
                                        MetaTag.meta("name", "keywords").
                                                add("content", "kubernetes, twitter, 2020, election, scraper, democratic")
                                )
                        ),
                        modeler.apply(m)));
    }

    public static Attribute makeAttr(final String name, final String value) {
        return new DOMAttribute(new QName(name), value);
    }

    public static class Document<M> {
        private final Function<M, HtmlPage> modeler;

        private Document(final Function<M, HtmlPage> modeler) {
            this.modeler = modeler;
        }

        public RenderResult render(final M model) {
            final long start = System.currentTimeMillis();
            try {
                return RenderResult.success((modeler.apply(model).render().asXML() + "\n").getBytes(utf8));
            } catch (Exception e) {
                log.error("Error Building DOM", e);
                return RenderResult.error("<html><head/><body>Error Rendering!</body></html>\n".getBytes(utf8));
            } finally {
                log.info("Rendered in {}", millisToReadableTime(System.currentTimeMillis() - start));
            }
        }

        public String renderIntoString(final M model) {
            final RenderResult result = renderIntoStream(model);
            return new String(result.stream.readAllBytes());
        }

        public RenderResult renderIntoStream(final M model) {
            final long start = System.currentTimeMillis();
            try {
                final Element renderedDom = modeler.apply(model).render();
                try (final StringWriter writer = new StringWriter()) {
                    renderedDom.write(writer);
                    return RenderResult.success(writer.toString().getBytes(utf8));
                }
            } catch (Exception e) {
                log.error("Error Building DOM", e);
                return RenderResult.error("<html><head/><body>Error Rendering!</body></html>\n".getBytes(StandardCharsets.UTF_8));
            } finally {
                log.info("Rendered to stream in {}", millisToReadableTime(System.currentTimeMillis() - start));
            }
        }

        public void renderJavalin(final Context ctx, final M model) {
            final RenderResult result = this.render(model);
            ctx.contentType(ContentType.HTML)
                    .status(result.getStatus())
                    .result(result.getStream());
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class RenderResult {
        private ByteArrayInputStream stream;
        private int status;

        public static RenderResult success(final ByteArrayInputStream stream) {
            return new RenderResult(stream, 200);
        }

        public static RenderResult success(final byte[] arr) {
            return new RenderResult(new ByteArrayInputStream(arr), 200);
        }

        public static RenderResult error(final ByteArrayInputStream stream) {
            return new RenderResult(stream, 500);
        }

        public static RenderResult error(final byte[] arr) {
            return new RenderResult(new ByteArrayInputStream(arr), 500);
        }
    }

}
