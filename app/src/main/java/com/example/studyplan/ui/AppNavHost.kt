package com.example.studyplan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
            NoteListScreen(
                notes = noteViewModel.notes,
                topics = noteViewModel.topics,
                selectedTopic = noteViewModel.selectedTopic,
                onTopicSelected = { topic -> noteViewModel.selectTopic(topic) },
                onAddClick = { navController.navigate(Screen.AddNote.route) },
                onReviewClick = { navController.navigate(Screen.DueFlashcards.route) },
                onNoteClick = { note -> navController.navigate(Screen.ShowNote.createRoute(note.id)) },
                onDeleteClick = { noteId -> noteViewModel.deleteNote(noteId) }
            )
        }

        composable(Screen.DueFlashcards.route) {
            FlashCardReviewScreen(
                dueCards = flashCardsViewModel.dueCards,
                onReview = { card, rating -> flashCardsViewModel.reviewCard(card, rating) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddNote.route) {
            NoteEditScreen(
                note = null,
                onSave = { note ->
                    noteViewModel.addNote(note.title, note.topicName, note.content, note.summary)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                onPdfPicked = { uri -> pdfAttachmentViewModel.extract(uri) },
                extractedPdfText = pdfAttachmentViewModel.content,
                onExtractedTextConsumed = { pdfAttachmentViewModel.clear() },
                isExtractingPdf = pdfAttachmentViewModel.isGenerating,
                pdfError = pdfAttachmentViewModel.error,
                onPdfErrorShown = { pdfAttachmentViewModel.clearError() }
            )
        }
        composable(
            route = Screen.EditNote.route,
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable
            val note = noteViewModel.findById(noteId) ?: return@composable
            NoteEditScreen(
                note = note,
                onSave = { updated ->
                    noteViewModel.updateNote(updated)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                onPdfPicked = { uri -> pdfAttachmentViewModel.extract(uri) },
                extractedPdfText = pdfAttachmentViewModel.content,
                onExtractedTextConsumed = { pdfAttachmentViewModel.clear() },
                isExtractingPdf = pdfAttachmentViewModel.isGenerating,
                pdfError = pdfAttachmentViewModel.error,
                onPdfErrorShown = { pdfAttachmentViewModel.clearError() }
            )
        }
        composable(
            route = Screen.ShowNote.route,
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable
            val note = noteViewModel.findById(noteId) ?: return@composable
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
                isGeneratingFlashcards = flashCardsViewModel.isGenerating,
                flashcardError = flashCardsViewModel.generationError,
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

            NoteCardsScreen(
                cards = flashCardsViewModel.cardsForNote,
                onAddCard = { front, back -> flashCardsViewModel.addCard(noteId, front, back) },
                onUpdateCard = { card -> flashCardsViewModel.updateCard(card) },
                onDeleteCard = { card -> flashCardsViewModel.deleteCard(card) },
                onBack = {
                    flashCardsViewModel.clearNewCardIds()
                    navController.popBackStack()
                },
                newCardIds = flashCardsViewModel.newCardIds
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

            SummaryReviewScreen(
                summary = summaryViewModel.generatedSummary.orEmpty(),
                isLoading = summaryViewModel.isGenerating,
                errorMessage = summaryViewModel.error,
                onAccept = {
                    // Saving the summary onto the note is a note mutation.
                    summaryViewModel.generatedSummary?.let { summary ->
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
