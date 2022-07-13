package com.jumadi.mqttsimulator.mqtt

import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import kotlin.reflect.KClass


abstract class Topic<T : Any>(private val typeData: KClass<T>, var topic: String = "") {

    companion object {
        const val TAG = "Topic"
    }

    open fun onMessage(string: String) {
        try {
            val json = JSONObject(string)
            val job = json.getString("job")
            val payload = if (json.isNull("payload")) "{}" else json.getJSONObject("payload").toString()
            Log.e(TAG, "job $job")
            Log.e(TAG, "payload $payload")

            if (typeData.java == Payload::class.java)
                getMessage(job, Payload(payload) as T)
            else
                getMessage(job, Gson().fromJson(payload, typeData.java))

        } catch (e: Exception) {
            Log.e(TAG, "Exception: " + e.message)
        }
    }

    abstract fun subscribeListener(isSubscribe: Boolean)

    fun setTopic(topic: String) : Topic<T> {
        this.topic = topic
        return this
    }

    abstract fun getMessage(job: String, payload: T)
}

@Parcelize
data class Payload(val data: String) : Parcelable