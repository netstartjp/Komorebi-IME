package me.zssu.ime.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class InputFieldPolicyTest {
    @Test
    fun normalTextUsesUserDefault() {
        assertEquals(
            InputFieldPolicy.DEFAULT,
            policy(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL),
        )
    }

    @Test
    fun passwordUsesAsciiWithoutCandidatesOrLearning() {
        assertEquals(
            InputFieldPolicy(
                InputFieldPolicy.Plane.ASCII,
                showCandidates = false,
                incognito = true,
            ),
            policy(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD),
        )
    }

    @Test
    fun numericPasswordUsesNumericWithoutCandidatesOrLearning() {
        assertEquals(
            InputFieldPolicy(
                InputFieldPolicy.Plane.NUMERIC,
                showCandidates = false,
                incognito = true,
            ),
            policy(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD),
        )
    }

    @Test
    fun urlAndEmailUseAsciiWithToolbarAndCandidates() {
        listOf(
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        ).forEach { variation ->
            assertEquals(
                InputFieldPolicy(
                    InputFieldPolicy.Plane.ASCII,
                    showCandidates = true,
                    incognito = false,
                ),
                policy(InputType.TYPE_CLASS_TEXT or variation),
            )
        }
    }

    @Test
    fun numberPhoneAndDateTimeUseNumericPlane() {
        listOf(
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME,
        ).forEach { inputType ->
            assertEquals(
                InputFieldPolicy(
                    InputFieldPolicy.Plane.NUMERIC,
                    showCandidates = false,
                    incognito = false,
                ),
                policy(inputType),
            )
        }
    }

    private fun policy(inputType: Int): InputFieldPolicy =
        InputFieldPolicy.from(EditorInfo().apply { this.inputType = inputType })
}
