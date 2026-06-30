package com.example.studyplan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studyplan.data.FlashCard
import com.example.studyplan.domain.flashcard.Rating
import com.example.studyplan.ui.theme.StudyPlanTheme

/**
 * Walks through the cards due today, one at a time. The user reads the front and
 * rates their recall; rating reveals the back (the answer), and Next advances to
 * the next due card.
 *
 * Rating reschedules a card and drops it from the deck (the [onReview] handler
 * re-queues it for later today if recall failed), so the front always shows the
 * first remaining card. The just-rated card is held in local state so its back
 * stays visible until Next is pressed.
 *
 * @param dueCards the cards still due for review today, in display order.
 * @param onReview invoked with the shown card and the user's recall rating.
 * @param onBack invoked when the user navigates back to the note list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardReviewScreen(
    dueCards: List<FlashCard>,
    onReview: (FlashCard, Rating) -> Unit,
    onBack: () -> Unit,
) {
    // Non-null while showing the back of the card just rated; null means showing a front.
    var revealedCard by remember { mutableStateOf<FlashCard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Flashcards") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp)

        val revealed = revealedCard
        val frontCard = dueCards.firstOrNull()
        when {
            revealed != null -> BackContent(
                back = revealed.back,
                onNext = { revealedCard = null },
                // Announce the revealed answer; TalkBack users rate without seeing it appear.
                modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            frontCard == null -> CenteredMessage("No flashcards due for review.", modifier)

            else -> FrontContent(
                front = frontCard.front,
                remaining = dueCards.size,
                onReview = { rating ->
                    onReview(frontCard, rating)
                    revealedCard = frontCard
                },
                modifier = modifier,
            )
        }
    }
}

/** The question side: the card face plus the four recall-rating buttons. */
@Composable
private fun FrontContent(
    front: String,
    remaining: Int,
    onReview: (Rating) -> Unit,
    modifier: Modifier = Modifier,
) {
    FaceLayout(
        label = if (remaining == 1) "1 card left" else "$remaining cards left",
        text = front,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Rating.entries.forEach { rating ->
                Button(
                    onClick = { onReview(rating) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(rating.label)
                }
            }
        }
    }
}

/** The answer side: the card face plus a single Next button. */
@Composable
private fun BackContent(
    back: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FaceLayout(
        label = "Answer",
        text = back,
        modifier = modifier,
    ) {
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Next")
        }
    }
}

/** Shared layout for both faces: a small label, the styled card, then the action(s). */
@Composable
private fun FaceLayout(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sit the card in the upper part of the screen rather than dead center.
        Spacer(Modifier.height(48.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        actions()
    }
}

/** Title-cased button label, e.g. AGAIN -> "Again". */
private val Rating.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FrontContentPreview() {
    StudyPlanTheme {
        FrontContent(
            front = "What is the capital of France?",
            remaining = 3,
            onReview = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BackContentPreview() {
    StudyPlanTheme {
        BackContent(
            back = "Paris",
            onNext = {},
        )
    }
}
