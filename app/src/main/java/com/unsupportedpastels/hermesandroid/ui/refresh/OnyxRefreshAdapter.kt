package com.unsupportedpastels.hermesandroid.ui.refresh

/**
 * Optional seam for a later public e-ink refresh contract.
 *
 * Paper UI does not call this. [NoOpOnyxRefreshAdapter] is the shipping
 * behavior: it returns without an SDK, reflection, or a system setting write.
 */
interface OnyxRefreshAdapter {
    fun refresh(sink: OnyxRefreshSink)
}

/**
 * Write boundary a later approved contract could use.
 * [NoOpOnyxRefreshAdapter] must not call [touch].
 */
fun interface OnyxRefreshSink {
    fun touch()
}

/**
 * Shipping refresh behavior. Returns immediately and does not call [sink].
 */
object NoOpOnyxRefreshAdapter : OnyxRefreshAdapter {
    override fun refresh(sink: OnyxRefreshSink) = Unit
}
