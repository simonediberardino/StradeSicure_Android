package com.simonediberardino.stradesicure.misc

interface RunnablePar : Runnable {
    fun run(any: Any) {}
    override fun run() {}
}