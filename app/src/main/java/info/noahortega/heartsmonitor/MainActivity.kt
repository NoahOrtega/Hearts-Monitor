package info.noahortega.heartsmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.viewmodel.compose.viewModel
import info.noahortega.heartsmonitor.ui.theme.HeartsMonitorTheme

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         HeartsMonitorTheme {
            Surface(Modifier.fillMaxSize()) {
//               val vm: HeartsViewModel = viewModel()
//               ContactsScreen(contacts = vm.dummyContacts(20))
//               SuggTest()
               TestContactEdit()
            }
         }
      }
   }
}

@Preview()
@Composable
fun TestContactEdit() {
   EditContactScreen(contact = null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(onRandom: () -> Unit = {}, contact: Contact?) {
   val imageId = R.drawable.smiles001 //TODO: from VM

   Column(modifier = Modifier
         .padding(40.dp)
         .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
   ) {
      Column(Modifier.width(intrinsicSize = IntrinsicSize.Max)) {
         Row(verticalAlignment = Alignment.CenterVertically) {
            ContactImage(modifier = Modifier.width(96.dp), imgId = imageId, name = "Edit Screen")
            Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = { onRandom() }) {
               Text(text = "Shuffle Profile Pic")
            }
         }
         Spacer(modifier = Modifier.size(16.dp))
         OutlinedTextField( //TODO: text limit
            value = contact?.name ?: "",
            onValueChange = { contact?.name = it },
            label = { Text("Contact Name") }
         )
         Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 20.dp))
         Text(text = "Nudge Settings:")
         Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = false, onCheckedChange = {}) //Todo: make work
            Text(text = "Nudge me to message them")
         }
         OutlinedTextField( //TODO: only allow Ints
            value = contact?.nudgeDayInterval?.toString() ?: "",
            onValueChange = { contact?.nudgeDayInterval =  it.toInt()},
            label = { Text("How Often (In Days)") },
            enabled = true
         )
         Row(horizontalArrangement = Arrangement.SpaceEvenly,
         modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
            val buttonModifier = Modifier.width(102.dp)
            Button(modifier = buttonModifier,
               onClick = {  }) {
               Text(text = "Save  ", )
            }
            OutlinedButton(modifier = buttonModifier,
               onClick = {  }) {
               Text(text = "Cancel")
            }
         }

      }
   }
}

//@Preview
@Composable
fun TestContactsScreen() {
   val vm: HeartsViewModel = viewModel()
   ContactsScreen(contacts = vm.dummyContacts(3))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(contacts : List<Contact>, onFab: () -> Unit = {}) {
   Box {
      LazyColumn(contentPadding = PaddingValues(bottom = 20.dp),) {
         items(items = contacts,
            key = { it.contactId })
         { contact ->
            ContactItem(imgId = contact.picture,
               name = contact.name,
               dateMessage = "Example message") //TODO:
         }
      }
      FloatingActionButton(
         modifier = Modifier
            .padding(40.dp)
            .align(Alignment.BottomEnd),
         onClick = { onFab() },
      ) {
         Icon(Icons.Filled.Add, "Add a new contact")
      }
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ItemTest() {
   ContactItem(imgId = R.drawable.smiles001,
      name = "Noah Ortega",
      dateMessage = "it's been 14 days")
}
@Composable
@ExperimentalMaterial3Api
fun ContactItem(imgId: Int, name: String, dateMessage: String,
                modifier: Modifier = Modifier, isExpired: Boolean = false,
                onItemClicked: () -> Unit = {}, onIconTap: () -> Unit = {}) {
   ListItem(modifier = modifier.clickable { onItemClicked()},
      leadingContent = {
         ContactImage(modifier = Modifier.width(48.dp), imgId = imgId, name = name)
      },
      headlineText = { Text(text = name) },
      supportingText = { Text(dateMessage) },
      trailingContent = {
         Icon(
            painter = if(isExpired) painterResource(id = R.drawable.ic_baseline_heart_broken_24)
                        else painterResource(id = R.drawable.ic_baseline_favorite_24),
            contentDescription = "Mark as Contacted",
            modifier = Modifier
               .size(32.dp)
               .clickable {
                  onIconTap()
               }
         )
      }
   )
}

//@Preview(showBackground = true)
@Composable
fun SuggTest() {
   val vm : HeartsViewModel = viewModel()
   val contact = vm.dummyContact()
   Surface() {
      SuggestionScreen(name = contact.name, imgId = contact.picture)
   }
}
@Composable
fun SuggestionScreen(name: String, imgId : Int, modifier: Modifier = Modifier, 
                     onChat: () -> Unit = {}, onIgnore: () -> Unit = {}) {
   val configuration = LocalConfiguration.current
   val screenHeight = configuration.screenHeightDp.dp
   val screenWidth = configuration.screenWidthDp.dp

   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally) {

      Text(style = MaterialTheme.typography.titleLarge,text = "Why not chat with...")

      ContactImage(modifier = Modifier.width(min(screenHeight, screenWidth) / 2), imgId = imgId, name = name)

      Text(style = MaterialTheme.typography.titleLarge, text = name)

      Row() {
         val buttonModifier = Modifier.padding(horizontal = 5.dp)
         Button(modifier = buttonModifier,
            onClick = { onChat() }) {
            Icon(imageVector = Icons.Outlined.Favorite, null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "Will Do")
         }
         OutlinedButton(modifier = buttonModifier,
            onClick = { onIgnore() }) {
            Icon(imageVector = Icons.Filled.Close, null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "Not Now")
         }
      }
   }
}

@Composable
fun ContactImage(modifier: Modifier,imgId: Int, name: String) {
   val iconColor = if(isSystemInDarkTheme())MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
   Box(modifier = modifier
      .clip(CircleShape)
      .background(iconColor)) {
      Image(painter = painterResource(id = imgId),
         contentDescription = "Profile picture of $name",
         modifier = Modifier
            .clip(CircleShape)
            .border(BorderStroke(2.dp, iconColor), CircleShape))
   }
}

