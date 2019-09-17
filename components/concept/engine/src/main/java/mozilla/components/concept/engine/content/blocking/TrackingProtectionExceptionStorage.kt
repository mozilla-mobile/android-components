package mozilla.components.concept.engine.content.blocking

import mozilla.components.concept.engine.EngineSession

interface TrackingProtectionExceptionStorage {
    fun getAll(onFinish: (List<String>) -> Unit)
    fun add(session:EngineSession)
    fun remove(session:EngineSession)
    fun containsAsync(session: EngineSession, onFinish: (Boolean) -> Unit)
    fun removeAll()
}