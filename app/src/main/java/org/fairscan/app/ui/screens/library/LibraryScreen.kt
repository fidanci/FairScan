/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.fairscan.app.ui.screens.library

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.fairscan.app.R
import org.fairscan.app.data.LibraryDocument
import org.fairscan.app.ui.Navigation
import org.fairscan.app.ui.components.AppOverflowMenu
import java.util.Date

private enum class LibrarySort { NAME, DATE, SIZE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    hasDraft: Boolean,
    navigation: Navigation,
    onScan: () -> Unit,
    onResumeDraft: () -> Unit,
    onOpen: (LibraryDocument) -> Unit,
    onShare: (LibraryDocument) -> Unit,
    onShareSelected: (List<LibraryDocument>) -> Unit,
    onAddDocuments: () -> Unit,
    onFindOldDocuments: () -> Unit,
    onRename: (LibraryDocument, String) -> Unit,
    onDelete: (List<LibraryDocument>) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    var deletingDocument by remember { mutableStateOf<LibraryDocument?>(null) }
    var renamingDocument by remember { mutableStateOf<LibraryDocument?>(null) }
    var editedName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(LibrarySort.DATE) }
    var sortExpanded by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf(setOf<String>()) }
    val selectedDocuments = state.documents.filter { it.uri.toString() in selectedUris }
    val visibleDocuments = state.documents
        .filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        .let { documents -> when (sort) {
            LibrarySort.NAME -> documents.sortedBy { it.name.lowercase() }
            LibrarySort.DATE -> documents.sortedByDescending { it.modifiedAt }
            LibrarySort.SIZE -> documents.sortedByDescending { it.size }
        } }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text(if (selectionMode) stringResource(R.string.library_selected_count, selectedDocuments.size)
                else stringResource(R.string.library_title)) },
            actions = {
                if (selectionMode) {
                    IconButton(onClick = { if (selectedDocuments.isNotEmpty()) onShareSelected(selectedDocuments) },
                        enabled = selectedDocuments.isNotEmpty()) {
                        Icon(Icons.Default.Share, stringResource(R.string.library_share_selected))
                    }
                    IconButton(onClick = { if (selectedDocuments.isNotEmpty()) deletingDocument = selectedDocuments.first() },
                        enabled = selectedDocuments.isNotEmpty()) {
                        Icon(Icons.Default.Delete, stringResource(R.string.library_delete_selected))
                    }
                    IconButton(onClick = { selectionMode = false; selectedUris = emptySet() }) {
                        Icon(Icons.Default.Close, stringResource(R.string.library_cancel_selection))
                    }
                } else {
                    IconButton(onClick = onFindOldDocuments) {
                        Icon(Icons.Default.FolderOpen, stringResource(R.string.library_reconnect_folder))
                    }
                    IconButton(onClick = onAddDocuments) {
                        Icon(Icons.Default.Add, stringResource(R.string.library_add_documents))
                    }
                    IconButton(onClick = { selectionMode = true }) {
                        Icon(Icons.Default.CheckBox, stringResource(R.string.library_manage_documents))
                    }
                    AppOverflowMenu(navigation)
                }
            },
        ) },
        floatingActionButton = {
            FloatingActionButton(onClick = onScan) {
                Icon(Icons.Default.CameraAlt, stringResource(R.string.scan_new))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(stringResource(R.string.library_location),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            if (state.documents.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.library_search)) },
                        placeholder = { Text(stringResource(R.string.library_search_hint)) },
                    )
                    Box {
                        TextButton(onClick = { sortExpanded = true }) {
                            Text(stringResource(when (sort) {
                                LibrarySort.NAME -> R.string.library_sort_name
                                LibrarySort.DATE -> R.string.library_sort_date
                                LibrarySort.SIZE -> R.string.library_sort_size
                            }))
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.library_sort_name)) },
                                onClick = { sort = LibrarySort.NAME; sortExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.library_sort_date)) },
                                onClick = { sort = LibrarySort.DATE; sortExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.library_sort_size)) },
                                onClick = { sort = LibrarySort.SIZE; sortExpanded = false })
                        }
                    }
                }
            }
            if (hasDraft) {
                Card(onClick = onResumeDraft, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.library_draft), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.library_resume), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.library_load_error))
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.library_retry)) }
                    }
                }
                state.documents.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.library_empty_hint), style = MaterialTheme.typography.bodyMedium)
                        if (!state.hasFolderAccess) {
                            TextButton(onClick = onFindOldDocuments) {
                                Text(stringResource(R.string.library_reconnect_folder))
                            }
                        }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (visibleDocuments.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.library_search_no_results),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(visibleDocuments, key = { it.uri.toString() }) { document ->
                        var menuExpanded by remember(document.uri) { mutableStateOf(false) }
                        val isSelected = document.uri.toString() in selectedUris
                        Card(onClick = {
                            if (selectionMode) {
                                selectedUris = if (isSelected) selectedUris - document.uri.toString()
                                    else selectedUris + document.uri.toString()
                            } else onOpen(document)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f)) {
                                    Text(document.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("${DateFormat.getDateFormat(context).format(Date(document.modifiedAt))} · " +
                                        Formatter.formatShortFileSize(context, document.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selectionMode) {
                                    Checkbox(checked = isSelected, onCheckedChange = { checked ->
                                        selectedUris = if (checked) selectedUris + document.uri.toString()
                                            else selectedUris - document.uri.toString()
                                    })
                                } else Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, stringResource(R.string.library_document_menu, document.name))
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Share, null) },
                                            text = { Text(stringResource(R.string.share)) },
                                            onClick = { menuExpanded = false; onShare(document) },
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                                            text = { Text(stringResource(R.string.library_rename_document)) },
                                            onClick = {
                                                menuExpanded = false
                                                editedName = document.name
                                                renamingDocument = document
                                            },
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                                            text = { Text(stringResource(R.string.library_delete_selected)) },
                                            onClick = { menuExpanded = false; deletingDocument = document },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deletingDocument?.let { document ->
        val documentsToDelete = if (selectionMode) selectedDocuments else listOf(document)
        AlertDialog(
            onDismissRequest = { deletingDocument = null },
            title = { Text(stringResource(R.string.library_delete_title)) },
            text = { Text(stringResource(R.string.library_delete_message, documentsToDelete.size)) },
            confirmButton = {
                TextButton(onClick = {
                    deletingDocument = null
                    selectionMode = false
                    selectedUris = emptySet()
                    onDelete(documentsToDelete)
                }) { Text(stringResource(R.string.library_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingDocument = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    renamingDocument?.let { document ->
        AlertDialog(
            onDismissRequest = { renamingDocument = null },
            title = { Text(stringResource(R.string.library_rename_document)) },
            text = {
                TextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.library_rename_name)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renamingDocument = null
                    onRename(document, editedName)
                }) { Text(stringResource(R.string.library_rename_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { renamingDocument = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
