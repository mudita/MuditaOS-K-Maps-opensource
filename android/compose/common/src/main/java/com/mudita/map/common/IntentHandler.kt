package com.mudita.map.common

interface IntentHandler<in Intent> {
    fun obtainIntent(intent: Intent)
}