package com.simonediberardino.stradesicure.entity

import android.content.Context
import android.speech.tts.TextToSpeech

class TTS(val context: Context, val listener: OnInitListener) : TextToSpeech(context, listener) {
    fun speak(text: String){
        super.speak(text, QUEUE_ADD, null, null)
    }
}