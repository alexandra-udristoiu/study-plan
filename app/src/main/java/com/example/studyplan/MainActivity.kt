package com.example.studyplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.studyplan.ui.AppNavHost
import com.example.studyplan.ui.FlashCardsViewModel
import com.example.studyplan.ui.NoteViewModel
import com.example.studyplan.ui.SummaryViewModel
import com.example.studyplan.ui.theme.StudyPlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as StudyPlanApplication).container

        setContent {
            StudyPlanTheme {
                val navController = rememberNavController()
                val noteViewModel: NoteViewModel = viewModel {
                    NoteViewModel(container.noteRepository)
                }
                val summaryViewModel: SummaryViewModel = viewModel {
                    SummaryViewModel(container.summaryGenerator)
                }
                val flashCardsViewModel: FlashCardsViewModel = viewModel {
                    FlashCardsViewModel(container.flashCardRepository, container.flashCardsGenerator)
                }
                AppNavHost(
                    navController = navController,
                    noteViewModel = noteViewModel,
                    summaryViewModel = summaryViewModel,
                    flashCardsViewModel = flashCardsViewModel,
                )
            }
        }
    }
}
