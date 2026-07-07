package io.github.adulescentia.LUMOS_lib

import java.util.function.IntFunction

class SamplingQueue<T>(val sampleCount : Int, init : (Int) -> T){
    private val arr = MutableList(sampleCount,init)
    fun add(element : T){
        if (arr.size >= sampleCount) arr.removeAt(0)
        arr.add(element)
    }
    fun take(count : Int): List<T>{
        return arr.takeLast(count)
    }
    fun toList(): List<T> = arr.toList()
    fun checkDataIntegrity(filter : (T) -> Boolean) : T?{
        val filtered = arr.filter(filter)
        val sample = filtered.firstOrNull() ?: return null
        if(filtered.all { it == sample }) return sample
        return null
    }
}