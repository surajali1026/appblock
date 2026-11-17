package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.LyriaModelOption
import com.example.audio.AudioPlaybackState
import com.example.data.local.FocusTrackEntity
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LuminousEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.viewmodel.MusicGenUiState
import com.example.usage.UsageStatsHelper

private data class SoundscapePreset(val title: String, val prompt: String)

private val FOCUS_PRESETS = listOf(
    SoundscapePreset(
        title = "Deep Coding Lo-Fi",
        prompt = "Instrumental chill lofi beats at 78 BPM with warm Rhodes piano, soft vinyl texture, gentle sub bass, zero vocals, calm deep focus."
    ),
    SoundscapePreset(
        title = "Dopamine Reset Ambient",
        prompt = "Serene binaural ambient soundscape with soft glass chimes, warm analog synth pads, rain atmosphere, slow meditative progression, instrumental."
    ),
    SoundscapePreset(
        title = "Acoustic Flow State",
        prompt = "Gentle fingerpicked acoustic guitar and warm cello harmonics at 72 BPM, peaceful study session atmosphere, instrumental."
    ),
    SoundscapePreset(
        title = "Midnight Synth Drift",
        prompt = "Hypnotic minimal synthwave pulse with muted arpeggios, deep atmospheric pads, 90 BPM, steady concentration groove, instrumental."
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusMusicScreen(
    musicGenState: MusicGenUiState,
    tracks: List<FocusTrackEntity>,
    playbackState: AudioPlaybackState,
    onGenerateMusic: (title: String, prompt: String, option: LyriaModelOption) -> Unit,
    onTogglePlay: (FocusTrackEntity) -> Unit,
    onToggleLoop: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onDeleteTrack: (FocusTrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedModel by remember { mutableStateOf(LyriaModelOption.CLIP_30S) }
    var trackTitle by remember { mutableStateOf("Deep Coding Lo-Fi") }
    var promptText by remember { mutableStateOf(FOCUS_PRESETS.first().prompt) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, SlateOutline),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_focus_soundscape),
                            contentDescription = "Focus Audio Studio Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ObsidianVoid.copy(alpha = 0.25f),
                                            ObsidianVoid.copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = AmberPulse.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "LYRIA 3 FOCUS STUDIO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberPulse,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Replace Reels with Deep Focus Soundscapes",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Generate 30-second reset loops or full-length focus compositions on demand.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Model Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LyriaModelOption.entries.forEach { option ->
                                FilterChip(
                                    selected = selectedModel == option,
                                    onClick = { selectedModel = option },
                                    label = { Text(option.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("model_chip_${option.modelId}")
                                )
                            }
                        }
                        Text(
                            text = selectedModel.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Detox Presets",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FOCUS_PRESETS.forEach { preset ->
                                SuggestionChip(
                                    onClick = {
                                        trackTitle = preset.title
                                        promptText = preset.prompt
                                    },
                                    label = { Text(preset.title) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = trackTitle,
                            onValueChange = { trackTitle = it },
                            label = { Text("Track Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("music_title_field")
                        )

                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            label = { Text("Musical Prompt (genre, BPM, instruments, mood)") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("music_prompt_field")
                        )

                        val isLoading = musicGenState is MusicGenUiState.Loading
                        Button(
                            onClick = { onGenerateMusic(trackTitle, promptText, selectedModel) },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuminousEmerald,
                                contentColor = ObsidianVoid
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_music_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                    color = ObsidianVoid
                                )
                                Text("Composing with ${selectedModel.modelId}...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Generate Focus Audio")
                            }
                        }

                        when (musicGenState) {
                            is MusicGenUiState.Error -> {
                                Surface(
                                    color = CrimsonGuard.copy(alpha = 0.14f),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, CrimsonGuard.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = musicGenState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CrimsonGuard,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            is MusicGenUiState.Success -> {
                                Surface(
                                    color = LuminousEmerald.copy(alpha = 0.14f),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, LuminousEmerald.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Ready & playing: ${musicGenState.trackTitle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LuminousEmerald,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Soundscapes (${tracks.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    FilterChip(
                        selected = playbackState.isLooping,
                        onClick = onToggleLoop,
                        leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) },
                        label = { Text(if (playbackState.isLooping) "Loop On" else "Loop Off") }
                    )
                }
            }

            if (tracks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AmberPulse)
                            Text(
                                text = "Generate your first Lyria 3 focus clip or full track above to replace short-video impulses with deep audio immersion.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(tracks, key = { it.id }) { track ->
                    val isActive = playbackState.activeTrackId == track.id
                    val isPlaying = isActive && playbackState.isPlaying
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) SlateElevated else SlateSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) LuminousEmerald.copy(alpha = 0.6f) else SlateOutline
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onTogglePlay(track) },
                                    modifier = Modifier.testTag("play_track_${track.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = LuminousEmerald
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${track.modelUsed} • ${track.durationLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteTrack(track) },
                                    modifier = Modifier.testTag("delete_track_${track.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete track",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isActive && playbackState.durationMs > 0) {
                                Slider(
                                    value = playbackState.currentPositionMs.toFloat()
                                        .coerceIn(0f, playbackState.durationMs.toFloat()),
                                    onValueChange = { onSeekTo(it.toInt()) },
                                    valueRange = 0f..playbackState.durationMs.toFloat()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = UsageStatsHelper.formatDuration(playbackState.currentPositionMs.toLong()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = UsageStatsHelper.formatDuration(playbackState.durationMs.toLong()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (track.lyricsOrNotes.isNotBlank()) {
                                Text(
                                    text = track.lyricsOrNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
