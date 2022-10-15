package info.noahortega.heartsmonitor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
import info.noahortega.heartsmonitor.room.entities.Contact
import info.noahortega.heartsmonitor.ui.theme.HeartsMonitorTheme
import java.time.LocalDateTime
import kotlin.math.roundToInt

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

   LaunchedEffect(true ) {
      vm.composeLateInit()
   }

   val nav = rememberNavController()
   MainScaffold(
      nav = nav,
      content = {
         val mod = Modifier.padding(it)
         NavHost(navController = nav, startDestination = Screen.Contact.route) {
            composable(Screen.Contact.route) { ContactsScreen (
               contacts = vm.contactList,
               modifier = mod.fillMaxSize(),
               onFab = {
                  vm.onFabPressed()
                  nav.navigate(Screen.Edit.route)
               },
               contactListItemMessage = vm::contactListItemMessage,
               onHeartTapped = vm::onHeartPressed,
               onTrashTapped = vm::onTrashPressed,
               onItemTapped = { contact: Contact ->
                  vm.onContactPressed(contact)
                  nav.navigate(Screen.Edit.route)
               })}
            composable(Screen.Edit.route) {
               EditContactScreen(
                  name = vm.myEditState.name, picture = vm.myEditState.imgId,
                  isNudger = vm.myEditState.isNudger, dayInterval = vm.myEditState.nudgeDayInterval,
                  onRandomPressed = {vm.onRandomPicPress()},
                  modifier = mod.fillMaxSize(),
                  nameErrorMessage = vm.myEditState.nameError,
                  nudgeErrorMessage = vm.myEditState.nudgeError,
                  onSavePressed = {
                     vm.onSavePressed()
                     if (vm.myEditState.nameError ==  null && vm.myEditState.nudgeError == null) {
                           nav.popBackStack()
                     }
                  },
                  onCancelPressed = {
                     nav.popBackStack()
                  },
                  onNameChange = {name -> vm.myEditState.name = name},
                  onDayIntervalChange = {interval -> vm.tryToChangeInterval(interval)},
                  onNudgeChange = {doNudge -> vm.myEditState.isNudger = doNudge},
               )}
            composable(Screen.Nudge.route) { }
            composable(Screen.Random.route) { SuggestionScreen(
               contact = vm.suggestedContact,
               modifier = mod.fillMaxSize(),
               launchLogic = {vm.suggestLaunchLogic()},
               onChat = { vm.contactSuggestionPressed()},
               onIgnore = {vm.newSuggestionPressed()},
               onAddContact = {nav.navigate((Screen.Contact.route))} )}
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

   NavigationBar(modifier = modifier) {
      screenList.forEach { screen ->
         val onThisScreen = currentDestination?.hierarchy?.any { it.route == screen.route }
         NavigationBarItem(
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
                      nameErrorMessage: String?, nudgeErrorMessage: String?,
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
            isError = (nameErrorMessage != null),
         )

         nameErrorMessage?.let { Text(text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall) }

         Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 20.dp))
         Text(text = "Nudge Settings:")
         Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isNudger, onCheckedChange = {onNudgeChange(it)}) //Todo: make work
            Text(text = "Nudge me to message them")
         }

         OutlinedTextField(
            isError = (nudgeErrorMessage != null),
            value = dayInterval ?: "",
            onValueChange = { onDayIntervalChange(it) },
            label = {Text("How Often (In Days)") },
            enabled = isNudger
         )

         nudgeErrorMessage?.let { Text(text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall) }

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

////@Preview
//@Composable
//fun TestContactsScreen() {
//   val vm: HeartsViewModel = viewModel()
//   ContactsScreen(contacts = vm.dummyContacts(4))
//}

@OptIn(ExperimentalMaterial3Api::class,)
@Composable
fun ContactsScreen(
   contacts: List<Contact>, modifier: Modifier = Modifier,
   contactListItemMessage: (LocalDateTime) -> String,
   onHeartTapped: (contactId: Long) -> Unit,
   onItemTapped: (Contact) -> Unit,
   onTrashTapped: (Contact) -> Unit,
   onFab: () -> Unit = {},
) {
   Box(modifier = modifier) {
      LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
         items(items = contacts, key = {it.contactId})
         { contact ->
               ContactItem(imgId = contact.picture,
                  name = contact.name,
                  dateMessage = contactListItemMessage(contact.lastMessageDate),
                  onItemTapped = {onItemTapped(contact)},
                  onTrashTapped = {onTrashTapped(contact)},
                  onIconTap = {onHeartTapped(contact.contactId)})
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

//@OptIn(ExperimentalMaterial3Api::class)
//@Preview
//@Composable
//fun ItemTest() {
//   ContactItem(
//      imgId = R.drawable.smiles001,
//      name = "Noah Ortega",
//      dateMessage = "it's been 14 days"
//   ) { onTrashTapped(contact) }
//}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@ExperimentalMaterial3Api
fun ContactItem(
   imgId: Int, name: String, dateMessage: String,
   modifier: Modifier = Modifier, isExpired: Boolean = false,
   onItemTapped: () -> Unit = {}, onIconTap: () -> Unit = {}, onTrashTapped: () -> Unit
) {
   val swipeableState = rememberSwipeableState(0)
   val sizePx = with(LocalDensity.current) { 80.dp.toPx() }
   val anchors = mapOf(0f to 0, sizePx to 1) // Maps anchor points (in px) to states

   Box(
      modifier = Modifier
         .swipeable(
            state = swipeableState,
            anchors = anchors,
            thresholds = { _, _ -> FractionalThreshold(0.3f) },
            orientation = Orientation.Horizontal
         )) {
      Icon(painter = painterResource(id = R.drawable.ic_round_delete_24), contentDescription = "Delete",
         tint = MaterialTheme.colorScheme.error,
         modifier = Modifier
            .offset(x = 16.dp, y = 0.dp)
            .size(40.dp)
            .align(alignment = Alignment.CenterStart)
            .clickable { onTrashTapped() })
      ListItem(
         modifier = modifier
            .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
            .clickable {
               onItemTapped()
            },
         leadingContent = {
            ContactImage(modifier = Modifier
               .width(48.dp),
               imgId = imgId, name = name)
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
}

@Composable
fun SuggestionScreen(contact: Contact?, modifier: Modifier = Modifier,
                     launchLogic: () -> Unit = {},
                     onChat: () -> Unit = {}, onIgnore: () -> Unit = {}, onAddContact: () -> Unit) {
   val configuration = LocalConfiguration.current
   val screenHeight = configuration.screenHeightDp.dp
   val screenWidth = configuration.screenWidthDp.dp

   LaunchedEffect(true) {
      launchLogic()
   }

   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally) {

      val noContacts = (contact == null)

      Text(style = MaterialTheme.typography.titleLarge,
         text = if (noContacts) "Why not create a new contact" else "Why not chat with")

      ContactImage(modifier = Modifier.size(min(screenHeight, screenWidth) / 2),
         imgId = if (noContacts) R.drawable.hearties_q else contact!!.picture,
         name = if (noContacts) "Missing Contact" else contact!!.name)

      if(noContacts) {
         OutlinedButton(modifier = Modifier.padding(horizontal = 5.dp),
            onClick = { onAddContact() }) {
            Icon(imageVector = Icons.Outlined.Favorite, null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "Add Contact")
         }
      }
      else {
         Text(style = MaterialTheme.typography.titleLarge, text = contact!!.name)

         Row {
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
}

@Composable
fun ContactImage(modifier: Modifier,imgId: Int, name: String) {
   val iconColor = if(isSystemInDarkTheme())MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
   Box(modifier = modifier
      .clip(CircleShape)
      .background(iconColor)) {
      Paint()
      Image(painter = painterResource(id = imgId),
         contentDescription = "Profile picture of $name",
         modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .border(BorderStroke(2.dp, iconColor), CircleShape))
   }
}

