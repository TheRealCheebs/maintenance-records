package com.github.therealcheebs.maintenancerecords.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.Type

// Moshi instance for JSON serialization/deserialization
val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

// Pre-defined adapters for common types
val stringMapAdapter: JsonAdapter<Map<String, String>> = moshi.adapter(
    Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
)

val stringAnyMapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(
    Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
)

val stringListAdapter: JsonAdapter<List<String>> = moshi.adapter(
    Types.newParameterizedType(List::class.java, String::class.java)
)

val anyListAdapter: JsonAdapter<List<Any?>> = moshi.adapter(
    Types.newParameterizedType(List::class.java, Any::class.java)
)

// Extension function to convert JSON string to Map<String, String>
fun String.toMap(): Map<String, String> {
    return try {
        stringMapAdapter.fromJson(this) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
}

// Extension function to convert JSON string to Map<String, Any?>
fun String.toAnyMap(): Map<String, Any?> {
    return try {
        stringAnyMapAdapter.fromJson(this) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
}

// Extension function to convert JSON string to List<String>
fun String.toStringList(): List<String> {
    return try {
        stringListAdapter.fromJson(this) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

// Extension function to convert JSON string to List<Any?>
fun String.toAnyList(): List<Any?> {
    return try {
        anyListAdapter.fromJson(this) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

// Helper function to create a generic JsonAdapter
inline fun <reified T> moshiAdapter(): JsonAdapter<T> {
    return moshi.adapter(T::class.java)
}

// Helper function to create a generic List JsonAdapter
inline fun <reified T> moshiListAdapter(): JsonAdapter<List<T>> {
    val listType = Types.newParameterizedType(List::class.java, T::class.java)
    return moshi.adapter(listType)
}

// Helper function to create a generic Map JsonAdapter
inline fun <reified K, reified V> moshiMapAdapter(): JsonAdapter<Map<K, V>> {
    val mapType = Types.newParameterizedType(Map::class.java, K::class.java, V::class.java)
    return moshi.adapter(mapType)
}

// Extension function to convert any object to JSON string
fun Any?.toJson(): String {
    return when (this) {
        null -> "null"
        is String -> if (this.startsWith("{") || this.startsWith("[")) this else "\"$this\""
        is Number, Boolean -> this.toString()
        is Map<*, *> -> {
            val stringKeyMap = this.entries
                .filter { it.key is String }
                .associate { it.key as String to it.value }
            val mapAdapter = moshiMapAdapter<String, Any?>()
            mapAdapter.toJson(stringKeyMap)
        }
        is List<*> -> {
            val listAdapter = moshiListAdapter<Any?>()
            listAdapter.toJson(this as List<Any?>)
        }
        else -> "\"${this.toString()}\""
    }
}

// Extension function to parse JSON string to any type
inline fun <reified T> String.fromJson(): T? {
    return try {
        moshi.adapter(T::class.java).fromJson(this)
    } catch (e: Exception) {
        null
    }
}

// Extension function to parse JSON string to list of any type
inline fun <reified T> String.fromJsonList(): List<T> {
    return try {
        val listType = Types.newParameterizedType(List::class.java, T::class.java)
        moshi.adapter<List<T>>(listType).fromJson(this) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

// Extension function to parse JSON string to map of any type
inline fun <reified K, reified V> String.fromJsonMap(): Map<K, V> {
    return try {
        val mapType = Types.newParameterizedType(Map::class.java, K::class.java, V::class.java)
        moshi.adapter<Map<K, V>>(mapType).fromJson(this) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
}

// Safe conversion extensions
fun String.toLongOrNull(): Long? {
    return try {
        this.toLong()
    } catch (e: NumberFormatException) {
        null
    }
}

fun String.toDoubleOrNull(): Double? {
    return try {
        this.toDouble()
    } catch (e: NumberFormatException) {
        null
    }
}

fun String.toIntOrNull(): Int? {
    return try {
        this.toInt()
    } catch (e: NumberFormatException) {
        null
    }
}

inline fun <reified V> Map<String, V>.toJson(): String {
    val adapter = moshi.adapter<Map<String, V>>(
        Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
    )
    return adapter.toJson(this)
}

inline fun <reified T> List<T>.toJson(): String {
    val adapter = moshi.adapter<List<T>>(
        Types.newParameterizedType(List::class.java, T::class.java)
    )
    return adapter.toJson(this)
}
