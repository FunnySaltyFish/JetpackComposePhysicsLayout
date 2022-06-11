## Jetpack Compose Custom Layout + Physical Engine = ?

This library is based on Jetpack Compose custom layout  +  physical engine（JBox2d）.

Especially thanks to [Jawnnypoo/PhysicsLayout: Android layout that simulates physics using JBox2D (github.com)](https://github.com/Jawnnypoo/PhysicsLayout).



### Demo



![PhysicsLayout Demo](screen_shot.gif)

(These three buttons are "give random impulse", "set random gravity" and "set random border size" )

The corresponding codes are roughly as follows:


```kotlin
val physicsConfig = PhysicsConfig()

PhysicsLayout(modifier = modifier, physicsLayoutState = physicsLayoutState, boundSize = boundSize.toFloat()) {
    RandomColorBox(modifier = Modifier
        .size(40.dp)
        .physics(physicsConfig, initialX = 300f, initialY = 500f))
    // This one has a circle shape
    // so you need to modify it with not only a `clip()` Modifier to make it "looks like" a circle
    // but also a `physics(physicsConfig.copy(shape = PhysicsShape.CIRCLE)` Modifier to create a circle Body
    RandomColorBox(modifier = Modifier
        .clip(CircleShape)
        .size(50.dp)
        .physics(physicsConfig.copy(shape = PhysicsShape.CIRCLE), 300f, 1000f))
    RandomColorBox(...)
    Checkbox(...)
    Card(...)
}
```

To experience additional features above, you can download the demo [here](demo.apk).



### Usage

The code of core library code is  located in module `physics`. 

The basically usage is as follows:


```kotlin
val physicsConfig = PhysicsConfig()
@Composable
fun PhysicsLayoutTest(modifier: Modifier) {
    // physicsLayoutState contains the class about physics, providing two methods
    // setGravity and giveRandomImpulse
    val physicsLayoutState by remember {
        mutableStateOf(PhysicsLayoutState())
    }
    var boundSize by remember {
        mutableStateOf(20)
    }
    PhysicsLayout(modifier = modifier, physicsLayoutState = physicsLayoutState, boundSize = boundSize.toFloat()) {
        // use `Modifier.physics` to specify the config of physics
        // initialX/Y is the initial position of the body
        RandomColorBox(modifier = Modifier
            .size(40.dp)
            .physics(physicsConfig, initialX = 300f, initialY = 500f))
        // This one has a circle shape
        // so you need to modify it with not only a `clip()` Modifier to make it "looks like" a circle
        // but also a `physics(physicsConfig.copy(shape = PhysicsShape.CIRCLE)` Modifier to create a circle Body
        RandomColorBox(modifier = Modifier
            .clip(CircleShape)
            .size(50.dp)
            .physics(physicsConfig.copy(shape = PhysicsShape.CIRCLE), 300f, 1000f))
    }
}
```



### Reference

#### JBox2d

> JBox2D is an open-source 2D physical engine, which can automatically simulate the physical motion of 2D rigid body according to the parameters set by developers, such as gravity, density, friction coefficient and elastic coefficient.



### How It Works?

#### definition

First, consider one thing: now each object actually has its own **location, size, shape** , so how does **parent layout get these values**?  If you've read some related blogs,  you can know it:  we may use `ParentData`.  So let's write a custom `ParentDataModifier` first.


```kotlin
class PhysicsParentData(
    var physicsConfig: PhysicsConfig = PhysicsConfig(),
    var initialX: Float = 0f,
    var initialY: Float = 0f,
    var width: Int = 0,
    var height: Int = 0
) : ParentDataModifier
```

`PhysicsConfig` stands for the basic physical configuration, and the rest is the **initial position and the  size**.



Well. let's write corresponding scope then.


```kotlin
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
```

The code above is basic operations to define a custom layout. If you don't understand, you can learn first before reading.



#### Use It

Now we've finished basic definition.  Let's start to implement the layout. Here are my process.

1. Initialize objects and the world

2. Use the code to simulate the movement of each object
3. Get the location and place it correctly during Layout

Let's do them one after another! *(Here are just my thoughts, and some of them may not be so elegant, if you have better ideas please point them out! )*

In general, there should be some code dedicated to the physical simulation process, this part of the code is class `Physics`, which is responsible for `creating the world`, `simulate`, and so on. We just pass it here.



##### initialization

Considering that the specific information of each sub-component is not readable until `Layout`  , so it seems that we can only initialize here; but cause the `Layout` process will be done repeatedly, and initialization should only be done once. So we use a variable to control it.


```kotlin
var initialized by remember {
    mutableStateOf(false)
}
```

They we store these`ParentData` at the first time.


```kotlin
val placeables = measurables.mapIndexed { index,  measurable ->
    val physicsParentData = (measurable.parentData as? PhysicsParentData) ?: PhysicsParentData()
    if (!initialized){
        parentDataList.add(index, physicsParentData)
    }
    measurable.measure(childConstraints)
}
```

And then, with a side effect, after all the object information is initialized, create the world and create **Body** (the class representing `rigid body` in JBox2d)


```kotlin
// 初始化世界
LaunchedEffect(initialized){
    if (!initialized) return@LaunchedEffect
    physics.createWorld { body, i ->
        parentDataList[i].body = body
    }
}
```

where method `createWorld` is responsible for creating `World` and  do callback after each creation.



##### continuous simulation

Simulating is the duty of `JBox2d` . All we have to do is call it repeatedly. 


```kotlin
LaunchedEffect(key1 = Unit){
    while (true){
        delay(16)
        physics.step() // 模拟 16ms
    }
}
```



##### Read and place correctly

This is not difficult，just reading each `Body`'s position and place it in `Layout`. 

A special thing is，because `Body` has its rotation angle，so we need to use method`placeWithLayer`, whose signature is as below:


```kotlin
fun Placeable.placeWithLayer(
    position: IntOffset,
    zIndex: Float = 0f,
    layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
)
```

where the third parameter `layerBlock` provides the way to `rotate`, `transform` and so on. The corresponding code is as below:


```kotlin
layout(constraints.maxWidth, constraints.maxHeight){
    placeables.forEachIndexed { i, placeable: Placeable ->
        val x = physics.metersToPixels(parentDataList[i].x).toInt() - placeable.width / 2
        val y = physics.metersToPixels(parentDataList[i].y).toInt() - placeable.height / 2
          
        placeable.placeWithLayer(IntOffset(x,y), zIndex = 0f, layerBlock = {
             rotationZ = parentDataList[i].rotation
        })
    }
}
```

The function `metersToPixels` used above is to map **physical world coordinates to reality coordinates**

Done!





### Follow-up

Actually, there's something not so perfect with the code right now, like, to trigger `Layout` , I actually used a state that wasn't useful... Because in my attempts, as long as `state` never appears to change, it won't be triggered (which is what Compose should behave); I can't think of any good ideas, so  if you have any good ideas, please discuss them with PRs.

If you're wondering what it can be used for......Well, I don't know, neither. I just thought it was fun. I've been thinking of doing it for a long time. and finally spent two days finishing it. It looks well, I think :)
