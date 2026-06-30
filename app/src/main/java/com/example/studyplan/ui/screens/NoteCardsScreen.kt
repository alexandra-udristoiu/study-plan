package com.example.studyplan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studyplan.data.FlashCard
import com.example.studyplan.domain.flashcard.Sm2ScheduleFactory
import com.example.studyplan.ui.theme.StudyPlanTheme

/**
 * Manages the flashcards belonging to a single note: lists them and lets the user
 * add, edit, or delete a card. Cards are loaded by the caller (see AppNavHost),
 * so this screen only renders [cards] and forwards user actions.
 *
 * @param cards the note's flashcards, in display order.
 * @param onAddCard invoked with the front/back text of a new card.
 * @param onUpdateCard invoked with an existing card carrying edited text.
 * @param onDeleteCard invoked with the card to remove.
 * @param onBack invoked when the user navigates back to the note.
 * @param newCardIds ids of just-generated cards to badge as "new".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCardsScreen(
    cards: List<FlashCard>,
    onAddCard: (front: String, back: String) -> Unit,
    onUpdateCard: (FlashCard) -> Unit,
    onDeleteCard: (FlashCard) -> Unit,
    onBack: () -> Unit,
    newCardIds: Set<Int> = emptySet(),
) {
    // Closed = no dialog; New = add dialog; Edit = edit dialog for a specific card.
    var editor by remember { mutableStateOf<CardEditor>(CardEditor.Closed) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashcards") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editor = CardEditor.New }) {
                Icon(Icons.Default.Add, contentDescription = "Add flashcard")
            }
        }
    ) { innerPadding ->
        if (cards.isEmpty()) {
            CenteredMessage(
                text = "No flashcards yet. Tap + to add one.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    CardRow(
                        card = card,
                        isNew = card.id in newCardIds,
                        onEdit = { editor = CardEditor.Edit(card) },
                        onDelete = { onDeleteCard(card) },
                    )
                }
            }
        }
    }

    // One dialog drives both add and edit: Edit pre-fills from its card, New starts blank.
    when (val current = editor) {
        CardEditor.Closed -> Unit
        is CardEditor.Open -> CardEditorDialog(
            editor = current,
            onDismiss = { editor = CardEditor.Closed },
            onSave = { front, back ->
                when (current) {
                    CardEditor.New -> onAddCard(front, back)
                    is CardEditor.Edit -> onUpdateCard(current.card.copy(front = front, back = back))
                }
                editor = CardEditor.Closed
            },
        )
    }
}

/** One card in the list: front and back preview, with edit and delete actions. */
@Composable
private fun CardRow(
    card: FlashCard,
    isNew: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Merge badge + front + back into one TalkBack stop, so the card reads as
            // "New card, <front>, <back>" instead of stopping on a bare "NEW".
            Column(modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {}) {
                if (isNew) {
                    NewBadge()
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = card.front,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.back,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit card")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete card")
            }
        }
    }
}

/** Small "NEW" pill shown on freshly generated cards. */
@Composable
private fun NewBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "NEW",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            // Announce the clearer "New card" rather than the cryptic visual "NEW".
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clearAndSetSemantics { contentDescription = "New card" },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteCardsScreenPreview() {
    val factory = Sm2ScheduleFactory()
    StudyPlanTheme {
        NoteCardsScreen(
            cards = listOf(
                FlashCard(1, 1, "Capital of France?", "Paris", factory.newSchedule()),
                FlashCard(2, 1, "Largest planet?", "Jupiter", factory.newSchedule()),
            ),
            onAddCard = { _, _ -> },
            onUpdateCard = {},
            onDeleteCard = {},
            onBack = {},
            newCardIds = setOf(2),
        )
    }
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
