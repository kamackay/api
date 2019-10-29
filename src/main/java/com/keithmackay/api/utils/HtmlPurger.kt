package com.keithmackay.api.utils

val pattern = Regex("(<script.*>.*<\\/script>)|(<script.*\\/>)",
    RegexOption.IGNORE_CASE)

fun purgeHtml(s: String) =
  s.replace(pattern, "")
