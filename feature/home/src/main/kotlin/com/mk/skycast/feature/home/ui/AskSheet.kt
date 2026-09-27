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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mk.skycast.core.designsystem.components.GlassCard
import com.mk.skycast.core.designsystem.components.SkyChoiceRow
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.domain.ask.AskQuestion
import com.mk.skycast.core.domain.ask.ExerciseKind
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
                AnswerBody(
                    display = askText.display(answer, now),
                    modifier = Modifier.bringIntoViewRequester(answerRequester),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerBody(display: com.mk.skycast.core.ui.ask.AskDisplay, modifier: Modifier = Modifier) {
    var reasonsExpanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.medium)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(display.headline, style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
            display.chips.forEach { chip ->
                AssistChip(onClick = {}, label = { Text(chip) })
            }
        }
        AiExplanationSlot()
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

@Composable
private fun AiExplanationSlot() = Unit
