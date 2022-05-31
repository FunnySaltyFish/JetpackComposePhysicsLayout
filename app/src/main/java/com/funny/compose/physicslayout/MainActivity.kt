package com.funny.compose.physicslayout

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funny.cmaterialcolors.MaterialColors
import com.funny.compose.physics.PhysicsConfig
import com.funny.compose.physics.PhysicsLayout
import com.funny.compose.physics.PhysicsLayoutState
import com.funny.compose.physics.PhysicsShape
import com.funny.compose.physicslayout.ui.RandomColorBox
import com.funny.compose.physicslayout.ui.theme.JetpackComposePhysicsLayoutTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackComposePhysicsLayoutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(8.dp)) {
                        val physicsLayoutState by remember {
                            mutableStateOf(PhysicsLayoutState())
                        }
                        var boundSize by remember {
                            mutableStateOf(20)
                        }
                        PhysicsLayoutTest(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.8f),
                            physicsLayoutState,
                            boundSize
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Button(onClick = { physicsLayoutState.giveRandomImpulse() }) {
                                Text(text = "随机给个冲量")
                            }
                            Button(onClick = {
                                physicsLayoutState.setGravity(
                                    Random.nextDouble(-5.0,5.0).toFloat(),
                                    Random.nextDouble(-9.8,9.8).toFloat()
                                )
                            }) {
                                Text(text = "随机重力")
                            }
                            Button(onClick = {
                                boundSize = Random.nextInt(5,20)
                            }) {
                                Text(text = "随机边框大小")
                            }
                        }

                    }
                }
            }
        }
    }
}

val physicsConfig = PhysicsConfig()
@Composable
fun PhysicsLayoutTest(modifier: Modifier, physicsLayoutState: PhysicsLayoutState, boundSize : Int) {
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
        RandomColorBox(modifier = Modifier
            .size(60.dp)
            .physics(physicsConfig))
        var checked by remember {
            mutableStateOf(false)
        }
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Card(modifier = Modifier
            .clip(CircleShape)
            .physics(physicsConfig.copy(shape = PhysicsShape.CIRCLE), initialX = 200f)) {
            Image(painter = painterResource(id = R.drawable.bg_avatar), contentDescription = "avatar", modifier = Modifier.size(100.dp))
        }
        val context = LocalContext.current
        LazyColumn(modifier = Modifier
            .width(200.dp)
            .height(100.dp)
            .background(MaterialColors.Orange200)
            .physics(physicsConfig, initialY = 300f)){
            item {
                ClickableText(text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.Blue,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Github/JetpackComposePhysicsLayout")
                    }
                }){
                    Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse("https://github.com/FunnySaltyFish/JetpackComposePhysicsLayout")
                    }.also {
                        context.startActivity(it)
                    }
                }
            }
            items(10){
                Text(text = "FunnySaltyFish", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.W500, fontSize = 18.sp)
            }
        }
    }
}