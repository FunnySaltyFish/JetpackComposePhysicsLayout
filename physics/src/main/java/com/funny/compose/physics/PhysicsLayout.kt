package com.funny.compose.physics

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jbox2d.common.Vec2

private const val TAG = "PhysicsLayout"

interface PhysicsLayoutScope {
    @Stable
    fun Modifier.physics(physicsConfig: PhysicsConfig, initialX : Float = 0f, initialY : Float = 0f) : Modifier
}

internal object PhysicsLayoutScopeInstance : PhysicsLayoutScope {
    @Stable
    override fun Modifier.physics(
        physicsConfig: PhysicsConfig,
        initialX: Float,
        initialY: Float
    ): Modifier = this.then(PhysicsParentData(physicsConfig, initialX, initialY))
}


@Composable
fun PhysicsLayout(
    modifier: Modifier = Modifier,
    physicsLayoutState: PhysicsLayoutState = remember { PhysicsLayoutState() },
    boundColor: Color? = Color(121, 85, 72),
    boundSize : Float? = 20f,
    giveInitialRandomImpulse : Boolean = true,
    content : @Composable PhysicsLayoutScope.()->Unit
){
    val parentDataList = physicsLayoutState.physics.composableGroup.physicsParentDatas
    val physics = physicsLayoutState.physics
    val density = LocalDensity.current
    var initialized by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = density){
        physics.density = density.density
        physics.pixelsPerMeter = with(density){
            10.dp.toPx()
        }
    }

    var recompose by remember {
        mutableStateOf(0)
    }

    // 初始化世界
    LaunchedEffect(initialized){
        Log.d(TAG, "PhysicsLayout: launchedEffect ${parentDataList.size} ${physics.width}")
        if (!initialized) return@LaunchedEffect
        if (parentDataList.isEmpty()) return@LaunchedEffect
        if (physics.width * physics.height == 0) return@LaunchedEffect
        physics.createWorld { body, i ->
            parentDataList[i].body = body
//            Log.d(TAG, "PhysicsLayout: createBody: $body")
        }
        if(giveInitialRandomImpulse) physics.giveRandomImpulse()
    }

    LaunchedEffect(key1 = boundSize){
        if (boundSize != null && boundSize > 0){
            physics.setBoundsSize(boundSize)
        }
    }

    LaunchedEffect(key1 = Unit){
        while (true){
            delay(16)
            physics.step() // 模拟 16ms
            recompose++
        }
    }

    val drawBoundModifier =
        Modifier.drawWithContent {
            if (physics.hasBounds && boundColor != null && boundSize != null && boundSize > 0){
                if(physics.density <= 0) return@drawWithContent
                // 绘制 bound
                val s = boundSize * physics.density
                val w = physics.width.toFloat()
                val h = physics.height.toFloat()
                drawRect(boundColor, Offset.Zero, Size(w,s))
                drawRect(boundColor, Offset(0f, h-s), Size(w,s))
                drawRect(boundColor, Offset(0f, s), Size(s, h - 2 * s))
                drawRect(boundColor, Offset(w - s, s), Size(s, h - 2 * s))
            }
            drawContent()
        }

    Layout(content = { PhysicsLayoutScopeInstance.content() }, modifier = modifier.then(drawBoundModifier)){ measurables, constraints ->
        if (!initialized) {
            physics.setSize(constraints.maxWidth, constraints.maxHeight)
        }

        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.mapIndexed { index,  measurable ->
            val physicsParentData = (measurable.parentData as? PhysicsParentData) ?: PhysicsParentData()
//            Log.d(TAG, "PhysicsLayout: init : $initialized")
            if (!initialized){
                parentDataList.add(index, physicsParentData)
//                Log.d(TAG, "PhysicsLayout: addParentData: (${physicsParentData.initialX}, ${physicsParentData.initialY})")
            }
            measurable.measure(childConstraints)
        }

        layout(constraints.maxWidth, constraints.maxHeight){
            placeables.forEachIndexed { i, placeable: Placeable ->
                // 正确设置各body大小
                parentDataList[i].width = placeable.width
                parentDataList[i].height = placeable.height

                val x = physics.metersToPixels(parentDataList[i].x).toInt() - placeable.width / 2
                val y = physics.metersToPixels(parentDataList[i].y).toInt() - placeable.height / 2

                val c = recompose // 这行代码什么用也没有，目的是触发重新 Layout

//                Log.d(TAG, "PhysicsLayout: $i -> x : $x y : $y")
//                Log.d(TAG, "PhysicsLayout: $recompose")
//                placeable.place(x, y)
                placeable.placeWithLayer(IntOffset(x,y), zIndex = 0f, layerBlock = {
                     rotationZ = parentDataList[i].rotation
                })
            }
        }.also {
            // 各类初始化只进行一次即可
            if (!initialized) {
                initialized = true
            }
        }
    }
}