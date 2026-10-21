package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class PasswordStrengthTest {

    @Test
    fun strengthGrowsWithLengthAlone() {
        assertEquals(PasswordStrength.Weak, passwordStrength("a"))
        assertEquals(PasswordStrength.Weak, passwordStrength("Tr0ub4d&!"))
        assertEquals(PasswordStrength.Fair, passwordStrength("0123456789"))
        assertEquals(PasswordStrength.Strong, passwordStrength("correct horse b"))
        assertEquals(PasswordStrength.VeryStrong, passwordStrength("correct horse battery staple"))
    }
}
