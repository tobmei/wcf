package de.tob.wcf

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.tob.wcf.R
import de.tob.wcf.ui.main.ui.theme.WCFTheme

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WCFTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    InputRow(modifier = Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
fun InputElement(
    modifier: Modifier = Modifier,
    image: Int,
    id: Int,
    selected: Boolean,
    itemClicked: (Int) -> Unit
) {
    Surface(
        color = if(selected) MaterialTheme.colorScheme.secondary else Color.LightGray,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .scale(if(selected) 1.3F else 1F)
            //.shadow(if(selected) 10.dp else 0.dp)
            .selectable (
                selected = selected,
                onClick = { itemClicked(id) }
            )
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(id = image),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
fun InputRow(
    modifier: Modifier = Modifier
) {
    var selectedItem by remember {
        mutableStateOf(1)
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.selectableGroup()
    ) {
        items(myList, key = { it.second }) { item ->
            InputElement(
                image = item.first,
                id = item.second,
                selected = selectedItem==item.second
            ) {
                selectedItem = it
            }
        }
    }
}

private val myList = listOf(
    R.drawable.test15 to 1,
    R.drawable.test16 to 2,
    R.drawable.test14 to 3,
    R.drawable.test13 to 4,
    R.drawable.test12 to 5,
    R.drawable.test11 to 6,
    R.drawable.test10 to 7,
    R.drawable.test17 to 8,
    R.drawable.test18 to 9,
    R.drawable.test19 to 10
)

@Preview(showBackground = true, backgroundColor =  0xFFF0EAE2)
@Composable
fun DefaultPreview() {
    WCFTheme {
        InputRow(modifier = Modifier.padding(vertical = 16.dp))
    }
}