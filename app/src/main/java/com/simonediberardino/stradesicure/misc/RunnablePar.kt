package com.simonediberardino.stradesicure.misc

interface RunnablePar : Runnable {
    fun run(p: Any?)
    override fun run() {}
}