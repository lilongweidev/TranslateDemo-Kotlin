package com.llw.translate

data class TranslateResult(
    val from: String,
    val to: String,
    val trans_result: List<TransResult>
)