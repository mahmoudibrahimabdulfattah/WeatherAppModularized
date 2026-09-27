package com.mk.skycast.feature.routine.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.ui.format.rememberWeatherFormatter
import com.mk.skycast.feature.routine.R
import com.mk.skycast.feature.routine.RoutineIntent
import com.mk.skycast.feature.routine.RoutineState
import com.mk.skycast.feature.routine.RoutineStep

/** Three short, skippable steps: week → trips & outings → nightly brief. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(state: RoutineState, onIntent: (RoutineIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditingExisting) R.string.routine_title_edit else R.string.routine_title,
                        ),
                        Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(RoutineIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.routine_back))
                    }
                },
                actions = {
                    if (state.step == RoutineStep.WEEK && !state.isEditingExisting) {
                        TextButton(onClick = { onIntent(RoutineIntent.SkipClicked) }) {
                            Text(stringResource(R.string.routine_skip))
                        }
                    }
                },
            )
        },
        bottomBar = { if (!state.isLoading) StepFooter(state, onNext = { onIntent(RoutineIntent.NextClicked) }) },
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
        } else {
            val formatter = rememberWeatherFormatter(UserPreferences(timeFormat = state.timeFormat))
            AnimatedContent(
                targetState = state.step,
                modifier = Modifier.padding(padding),
                label = "routine-step",
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = SkySpace.contentMaxWidth)
                            .fillMaxWidth()
                            .padding(horizontal = SkySpace.large),
                        verticalArrangement = Arrangement.spacedBy(SkySpace.large),
                    ) {
                        when (step) {
                            RoutineStep.WEEK -> WeekStep(state.draft.week, onIntent)
                            RoutineStep.TRIPS -> TripsStep(state, formatter, onIntent)
                            RoutineStep.BRIEF -> BriefStep(state.draft, formatter, onIntent)
                        }
                        Spacer(Modifier.height(SkySpace.medium))
                    }
                }
            }
        }
    }
    state.outingEditor?.let { outing ->
        OutingEditorSheet(
            outing = outing,
            isNew = state.draft.outings.none { it.id == outing.id },
            canSave = state.canSaveOuting,
            timeFormat = state.timeFormat,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun StepFooter(state: RoutineState, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val stepCount = RoutineStep.entries.size
    val position = state.step.ordinal + 1
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(SkySpace.large),
            verticalArrangement = Arrangement.spacedBy(SkySpace.medium),
        ) {
            LinearProgressIndicator(progress = { position / stepCount.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.routine_step_of, position, stepCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().heightIn(min = SkySpace.touch)) {
                Text(stringResource(if (state.isLastStep) R.string.routine_save else R.string.routine_next))
            }
        }
    }
}
