package com.example.ai

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

enum class LyriaModelOption(
    val modelId: String,
    val label: String,
    val durationLabel: String,
    val description: String
) {
    CLIP_30S(
        modelId = "lyria-3-clip-preview",
        label = "Lyria 3 Clip (30s)",
        durationLabel = "30s Loop",
        description = "Fast 30-second dopamine-reset loop via lyria-3-clip-preview."
    ),
    PRO_FULL(
        modelId = "lyria-3-pro-preview",
        label = "Lyria 3 Pro (Full Track)",
        durationLabel = "Full Track",
        description = "Full-length multi-section deep work soundtrack via lyria-3-pro-preview."
    )
}

data class GeneratedFocusMusic(
    val filePath: String,
    val mimeType: String,
    val lyricsOrNotes: String,
    val modelId: String,
    val durationLabel: String
)

class FocusMusicGenerator(private val context: Context) {

    private val clipModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = LyriaModelOption.CLIP_30S.modelId,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO, ResponseModality.TEXT)
            }
        )
    }

    private val proModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = LyriaModelOption.PRO_FULL.modelId,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO, ResponseModality.TEXT)
            }
        )
    }

    suspend fun generateFocusTrack(
        prompt: String,
        option: LyriaModelOption
    ): GeneratedFocusMusic {
        val model = when (option) {
            LyriaModelOption.CLIP_30S -> clipModel
            LyriaModelOption.PRO_FULL -> proModel
        }

        val timeoutMs = when (option) {
            LyriaModelOption.CLIP_30S -> 60_000L
            LyriaModelOption.PRO_FULL -> 90_000L
        }

        return try {
            val response = withTimeout(timeoutMs) {
                model.generateContent(prompt)
            }

            val parts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
            val audioPart = parts.filterIsInstance<InlineDataPart>().firstOrNull { part ->
                part.mimeType.startsWith("audio/", ignoreCase = true) ||
                    part.inlineData.isNotEmpty()
            } ?: throw IllegalStateException(
                "No audio track was returned by ${option.modelId}. Check Firebase AI Logic billing/quota or prompt."
            )

            val notes = parts.filterIsInstance<TextPart>()
                .joinToString("\n") { it.text }
                .trim()

            val extension = when {
                audioPart.mimeType.contains("wav", ignoreCase = true) -> "wav"
                audioPart.mimeType.contains("ogg", ignoreCase = true) -> "ogg"
                else -> "mp3"
            }

            val savedFile = withContext(Dispatchers.IO) {
                val dir = File(context.filesDir, "focus_tracks").apply { mkdirs() }
                val file = File(dir, "lyria_${System.currentTimeMillis()}.$extension")
                file.writeBytes(audioPart.inlineData)
                file
            }

            GeneratedFocusMusic(
                filePath = savedFile.absolutePath,
                mimeType = audioPart.mimeType,
                lyricsOrNotes = notes,
                modelId = option.modelId,
                durationLabel = option.durationLabel
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Lyria music generation timed out for ${option.modelId}", e)
            throw IllegalStateException("Music generation timed out after ${timeoutMs / 1000}s. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Lyria music generation failed for ${option.modelId}", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "FocusMusicGenerator"
    }
}
