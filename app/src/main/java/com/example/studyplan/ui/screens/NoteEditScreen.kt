package com.example.studyplan.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.studyplan.data.StudyNote
import com.example.studyplan.ui.theme.StudyPlanTheme

/**
 * A single screen for both creating and editing a note.
 *
 * @param note `null` for "add" mode (blank fields); a non-null note for "edit" mode
 *             (fields pre-filled, id preserved on save).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    note: StudyNote?,
    onSave: (StudyNote) -> Unit,
    onBack: () -> Unit,
    onPdfPicked: (Uri) -> Unit = {},
    extractedPdfText: String? = null,
    onExtractedTextConsumed: () -> Unit = {},
    isExtractingPdf: Boolean = false,
    pdfError: String? = null,
    onPdfErrorShown: () -> Unit = {},
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var topicName by remember { mutableStateOf(note?.topicName ?: "") }
    var summary by remember { mutableStateOf(note?.summary ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }

    val isSaveEnabled = title.isNotBlank() && content.isNotBlank()

    val context = LocalContext.current
    // Name of the PDF the user picked, shown as confirmation.
    // Text extraction into `content` will be wired up later.
    var attachedPdfName by remember { mutableStateOf<String?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedPdfName = context.queryDisplayName(uri) ?: uri.lastPathSegment
            onPdfPicked(uri)
        }
    }

    // When the extractor finishes, drop the PDF's text into the Content field and
    // tell the caller we've consumed it (so it isn't re-applied on the next visit).
    LaunchedEffect(extractedPdfText) {
        if (extractedPdfText != null) {
            content = extractedPdfText
            onExtractedTextConsumed()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    // Surface an extraction failure as a snackbar, then let the caller clear it.
    LaunchedEffect(pdfError) {
        pdfError?.let {
            snackbarHostState.showSnackbar(it)
            onPdfErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) "Add Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Edit: copy keeps the existing id. Add: id = 0 lets Room generate one.
                            val result = note?.copy(
                                title = title,
                                topicName = topicName,
                                summary = summary,
                                content = content
                            ) ?: StudyNote(
                                id = 0,
                                title = title,
                                topicName = topicName,
                                content = content,
                                summary = summary
                            )
                            onSave(result)
                        },
                        enabled = isSaveEnabled
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { innerPadding ->
        val fieldShape = RoundedCornerShape(12.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                singleLine = true
            )
            OutlinedTextField(
                value = topicName,
                onValueChange = { topicName = it },
                label = { Text("Topic") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                singleLine = true
            )
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Summary") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                minLines = 3
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content *") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                minLines = 5,
                trailingIcon = {
                    if (isExtractingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach PDF")
                        }
                    }
                }
            )
            attachedPdfName?.let { name ->
                Text(
                    text = "Attached: $name",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Looks up the human-readable file name for a content Uri, or null if unavailable. */
private fun android.content.Context.queryDisplayName(uri: Uri): String? =
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

@Preview(showBackground = true, name = "Add mode")
@Composable
private fun NoteEditScreenAddPreview() {
    StudyPlanTheme {
        NoteEditScreen(note = null, onSave = {}, onBack = {})
    }
}

@Preview(showBackground = true, name = "Edit mode")
@Composable
private fun NoteEditScreenEditPreview() {
    StudyPlanTheme {
        NoteEditScreen(
            note = StudyNote(1, "Neural Networks", "ML", "A neural network detects patterns in data.", "Inspired by the brain"),
            onSave = {},
            onBack = {}
        )
    }
}
