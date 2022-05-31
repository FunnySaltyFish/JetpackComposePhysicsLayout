package com.funny.compose.physics

import org.jbox2d.common.Vec2

data class PhysicsLayoutState(val physics: Physics = Physics(ComposableGroup(arrayListOf()))){
    /**
     * 设置重力
     * @param x Float x方向，默认0
     * @param y Float y方向，默认9.8
     */
    fun setGravity(x:Float = 0f, y:Float = 9.8f){
        physics.world?.gravity = Vec2(x,y)
    }

    /**
     * 给所有物体一个随机冲量
     */
    fun giveRandomImpulse(){
        physics.giveRandomImpulse()
    }
}