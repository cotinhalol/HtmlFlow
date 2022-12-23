/*
 * MIT License
 *
 * Copyright (c) 2014-18, mcarvalho (gamboa.pt) and lcduarte (github.com/lcduarte)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package htmlflow;

import htmlflow.visitor.HtmlViewVisitor;
import org.xmlet.htmlapifaster.Html;

import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * Dynamic views can be bound to a Model object.
 *
 * @param <T> The type of the Model bound to this View.
 *
 * @author Miguel Gamboa, Luís Duare
 */
public class HtmlView extends HtmlPage {

    private static final String WRONG_USE_OF_RENDER_WITHOUT_MODEL =
             "Wrong use of HtmlView! You should provide a " +
             "model parameter or use a static view instead!";
    /**
     * This field is like a union with the threadLocalVisitor, being used alternatively.
     * For non thread safe scenarios Visitors maybe shared concurrently by multiple threads.
     * On the other-hand, in thread-safe scenarios each thread must have its own visitor to
     * emit HTML to the output, and we use the threadLocalVisitor field instead.
     */
    private final HtmlViewVisitor visitor;
    /**
     * This issue is regarding ThreadLocal variables that are supposed to be garbage collected.
     * The given example deals with a static field of ThreadLocal which persists beyond an instance.
     * In this case the ThreadLocal is hold in an instance field and should stay with all
     * thread local instances during its entire life cycle.
     */
    @java.lang.SuppressWarnings("squid:S5164")
    private final ThreadLocal<HtmlViewVisitor> threadLocalVisitor;
    private final Supplier<HtmlViewVisitor> visitorSupplier;
    private final boolean threadSafe;
    /**
     * To check whether this view is emitting to PrintStream, or not.
     * Notice since the PrintStream maybe shared by different views processing
     * we cannot ensure thread safety, because concurrent threads maybe emitting
     * different HTML to the same PrintStream.
     */
    protected final Appendable out;
    /**
     * Auxiliary constructor used by clone().
     */
    HtmlView(
        Appendable out,
        Supplier<HtmlViewVisitor> visitorSupplier,
        boolean threadSafe)
    {
        this.out = out;
        this.visitorSupplier = visitorSupplier;
        this.threadSafe = threadSafe;
        if(threadSafe) {
            this.visitor = null;
            this.threadLocalVisitor = ThreadLocal.withInitial(visitorSupplier);
        } else {
            this.visitor = visitorSupplier.get();
            this.threadLocalVisitor = null;
        }
    }

    public final Html<HtmlPage> html() {
        this.getVisitor().write(HEADER);
        return new Html<>(this);
    }

    public final HtmlPage threadSafe(){
        /**
         * PrintStream output is not viable in a multi-thread scenario,
         * because different Visitor instances may share the same PrintStream.
         */
        if(out instanceof PrintStream) {
            throw new IllegalStateException(WRONG_USE_OF_THREADSAFE_ON_VIEWS_WITH_PRINTSTREAM);
        }
        return clone(visitorSupplier, true);
    }

    @Override
    public HtmlViewVisitor getVisitor() {
        return threadSafe
            ? threadLocalVisitor.get()
            : visitor;
    }

    @Override
    public String getName() {
        return "HtmlView";
    }

    public String render() {
        throw new UnsupportedOperationException(WRONG_USE_OF_RENDER_WITHOUT_MODEL);
    }

    public String render(Object model) {
        resolveView(model);
        return readAndReset();
    }

    public void write(Object model) {
        resolveView(model);
    }

    public void write() {
        throw new UnsupportedOperationException(WRONG_USE_OF_RENDER_WITHOUT_MODEL);
    }

    private void resolveView(Object model) {
        HtmlViewVisitor viewVisitor = getVisitor();
        viewVisitor.setAppendable(this.out);
        viewVisitor.resolve(model);
    }

    private String readAndReset() {
        String data = out.toString();
        ((StringBuilder) out).setLength(0);
        return data;
    }

    /**
     * Since HtmlView is immutable this is the preferred way to create a copy of the
     * existing HtmlView instance with a different threadSafe state.
     *
     * @param visitorSupplier
     * @param threadSafe
     */
    protected final HtmlPage clone(
        Supplier<HtmlViewVisitor> visitorSupplier,
        boolean threadSafe)
    {
        return new HtmlView(out, visitorSupplier, threadSafe);
    }

    /**
     * Returns a new instance of HtmlFlow with the same properties of this object
     * but with indented set to the value of isIndented parameter.
     */
    @Override
    public final HtmlPage setIndented(boolean isIndented) {
        return clone(() -> (HtmlViewVisitor) getVisitor().clone(isIndented), false);
    }
}
