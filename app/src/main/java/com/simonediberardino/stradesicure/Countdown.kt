package com.simonediberardino.stradesicure

import java.lang.Exception
import kotlin.properties.Delegates

class Countdown{
    var curSeconds: Int = 0
    var countdown: Int = -1

    var counter: Thread
    var onSecondCallback: Runnable? = null
    var onCompleteCallback: Runnable? = null

    constructor(countdown: Int, onCompleteCallback: Runnable, onSecondCallback: Runnable?){
        this.countdown = countdown
        this.onCompleteCallback = onCompleteCallback
        this.onSecondCallback = onSecondCallback!!
    }

    constructor(countdown: Int, onCompleteCallback: Runnable){
        this.countdown = countdown
        this.onCompleteCallback = onCompleteCallback
        this.onSecondCallback = null
    }

    fun start(){
        counter.start()
    }

    fun resetTimer(){
        curSeconds = 0
    }

    fun destroy(){
        counter.interrupt()
    }

    private fun getSecondsString(): String {
        val secondsStr: String = (curSeconds % 60).toString()

        return if (secondsStr.length == 1)
            "0${secondsStr}"
        else secondsStr
    }

    private fun getMinutesString(): String {
        var elapsedMinutes: String = (curSeconds / 60).toString()
        if (elapsedMinutes.length == 1) elapsedMinutes = "0$elapsedMinutes"
        return elapsedMinutes
    }

    fun getElapsedTimeString(): String {
        return "${getMinutesString()}: ${getSecondsString()}"
    }

    init{
        counter = Thread {
            curSeconds = countdown

            while(curSeconds > 0){
                try{
                    this.onSecondCallback?.run()
                    Thread.sleep(1000)
                    curSeconds--
                }catch (e: Exception){
                    return@Thread
                }
            }
            this.onCompleteCallback?.run()
        }
    }
}
