package com.mk.skycast.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.GlassCard
import com.mk.skycast.core.designsystem.components.SkyChoiceRow
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.domain.ai.AiWording
import com.mk.skycast.core.domain.ai.RephraseRequest
import com.mk.skycast.core.domain.ask.AskQuestion
import com.mk.skycast.core.domain.ask.ExerciseKind
import com.mk.skycast.core.ui.ask.AskDisplay
import com.mk.skycast.core.ui.ask.AskText
import com.mk.skycast.feature.home.AskSheetState
import com.mk.skycast.feature.home.HomeIntent
import com.mk.skycast.feature.home.R
import java.time.Instant

@Composable
internal fun AskEntryRow(locationName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(SkyIconSize.medium))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_ask_entry_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.home_ask_entry_subtitle, locationName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyColors.MutedSky,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun AskSheet(
    sheet: AskSheetState,
    askText: AskText,
    now: Instant,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val latestOnIntent by rememberUpdatedState(onIntent)
    val answerRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(sheet.answer) {
        if (sheet.answer != null) answerRequester.bringIntoView()
    }
    ModalBottomSheet(
        onDismissRequest = { onIntent(HomeIntent.AskDismissed) },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scroll)
                .padding(horizontal = SkySpace.large)
                .padding(bottom = SkySpace.large),
            verticalArrangement = Arrangement.spacedBy(SkySpace.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SkySpace.tiny)) {
                Text(
                    stringResource(R.string.home_ask_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(
                        R.string.home_ask_context,
                        sheet.location.name,
                        askText.dateRange(now),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
                AskQuestion.entries.forEach { question ->
                    FilterChip(
                        selected = sheet.question == question,
                        onClick = { onIntent(HomeIntent.AskQuestionSelected(question)) },
                        label = { Text(askText.questionLabel(question)) },
                    )
                }
            }
            if (sheet.question == AskQuestion.BEST_EXERCISE_TIME) {
                SkyChoiceRow(
                    title = null,
                    options = ExerciseKind.entries,
                    selected = sheet.exercise,
                    label = { askText.exerciseLabel(it) },
                    onSelect = { onIntent(HomeIntent.AskExerciseSelected(it)) },
                )
            }
            sheet.answer?.let { answer ->
                val display = askText.display(answer, now)
                val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
                val request = remember(display.headline, display.chips, display.reasons, languageTag) {
                    sheet.question?.let { question ->
                        RephraseRequest(
                            question = question,
                            languageTag = languageTag,
                            headline = display.headline.withoutIsolates(),
                            facts = display.chips.map { it.withoutIsolates() },
                            reasons = display.reasons.map { it.withoutIsolates() },
                        )
                    }
                }
                LaunchedEffect(request) { request?.let { latestOnIntent(HomeIntent.AskWordingRequested(it)) } }
                AnswerBody(
                    display = display,
                    aiWording = sheet.aiWording,
                    onConsent = { onIntent(HomeIntent.AskAiConsentGiven(it)) },
                    modifier = Modifier.bringIntoViewRequester(answerRequester),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerBody(
    display: AskDisplay,
    aiWording: AiWording?,
    onConsent: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reasonsExpanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.medium)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(display.headline, style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
            display.chips.forEach { chip ->
                AssistChip(onClick = {}, label = { Text(chip) })
            }
        }
        aiWording?.let { AiSection(it, onConsent) }
        TextButton(onClick = {
            reasonsExpanded = !reasonsExpanded
        }, modifier = Modifier.heightIn(min = SkySpace.touch)) {
            Text(stringResource(R.string.home_ask_why))
        }
        AnimatedVisibility(reasonsExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(SkySpace.small)) {
                display.reasons.forEach { reason ->
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Text(
                            reason,
                            modifier = Modifier.padding(horizontal = SkySpace.medium, vertical = SkySpace.small),
                        )
                    }
                }
            }
        }
        Text(
            display.freshness,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Bidi isolates help rendering but mean nothing to the model. */
private fun String.withoutIsolates(): String = filterNot { it in '\u2066'..'\u2069' }

@Composable
private fun AiSection(wording: AiWording, onConsent: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    when (wording) {
        AiWording.Hidden, AiWording.Declined -> Unit

        AiWording.NeedsConsent -> AiConsentCard(onConsent, modifier)

        AiWording.Loading -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkySpace.small),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(SkyIconSize.small), strokeWidth = 2.dp)
            Text(stringResource(R.string.home_ai_preparing), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        is AiWording.Ready -> Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.tiny)) {
            Text(wording.text, style = MaterialTheme.typography.bodyLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkySpace.tiny),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(SkyIconSize.small))
                Text(
                    stringResource(R.string.home_ai_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AiWording.DailyLimitReached -> AiNote(stringResource(R.string.home_ai_limit), modifier)

        AiWording.Unavailable -> AiNote(stringResource(R.string.home_ai_unavailable), modifier)
    }
}

@Composable
private fun AiNote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Asked once, before the first request: what is sent, to whom, and adults only. */
@Composable
private fun AiConsentCard(onConsent: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    var isAdult by rememberSaveable { mutableStateOf(false) }
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(SkySpace.medium), verticalArrangement = Arrangement.spacedBy(SkySpace.small)) {
            Text(stringResource(R.string.home_ai_consent_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.home_ai_consent_body), style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SkySpace.touch)
                    .toggleable(value = isAdult, role = Role.Checkbox, onValueChange = { isAdult = it }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isAdult, onCheckedChange = null)
                Text(stringResource(R.string.home_ai_consent_adult))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
                TextButton(onClick = { onConsent(false) }, modifier = Modifier.heightIn(min = SkySpace.touch)) {
                    Text(stringResource(R.string.home_ai_consent_decline))
                }
                Button(
                    onClick = { onConsent(true) },
                    enabled = isAdult,
                    modifier = Modifier.weight(1f).heightIn(min = SkySpace.touch),
                ) { Text(stringResource(R.string.home_ai_consent_accept)) }
            }
        }
    }
}
