package htmlflow.test

import htmlflow.*
import htmlflow.test.model.Track
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.xmlet.htmlapifaster.*
import reactor.core.publisher.Flux
import java.lang.String.format
import java.time.Duration
import java.time.LocalDate
import java.util.*


/**
 * These tests do not contain any assertion because they are only a sample for README.md
 * and HtmlFlow site examples.
 */
class TestKotlinExtensionsOnPartials {
    /**
     * Sample showcase of data binding with HtmlDoc
     */
    private fun trackDoc(out: Appendable, track: Track) {
        HtmlFlow
            .doc(out)
            .html {
                body {
                    ul {
                        li { text(format("Artist: %s", track.artist)) }
                        li { text(format("Track: %s", track.name)) }
                        if (track.diedDate != null) {
                            li { text(format("Died in %d", track.diedDate.year)) }
                        }
                    }
                }
            }
    }

    /**
     * Sample showcase of data binding with HtmlView
     */
    @Test
    fun trackView() {
        val trackView = view<Track> {
            html {
                body {
                    ul {
                        dyn { track: Track ->
                            li { text(format("Artist: %s", track.artist)) }
                            li { text(format("Track: %s", track.name)) }
                            if (track.diedDate != null) {
                                li { text(format("Died in %d", track.diedDate.year)) }
                            }
                        }
                    }
                }
            }
        }
        val spaceOddity = Track("David Bowie", "Space Oddity", LocalDate.of(2016, 1, 10))
        val actual = StringBuilder()
        trackDoc(actual, spaceOddity)
        assertEquals(actual.toString(), trackView.render(spaceOddity))
//        trackView.setOut(System.out).write(spaceOddity);
    }

    /**
     * Sample showcase of loop with HtmlDoc
     */
    private fun playlistDoc(out: Appendable, tracks: List<Track>) {
        HtmlFlow
            .doc(out)
            .html {
                body {
                    table {
                        tr {
                            th { text("Artist") }
                            th { text("Track") }
                        }
                        tracks.forEach { track ->
                            tr { td { text(track.artist) } }
                            tr { td { text(track.name) } }
                        }
                    } // table
                } // body
            } // html
    }

    /**
     * Sample showcase of loop with HtmlView
     */
    @Test
    fun playlistView() {
        val playlistView = view<List<Track>> {
            html {
                body {
                    table {
                        tr {
                            th { text("Artist") }
                            th { text("Track") }
                        }
                        dyn { tracks: List<Track> -> tracks
                            .forEach { track ->
                                tr { td { text(track.artist) } }
                                tr { td { text(track.name) } }
                            }
                        }
                    } // table
                } // body
            } // html
        }
        val tracks = listOf(
            Track("David Bowie", "Space Oddity", LocalDate.of(2016, 1, 10)),
            Track("U2", "Bad"),
            Track("Queen", "Under Pressure")
        )
        val actual = StringBuilder()
        playlistDoc(actual, tracks)
        assertEquals(actual.toString(), playlistView.render(tracks))
//        playlistView.setOut(System.out).write(tracks);
    }
    /**
     * Sample showcase of loop with HtmlViewAsync
     */
    @Test
    fun playlistViewAsync() {
        val playlistView = viewAsync<Flux<Track>> {
            html {
                body {
                    table {
                        tr {
                            th { text("Artist") }
                            th { text("Track") }
                        }
                        await { tracks: Flux<Track>, resume -> tracks
                            .doOnComplete(resume)
                            .doOnNext{ track ->
                                tr { td { text(track.artist) } }
                                tr { td { text(track.name) } }
                            }
                        }
                    } // table
                } // body
            } // html
        }
        val tracks = Arrays.asList(
            Track("David Bowie", "Space Oddity", LocalDate.of(2016, 1, 10)),
            Track("U2", "Bad"),
            Track("Queen", "Under Pressure")
        )
        val tracksFlux = Flux
            .fromIterable(tracks)
            .delayElements(Duration.ofMillis(10))
        val expected = StringBuilder()
        playlistDoc(expected, tracks)
        playlistView.renderAsync(tracksFlux).thenAccept { actual: String? ->
            assertEquals(
                expected.toString(),
                actual
            )
        }
    }
    /**
     * Sample showcase of loop with HtmlViewSuspend
     */
    @Test
    fun playlistViewSuspend() {
        val playlistView = viewSuspend<Flux<Track>> {
            html {
                body {
                    table {
                        tr {
                            th { text("Artist") }
                            th { text("Track") }
                        }
                        suspending { tracks: Flux<Track> -> tracks
                            .asFlow()
                            .collect { track ->
                                tr { td { text(track.artist) } }
                                tr { td { text(track.name) } }
                            }
                        }
                    } // table
                } // body
            } // html
        }
        val tracks = Arrays.asList(
            Track("David Bowie", "Space Oddity", LocalDate.of(2016, 1, 10)),
            Track("U2", "Bad"),
            Track("Queen", "Under Pressure")
        )
        val tracksFlux = Flux
            .fromIterable(tracks)
            .delayElements(Duration.ofMillis(10))
        val expected = StringBuilder()
        playlistDoc(expected, tracks)
        runBlocking {
            val actual = playlistView.render(tracksFlux)
            assertEquals(
                expected.toString(),
                actual
            )
        }
    }
    private fun partialInputField(container: Div<*>, label: String, id: String, value: Any) {
        container
            .div {
                attrClass("form-group")
                label { text(label) }
                input {
                    attrClass("form-control")
                    attrType(EnumTypeInputType.TEXT)
                    attrId(id)
                    attrName(id)
                    attrValue(value.toString())
                }
            }
    }
    private fun ownerTemplate(container: Div<*>) {
        container
            .h2 { text("Owner") }
            .form {
                attrMethod(EnumMethodType.POST)
                div {
                    attrClass("form-group has-feedback")
                    dyn { owner: Owner ->
                        partialInputField(this, "Name", "name", owner.name)
                        partialInputField(this, "Address", "address", owner.address)
                    }
                }
            }
    }
    private fun navbarFragment(nav: Nav<*>) {

    }

    private fun ownerView(navbar: (Nav<*>) -> Unit, content: (Div<*>) -> Unit): HtmlView<Owner> {
        return view<Owner> {
            html {
                head {
                    title { text("PetClinic :: a Spring Framework demonstration") }
                    link { attrRel(EnumRelType.STYLESHEET).attrHref("/resources/css/petclinic.css") }
                }
                body {
                    nav { navbar(this) }
                    div {
                        attrClass("container xd-container")
                        content(this)
                    } // div
                } // body
            } // html
        }
    }

    @Test fun testOwnerView() {
        val view = ownerView(::navbarFragment, ::ownerTemplate)
        // view.setOut(System.out).write(Owner("Ze Manel", "Rua da Alfandega"))
    }

    class Owner(val name: String, val address: String)
}