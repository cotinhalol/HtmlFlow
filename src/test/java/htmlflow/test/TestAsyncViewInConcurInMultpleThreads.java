package htmlflow.test;

import htmlflow.HtmlFlow;
import htmlflow.HtmlPage;
import htmlflow.HtmlView;
import htmlflow.HtmlViewAsync;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;

public class TestAsyncViewInConcurInMultpleThreads {

    @Test
    public void testMultipleThreadsInViewAsync() throws InterruptedException {
        HtmlViewAsync<Object> view = HtmlFlow.viewAsync(TestAsyncViewInConcurInMultpleThreads::template).threadSafe();
        checkRender(() -> view.renderAsync().join());
    }

    @Test
    public void testMultipleThreadsInView() throws InterruptedException {
        HtmlView<Object> view = HtmlFlow.view(TestAsyncViewInConcurInMultpleThreads::template).threadSafe();
        checkRender(() -> view.render());
    }

    public void checkRender(Supplier<String> render) throws InterruptedException {
        // out.println("start");
        final int threadCount = 50;
        // AtomicInteger left = new AtomicInteger(threadCount);
        Thread thread[] = new Thread[threadCount];
        String html[] = new String[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i;
            thread[i] = new Thread(() -> {
                try {
                    html[threadNumber] = render.get();
                    // out.println("Thread " + threadNumber + " exited, remaining: " + left.decrementAndGet());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        for (int i = 0; i < threadCount; i++) {
            thread[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            thread[i].join();
            assertEquals(expectedHtml, html[i]);
        }
        // out.println("end");
    }

    static void template(HtmlPage view) {
        view.div().span().of(span -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    span.a().attrHref("link").text("text").__().of(s -> {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        s.a().attrHref("link2").text("text2").__();
                    });
                }).__()
                .__();
    }

    final static String expectedHtml = "<div>\n" +
            "\t<span>\n" +
            "\t\t<a href=\"link\">\n" +
            "\t\t\ttext\n" +
            "\t\t</a>\n" +
            "\t\t<a href=\"link2\">\n" +
            "\t\t\ttext2\n" +
            "\t\t</a>\n" +
            "\t</span>\n" +
            "</div>";
}
