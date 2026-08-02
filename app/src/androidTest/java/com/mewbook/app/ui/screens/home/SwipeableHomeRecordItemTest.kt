package com.mewbook.app.ui.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.ui.theme.MewBookTheme
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Before
import org.junit.Test

class SwipeableHomeRecordItemTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun wakeAndShowTestActivity() {
        rule.activity.setShowWhenLocked(true)
        rule.activity.setTurnScreenOn(true)
    }

    private val record = Record(
        id = 1L,
        amount = 28.0,
        type = RecordType.EXPENSE,
        categoryId = 2L,
        note = "午餐",
        date = LocalDate.of(2026, 8, 2),
        createdAt = LocalDateTime.of(2026, 8, 2, 12, 0),
        updatedAt = LocalDateTime.of(2026, 8, 2, 12, 0),
        syncId = "record-1"
    )

    @Test
    fun closedRow_hidesActionsFromAccessibility() {
        setContent()

        rule.onAllNodesWithText("编辑").assertCountEquals(0)
        rule.onAllNodesWithText("删除").assertCountEquals(0)
    }

    @Test
    fun swipeLeft_revealsLabeledActions_withoutInvokingEitherAction() {
        var editCalls = 0
        var deleteCalls = 0
        setContent(onEdit = { editCalls++ }, onDelete = { deleteCalls++ })

        rule.onNodeWithTag("home-record-card-1").performTouchInput { swipeLeft() }

        rule.onNodeWithText("编辑").assertIsDisplayed()
        rule.onNodeWithText("删除").assertIsDisplayed()
        assertEquals(0, editCalls)
        assertEquals(0, deleteCalls)
    }

    @Test
    fun delete_requiresButtonTap_afterSwipe() {
        var deleteCalls = 0
        setContent(onDelete = { deleteCalls++ })

        rule.onNodeWithTag("home-record-card-1").performTouchInput { swipeLeft() }
        assertEquals(0, deleteCalls)
        rule.onNodeWithText("删除").performClick()

        assertEquals(1, deleteCalls)
    }

    @Test
    fun editButton_routesTheRecord() {
        var editedRecord: Record? = null
        setContent(onEdit = { editedRecord = it })

        rule.onNodeWithTag("home-record-card-1").performTouchInput { swipeLeft() }
        rule.onNodeWithText("编辑").performClick()

        assertEquals(1L, editedRecord?.id)
    }

    private fun setContent(
        onEdit: (Record) -> Unit = {},
        onDelete: (Long) -> Unit = {}
    ) {
        rule.setContent {
            var isOpen by remember { mutableStateOf(false) }
            MewBookTheme(darkTheme = false) {
                SwipeableHomeRecordItem(
                    record = record,
                    categoryName = "餐饮",
                    categoryIcon = "restaurant",
                    categoryColor = 0xFFF3CFA8,
                    isOpen = isOpen,
                    onRequestOpen = { isOpen = true },
                    onRequestClose = { isOpen = false },
                    onClick = {},
                    onEdit = onEdit,
                    onDelete = onDelete,
                    hapticFeedbackEnabled = false
                )
            }
        }
    }
}
