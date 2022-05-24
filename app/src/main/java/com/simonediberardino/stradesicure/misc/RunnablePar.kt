package com.simonediberardino.stradesicure.misc

abstract class RunnablePar {
    open fun run(p: Any?) {}
    open fun run(vararg p: Any?) {}
}