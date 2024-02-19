package com.example.exer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.getLogger as getLogger

class IndexingAction : AnAction() {
    private val NEXT_LINE_PATTERN = "\n"
    private val INDEX_NAME_PATTERN = "^\\s*private\\s+static\\s+final\\s+int\\s+[a-zA-Z_]+_INDEX\\s*=\\s*\\d+;\\s*$"
    private val INDEX_VALUE_PATTERN = "=\\s*\\d+;"

    override fun actionPerformed(actionEvent: AnActionEvent) {
        val project = actionEvent.project
        val requiredData = actionEvent.getRequiredData(CommonDataKeys.EDITOR)
        val caret = requiredData.caretModel
        val currentCaret = caret.currentCaret
        val selectedText: @NlsSafe String? = currentCaret.selectedText
        val document = requiredData.document
        val start = currentCaret.selectionStart
        val end = currentCaret.selectionEnd

        assert(selectedText != null)

        val lines: Array<String>? = selectedText?.split(NEXT_LINE_PATTERN.toRegex())
            ?.dropLastWhile { it.isEmpty() }
            ?.toTypedArray()

        val newLines: MutableList<String> = ArrayList()
        var i = 0
        for (line in lines!!) {
            if (line.matches(INDEX_NAME_PATTERN.toRegex())) {
                newLines.add(line.replace(INDEX_VALUE_PATTERN.toRegex(), "= $i;"))
                i++
            }
        }

        if (newLines.isEmpty()) {
            getLogger<IndexingAction>().log(LogLevel.Error, "failed to index!", Exception("failed to index!"))
        } else {
            val replaceString = newLines.joinToString(NEXT_LINE_PATTERN)

            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(start, end, replaceString)
            }

            currentCaret.removeSelection()
        }
    }

    override fun update(actionEvent: AnActionEvent) {
        val editor = actionEvent.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        actionEvent.presentation.setEnabledAndVisible(caretModel.currentCaret.hasSelection())
    }
}
