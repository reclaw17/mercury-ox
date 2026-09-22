package com.unsupportedpastels.hermesandroid.ui.refresh

import org.junit.Assert.assertEquals
import org.junit.Test

class OnyxRefreshAdapterTest {
    @Test
    fun noOpDoesNotTouchFakeSink() {
        val sink = FakeOnyxRefreshSink()

        NoOpOnyxRefreshAdapter.refresh(sink)
        NoOpOnyxRefreshAdapter.refresh(sink)

        assertEquals(0, sink.touches)
    }

    @Test
    fun fakeSinkRecordsAnExplicitTouch() {
        val sink = FakeOnyxRefreshSink()

        sink.touch()

        assertEquals(1, sink.touches)
    }
}

private class FakeOnyxRefreshSink : OnyxRefreshSink {
    var touches: Int = 0
        private set

    override fun touch() {
        touches += 1
    }
}
