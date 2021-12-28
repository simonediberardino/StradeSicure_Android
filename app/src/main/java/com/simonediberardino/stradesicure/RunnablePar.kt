package com.simonediberardino.stradesicure

interface RunnablePar : Runnable {
    fun run(any: Any) {}
    override fun run() {}
}