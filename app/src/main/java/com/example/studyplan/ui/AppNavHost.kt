package com.example.studyplan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.studyplan.ui.screens.FlashCardReviewScreen
import com.example.studyplan.ui.screens.NoteCardsScreen
import com.example.studyplan.ui.screens.NoteDetailScreen
import com.example.studyplan.ui.screens.NoteEditScreen
import com.example.studyplan.ui.screens.NoteListScreen
import com.example.studyplan.ui.screens.SummaryReviewScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    noteViewModel: NoteViewModel,
    summaryViewModel: SummaryViewModel,
    flashCardsViewModel: FlashCardsViewModel,
    pdfAttachmentViewModel: PdfAttachmentViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.NoteList.route
    ) {
        composable(Screen.NoteList.route) {
            val notesUiState by noteViewModel.uiState.collectAsStateWithLifecycle()
            NoteListScreen(
                notes = notesUiState.notes,
                topics = notesUiState.topics,
                selectedTopic = notesUiState.selectedTopic,
                onTopicSelected = { topic -> noteViewModel.selectTopic(topic) },
                onAddClick = { navController.navigate(Screen.AddNote.route) },
                onReviewClick = { navController.navigate(Screen.DueFlashcards.route) },
                onNoteClick = { note -> navController.navigate(Screen.ShowNote.createRoute(note.id)) },
                onDeleteClick = { noteId -> noteViewModel.deleteNote(noteId) }
            )
        }

        composable(Screen.DueFlashcards.route) {
            val flashCardsUiState by flashCardsViewModel.uiState.collectAsStateWithLifecycle()
            FlashCardReviewScreen(
                dueCards = flashCardsUiState.dueCards,
                onReview = { card, rating -> flashCardsViewModel.reviewCard(card, rating) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddNote.route) {
            val pdfUiState by pdfAttachmentViewModel.uiState.collectAsStateWithLifecycle()
            NoteEditScreen(
                note = null,
                onSave = { note ->
                    noteViewModel.addNote(note.title, note.topicName, note.content, note.summary)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                onPdfPicked = { uri -> pdfAttachmentViewModel.extract(uri) },
                extractedPdfText = pdfUiState.content,
                onExtractedTextConsumed = { pdfAttachmentViewModel.clear() },
                isExtractingPdf = pdfUiState.isGenerating,
                pdfError = pdfUiState.error,
                onPdfErrorShown = { pdfAttachmentViewModel.clearError() }
            )
        }
        composable(
            route = Screen.EditNote.route,
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable
            val note = noteViewModel.findById(noteId) ?: return@composable
            val pdfUiState by pdfAttachmentViewModel.uiState.collectAsStateWithLifecycle()
            NoteEditScreen(
                note = note,
                onSave = { updated ->
                    noteViewModel.updateNote(updated)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                onPdfPicked = { uri -> pdfAttachmentViewModel.extract(uri) },
                extractedPdfText = pdfUiState.content,
                onExtractedTextConsumed = { pdfAttachmentViewModel.clear() },
                isExtractingPdf = pdfUiState.isGenerating,
                pdfError = pdfUiState.error,
                onPdfErrorShown = { pdfAttachmentViewModel.clearError() }
            )
        }
        composable(
            route = Screen.ShowNote.route,
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable
            val note = noteViewModel.findById(noteId) ?: return@composable
            val flashCardsUiState by flashCardsViewModel.uiState.collectAsStateWithLifecycle()
            NoteDetailScreen(
                note = note,
                onEditClick = { note -> navController.navigate(Screen.EditNote.createRoute(note.id)) },
                onBack = { navController.popBackStack() },
                onGenerateSummary = { note -> navController.navigate(Screen.ReviewSummary.createRoute(note.id)) },
                onGenerateFlashcards = { note ->
                    flashCardsViewModel.generateCards(note) {
                        navController.navigate(Screen.NoteCards.createRoute(note.id))
                    }
                },
                onManageCards = { note -> navController.navigate(Screen.NoteCards.createRoute(note.id)) },
                isGeneratingFlashcards = flashCardsUiState.isGenerating,
                flashcardError = flashCardsUiState.generationError,
                onFlashcardErrorShown = { flashCardsViewModel.clearGenerationError() }
            )
        }

        composable(
            route = Screen.NoteCards.route,
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable

            // The query happens here, when the cards screen opens — never on the note screen.
            LaunchedEffect(noteId) { flashCardsViewModel.loadCardsForNote(noteId) }

            val flashCardsUiState by flashCardsViewModel.uiState.collectAsStateWithLifecycle()
            NoteCardsScreen(
                cards = flashCardsUiState.cardsForNote,
                onAddCard = { front, back -> flashCardsViewModel.addCard(noteId, front, back) },
                onUpdateCard = { card -> flashCardsViewModel.updateCard(card) },
                onDeleteCard = { card -> flashCardsViewModel.deleteCard(card) },
                onBack = {
                    flashCardsViewModel.clearNewCardIds()
                    navController.popBackStack()
                },
                newCardIds = flashCardsUiState.newCardIds
            )
        }

        composable(
            route = Screen.ReviewSummary.route,
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable
            val note = noteViewModel.findById(noteId) ?: return@composable

            // Generate once when this screen is first shown for the note.
            LaunchedEffect(noteId) {
                summaryViewModel.generate(note)
            }

            val summaryUiState by summaryViewModel.uiState.collectAsStateWithLifecycle()
            SummaryReviewScreen(
                summary = summaryUiState.generatedSummary.orEmpty(),
                isLoading = summaryUiState.isGenerating,
                errorMessage = summaryUiState.error,
                onAccept = {
                    // Saving the summary onto the note is a note mutation.
                    summaryViewModel.uiState.value.generatedSummary?.let { summary ->
                        noteViewModel.updateNote(note.copy(summary = summary))
                    }
                    summaryViewModel.clear()
                    navController.popBackStack()
                },
                onCancel = {
                    summaryViewModel.clear()
                    navController.popBackStack()
                },
                onRetry = { summaryViewModel.generate(note) }
            )
        }
    }
}
