package com.example.offlinecounter

import java.util.Locale

data class Person(
    val fio: String,
    val birthDate: String,
    val sex: String,
    val status: String,
) {
    val key: String = "${fio.trim().lowercase(Locale.ROOT)}|$birthDate"
}
