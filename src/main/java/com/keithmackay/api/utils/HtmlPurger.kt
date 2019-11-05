package com.keithmackay.api.utils

val pattern = Regex("(<script.*>.*<\\/script>)|(<script.*\\/>)",
    RegexOption.IGNORE_CASE)

fun purgeHtml(s: String): String = purgeHtml(s, null)

fun purgeHtml(s: String, servers: Regex?): String {
  var temp = s.replace(pattern, "")

  if (servers != null) {
    temp = temp.replace(servers, "https://example.com")
  }

  return temp
}
