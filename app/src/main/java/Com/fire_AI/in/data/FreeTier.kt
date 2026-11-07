package Com.fire_AI.`in`.data

/**
 * Chat-capable models that Google's pricing page lists as "Free of charge" on the Free Tier
 * (Standard rows). The API's model list cannot say what is free, so this allowlist is the source
 * of truth. It is a snapshot: update it when Google's page changes.
 *
 * Source: https://ai.google.dev/gemini-api/docs/pricing (page "Last updated 2026-06-02 UTC").
 * Left out on purpose: Live, TTS, image, embedding and robotics models (not text chat), the
 * shut-down Gemini 2.0 models, and Gemma (its API model ids are not published on that page).
 */
object FreeTier {
    const val VERIFIED_ON = "2026-06-02"

    val chatModels: Set<String> = linkedSetOf(
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-3-flash-preview",
        "gemini-2.5-pro",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash-lite-preview-09-2025",
    )

    fun isFree(id: String): Boolean = id in chatModels
}
