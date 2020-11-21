package com.keithmackay.api.utils

import com.google.common.collect.Lists
import com.mongodb.client.model.UpdateOptions
import io.javalin.http.Context
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

const val byteUnitIncrement: Long = 1000

fun getLogger(name: String): Logger = LogManager.getLogger(name)
fun <T : Any> getLogger(c: Class<T>): Logger = getLogger("keith.${c.simpleName}")
fun <T : Any> getLogger(c: KClass<T>): Logger = getLogger("keith.${c.simpleName}")

object LogLevels {
  val NETWORK: Level = Level.forName("NETWORK", 450)
  val BENCHMARK: Level = Level.forName("BENCH", 350)
  val TIMER: Level = Level.forName("TIMER", 450)
}

fun cloneDoc(doc: Document?): Document? {
  return if (doc == null) null else Document.parse(doc.toJson())
}

fun millisToReadableTime(timeDiff: Long): String {
  val diffSeconds = timeDiff / 1000 % 60
  val diffMinutes = timeDiff / (60 * 1000) % 60
  val diffHours = timeDiff / (60 * 60 * 1000) % 24
  val diffDays = timeDiff / (24 * 60 * 60 * 1000)
  val builder = StringBuilder()
  if (diffDays > 0) {
    builder.append(diffDays).append(" Day${conditionalPlural(diffDays)} ")
  }
  if (diffHours > 0) {
    builder.append(diffHours).append(" Hour${conditionalPlural(diffHours)} ")
  }
  if (diffMinutes > 0) {
    builder.append(diffMinutes).append(" Minute${conditionalPlural(diffMinutes)} ")
  }
  if (diffSeconds > 0) {
    builder.append(diffSeconds).append(" Second${conditionalPlural(diffSeconds)} ")
  }
  return builder.append(timeDiff % 1000).append("ms").toString()
}

fun printTimeDiff(start: Long): String = millisToReadableTime(System.currentTimeMillis() - start)

fun conditionalPlural(n: Number) = if (n.toDouble() > 1) "s" else ""

fun cleanDoc(doc: Document?): Document? {
  val d = cloneDoc(doc) ?: return null
  val id = d.remove("_id")
  d["id"] = id.toString()
  return d
}

fun Document.cleanTo(vararg values: String): Document {
  this.keys.forEach {
    if (!values.contains(it)) {
      this.remove(it)
    }
  }
  return this
}

fun Document.add(key: String, value: () -> Any?): Document = this.append(key, value())
fun Document.join(doc: Document): Document = this.join(doc, true)
fun Document.drop(key: String): Document {
  this.remove(key)
  return this
}

fun Document.join(doc: Document, overwrite: Boolean): Document {
  doc.keys.forEach {
    if (!this.containsKey(it) || overwrite) {
      this[it] = doc[it]
    }
  }
  return this
}

fun Double.print(decimals: Int): String = "%.${decimals}f".format(this)

fun noop() {
  // No-op
}

fun inputStream(s: String): InputStream = s.byteInputStream(Charset.defaultCharset())

fun big(n: Double): BigDecimal = BigDecimal.valueOf(n)

fun humanizeBytes(bytes: Long): String = humanizeBytes(BigInteger.valueOf(bytes))
fun humanizeBytes(bytes: BigInteger): String {
  var b = bytes.toBigDecimal() // Clone the object
  var unit = 0
  while (b.greaterThanEqual(byteUnitIncrement.toDouble()) && unit < 7) {
    b = b.divide(big(byteUnitIncrement.toDouble()))
    unit++
  }
  return String.format("%.2f %s", b.toDouble(), arrayOf("bytes", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")[unit])
}

fun humanizeBytes(bytes: Int): String = humanizeBytes(BigInteger.valueOf(bytes.toLong()))

private fun BigDecimal.greaterThanEqual(i: Double): Boolean {
  val comparison = this.compareTo(big(i))
  return comparison == 0 || comparison == 1
}

fun Document.subDoc(name: String): Document {
  return this[name] as Document
}

fun httpLog(ctx: Context, time: Float) {
  val logger = LogManager.getLogger("Server")
  val line = "${ctx.protocol()}:${ctx.method()}:${ctx.status()} ${humanizeBytes(ctx.bodyAsBytes().size)} " +
      "on '${ctx.path()}' from ${ctx.ip()} took ${time}ms"

  val level: Level = if (time >= 10000) {
    Level.WARN
  } else if (time <= 10 && ctx.status() == 200) {
    LogLevels.NETWORK
  } else {
    Level.INFO
  }
  logger.log(level, line)
}

fun JSONArray.iterateObjects(): List<JSONObject> = this.iterateObjects(Int.MAX_VALUE)

fun JSONArray.iterateObjects(max: Int): List<JSONObject> {
  val l = mutableListOf<JSONObject>()
  for (i in 0 until this.length().coerceAtMost(max)) {
    l.add(this.getJSONObject(i))
  }
  return l
}

fun <T : Any> threadSafeList(content: Collection<T>): MutableList<T> =
    Collections.synchronizedList(Lists.newArrayList(content))

fun <T : Any> threadSafeList(vararg content: T): MutableList<T> =
    threadSafeList(listOf(*content))

fun <T : Any, S : Any> threadSafeMap(): MutableMap<T, S> = Collections.synchronizedMap(HashMap())

fun urlEncode(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8.toString())

fun fileToString(filename: String): String =
    Files.readString(Paths.get(filename))

fun upsert(): UpdateOptions = UpdateOptions().upsert(true)

/** Bson Shorthands */
fun doc(name: String, value: Any?): Document = Document(name, value)

fun doc(): Document = Document()
fun json(name: String, value: Any?): String = doc(name, value).toJson()

fun eq(content: Any?): Document = doc("\$eq", content)
fun ne(content: Any?): Document = doc("\$ne", content)
fun and(content: Collection<Any>): Document = doc("\$and", content)
fun and(vararg content: Document?): Document = and(arr(*content))
fun or(vararg content: Document?): Document = or(arr(*content))
fun or(content: Collection<Any>): Document = doc("\$or", content)
fun lessThan(content: Any): Document = doc("\$lt", content)
fun matchPattern(content: String): Document = doc("\$match", content)
fun arr(vararg docs: Document?): MutableList<Document> =
    docs.filter(Objects::nonNull).mapNotNull { it }.toMutableList()

fun set(value: Any?): Document = Document("\$set", value)

/** Greater Than Equal */
fun gte(value: Any?): Document = Document("\$gte", value)

fun gt(value: Any?): Document = Document("\$gt", value)

/** Less Than Equal */
fun lte(value: Any?): Document = Document("\$lte", value)

fun async(task: Runnable): Unit = Thread(task).start()
fun async(task: () -> Unit): Unit = Thread(task).start()

fun defer(task: Runnable, time: Long) = defer({ task.run() }, time)
fun defer(task: () -> Unit, time: Long) = async {
  try {
    Thread.sleep(time)
  } catch (e: Exception) {
    // Error Waiting
  }
  task()
}
