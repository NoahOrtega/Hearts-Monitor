package info.noahortega.heartsmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.noahortega.heartsmonitor.ui.theme.HeartsMonitorTheme

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         HeartsMonitorTheme {
            Surface(Modifier.fillMaxSize()) {
               EntryPoint()
            }
         }
      }
   }
}


sealed class Screen(val route: String, @StringRes val resourceId: Int, val icon : ImageVector) {
   object Contact : Screen("contact", R.string.screen_contact, Icons.Filled.Favorite)
   object Edit : Screen("edit", R.string.screen_edit, Icons.Filled.Favorite)
   object Nudge : Screen("nudge", R.string.screen_nudge, Icons.Filled.Warning)
   object Random : Screen("random", R.string.screen_random, Icons.Filled.Refresh)
}

@Preview
@Composable
fun EntryPoint() {
   val vm : HeartsViewModel = viewModel()
   val nav = rememberNavController()
   MainScaffold(nav = nav,
      content = {
         val mod = Modifier.padding(it)
         NavHost(navController = nav, startDestination = Screen.Contact.route) {
            composable(Screen.Contact.route) { ContactsScreen (
               contacts = listOf(),
               modifier = mod.fillMaxSize(),
               onFab = {
                  vm.onFabPressed()
                  nav.navigate(Screen.Edit.route)
               })}
            composable(Screen.Edit.route) {
               EditContactScreen(
                  name = vm.myEditState.name.value, picture = vm.myEditState.imgId.value,
                  isNudger = vm.myEditState.isNudger.value, dayInterval = vm.myEditState.nudgeDayInterval.value,
                  onRandomPressed = {vm.onRandomPicPress()},
                  modifier = mod.fillMaxSize(),
                  onSavePressed = {
                     //TODO: save in vm + prevent multiple presses
                     nav.popBackStack()
                  },
                  onCancelPressed = {
                     nav.popBackStack()
                  },
                  onNameChange = {name -> vm.myEditState.name.value = name},
                  onDayIntervalChange = {interval -> vm.tryToChangeInterval(interval)},
                  onNudgeChange = {doNudge -> vm.myEditState.isNudger.value = doNudge},
               )}
            composable(Screen.Nudge.route) { }
            composable(Screen.Random.route) { SuggestionScreen(
               contact = vm.suggestedContact.value,
               modifier = mod.fillMaxSize(),
               onChat = { vm.contactSuggestionPressed()},
               onIgnore = {vm.newSuggestionPressed()})}
         }
      })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(modifier: Modifier = Modifier,
                 nav: NavHostController,
                 content: @Composable (PaddingValues) -> Unit) {
   Scaffold(
      modifier = modifier,
      bottomBar = { BottomBar(nav = nav)},
   ) {  contentPadding -> content(contentPadding) }
}

val screenList = listOf(
   Screen.Contact,
   Screen.Random,
)

@Composable
fun BottomBar(modifier: Modifier = Modifier, nav : NavHostController) {
   val navBackStackEntry by nav.currentBackStackEntryAsState()
   val currentDestination = navBackStackEntry?.destination

   BottomNavigation(modifier = modifier) {
      screenList.forEach { screen ->
         val onThisScreen = currentDestination?.hierarchy?.any { it.route == screen.route }
         BottomNavigationItem(
            icon = { Icon(screen.icon, null) },
            label = { Text(stringResource(id = screen.resourceId)) },
            selected = onThisScreen == true,
            onClick = {
               nav.navigate(screen.route) {
                  //save entire back stack and pop it until we return to this route
                  popUpTo(nav.graph.findStartDestination().id) {
                     saveState = true
                  }
                  //prevent copies on top of the backstack
                  launchSingleTop = true
                  // Restore state when selecting a previously selected item
                  restoreState = true

               }
            }
         )
      }
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(name: String, picture: Int, isNudger: Boolean, dayInterval: String?,
                      onNameChange: (String) -> Unit, onDayIntervalChange: (String) -> Unit, onNudgeChange: (Boolean)-> Unit,
                      modifier: Modifier = Modifier,
                      onRandomPressed: () -> Unit, onSavePressed: () -> Unit, onCancelPressed: () -> Unit,) {
   Column(modifier = modifier
      .padding(40.dp)
      .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
   ) {
      Column(Modifier.width(intrinsicSize = IntrinsicSize.Max)) {
         Row(verticalAlignment = Alignment.CenterVertically) {
            ContactImage(modifier = Modifier.width(96.dp), imgId = picture, name = "Edit Screen")
            Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = { onRandomPressed() }) {
               Text(text = "Shuffle Profile Pic")
            }
         }
         Spacer(modifier = Modifier.size(16.dp))
         OutlinedTextField( //TODO:text size limit
            value = name,
            onValueChange = { onNameChange(it) },
            label = { Text("Contact Name") },
            singleLine = true,

         )
         Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 20.dp))
         Text(text = "Nudge Settings:")
         Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isNudger, onCheckedChange = {onNudgeChange(it)}) //Todo: make work
            Text(text = "Nudge me to message them")
         }
         OutlinedTextField(
            value = dayInterval?.toString() ?: "",
            onValueChange = { onDayIntervalChange(it) },
            label = { Text("How Often (In Days)") },
            enabled = isNudger
         )
         Row(horizontalArrangement = Arrangement.SpaceEvenly,
         modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)) {
            val buttonModifier = Modifier.width(102.dp)
            Button(modifier = buttonModifier,
               onClick = { onSavePressed() }) {
               Text(text = "Save" )
            }
            OutlinedButton(modifier = buttonModifier,
               onClick = { onCancelPressed() }) {
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
   ContactsScreen(contacts = vm.dummyContacts(4))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(contacts : List<Contact>, modifier: Modifier = Modifier, onFab: () -> Unit = {}) {
   Box(modifier = modifier) {
      LazyColumn(contentPadding = PaddingValues(bottom = 20.dp),) {
         items(contacts)
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
      SuggestionScreen(contact = contact)
   }
}
@Composable
fun SuggestionScreen(contact: Contact?, modifier: Modifier = Modifier,
                     onChat: () -> Unit = {}, onIgnore: () -> Unit = {}) {
   val configuration = LocalConfiguration.current
   val screenHeight = configuration.screenHeightDp.dp
   val screenWidth = configuration.screenWidthDp.dp

   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally) {

      if(contact != null) {
         Text(style = MaterialTheme.typography.titleLarge,text = "Why not chat with...")

         ContactImage(modifier = Modifier.width(min(screenHeight, screenWidth) / 2), imgId = contact.picture, name = contact.name)

         Text(style = MaterialTheme.typography.titleLarge, text = contact.name)
      }
      else {
         Text(style = MaterialTheme.typography.titleLarge,text = "Why not chat add a new contact")

         ContactImage(modifier = Modifier.width(min(screenHeight, screenWidth) / 2), imgId = R.drawable.smiles000, name = "Default Contact")

         Text(style = MaterialTheme.typography.titleLarge, text = "")
      }


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

