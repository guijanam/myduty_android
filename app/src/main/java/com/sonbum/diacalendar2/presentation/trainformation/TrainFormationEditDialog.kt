package com.sonbum.diacalendar2.presentation.trainformation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sonbum.diacalendar2.domain.model.TrainFormation
import com.sonbum.diacalendar2.domain.model.TrainHalf

/**
 * 편성 추가/수정 다이얼로그.
 * DateDetailScreen 과 TrainFormationListScreen 양쪽에서 쓰이므로 internal.
 *
 * 수정 시에도 편성번호/메모만 바꾼다 — 스냅샷(shiftName, numTr)은 건드리지 않는다.
 */
@Composable
internal fun TrainFormationEditDialog(
    half: TrainHalf,
    initial: TrainFormation?,
    onConfirm: (Int, String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var formationText by remember { mutableStateOf(initial?.formationNo?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    val parsed = formationText.toIntOrNull()
    val isValid = parsed != null && parsed > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(if (initial == null) "${half.label} 편성 추가" else "${half.label} 편성 수정")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formationText,
                    onValueChange = { v -> formationText = v.filter { it.isDigit() }.take(5) },
                    label = { Text("편성번호") },
                    placeholder = { Text("예: 254") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("메모 (선택)") },
                    placeholder = { Text("예: 에어컨 고장") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it, note) } },
                enabled = isValid
            ) { Text("저장") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        }
    )
}
