package com.iceteaviet.fastfoodfinder.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsTest {
    @Test
    fun isEmpty_true() {
        assertEquals(true, isEmpty(""))
    }

    @Test
    fun isEmpty_nullString() {
        assertEquals(true, isEmpty(null))
    }

    @Test
    fun isEmpty_false() {
        assertEquals(false, isEmpty("abc"))
    }
}