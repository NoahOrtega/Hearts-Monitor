package info.noahortega.heartsmonitor

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.os.Bundle
import android.widget.DatePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import java.time.LocalDate
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


@Composable
fun EntryPoint() {
   val vm : HeartsViewModel = viewModel()

   LaunchedEffect(true ) {
      vm.composeLateInit()
   }

   val nav = rememberNavController()
   MainScaffold(
      nav = nav,
      nudgesNum = vm.contactList.count { (it.nextNudgeDate != null) && (vm.nudgeIsOverdue(it.nextNudgeDate)) },
      hasNudges = vm.contactList.find {it.isNudger} != null,
      content = {
         val mod = Modifier.padding(it)
         NavHost(navController = nav, startDestination = Screen.Contact.route) {
            composable(Screen.Contact.route) {
               ContactsScreen (
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
            composable(Screen.Random.route) {
               SuggestionScreen(
                  contact = vm.suggestedContact,
                  modifier = mod.fillMaxSize(),
                  launchLogic = {vm.suggestLaunchLogic()},
                  onChat = { vm.contactSuggestionPressed()},
                  onIgnore = {vm.newSuggestionPressed()},
                  onAddContact = {nav.navigate((Screen.Contact.route))} )}
            composable(Screen.Edit.route) {
               EditContactScreen(
                  name = vm.myEditState.name, picture = vm.myEditState.imgId,
                  isNudger = vm.myEditState.isNudger, dayInterval = vm.myEditState.nudgeDayInterval,
                  lastContacted = vm.myEditState.lastMessagedDate,
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
                  onLastContactedChanged = {date -> vm.myEditState.lastMessagedDate = date},
                  onNameChange = {name -> vm.myEditState.name = name},
                  onDayIntervalChange = {interval -> vm.tryToChangeInterval(interval)},
                  onNudgeChange = {doNudge -> vm.myEditState.isNudger = doNudge},
               )}
            composable(Screen.Nudge.route) {
               NudgeScreen(
                  nudgeContacts = vm.contactList.filter { item -> item.isNudger },
                  contactListItemMessage = vm::nudgeListItemMessage,
                  modifier = mod.fillMaxSize(),
                  onTrashTapped = vm::onTrashPressed,
                  onHeartTapped = vm::onHeartPressed,
                  onItemTapped = {contact: Contact ->
                     vm.onContactPressed(contact)
                     nav.navigate(Screen.Edit.route)},
                  checkIfExpired = vm::nudgeIsOverdue)
            }
         }
      })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(modifier: Modifier = Modifier,
                 nav: NavHostController,
                 nudgesNum: Int,
                 hasNudges: Boolean,
                 content: @Composable (PaddingValues) -> Unit) {
   Scaffold(
      modifier = modifier,
      bottomBar = { BottomBar(nav = nav, hasNudges = hasNudges, nudgesNum = nudgesNum)},
   ) {  contentPadding -> content(contentPadding) }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(modifier: Modifier = Modifier, nav : NavHostController, hasNudges: Boolean, nudgesNum : Int) {
   val navBackStackEntry by nav.currentBackStackEntryAsState()
   val currentDestination = navBackStackEntry?.destination

   val screenList = mutableListOf<Screen>(Screen.Contact).also {
      if(hasNudges) {
         it.add(Screen.Nudge)
      }
      it.add(Screen.Random)
   }

   NavigationBar(modifier = modifier) {
      screenList.forEach { screen ->
         val onThisScreen = currentDestination?.hierarchy?.any { it.route == screen.route }
         NavigationBarItem(
            icon = {
               if(screen is Screen.Nudge && nudgesNum > 0) {
                  BadgedBox(badge = { Badge { Text(nudgesNum.toString()) } }) {
                     Icon(screen.icon, null)
                  }
               }
               else {
                  Icon(screen.icon, null)
               }
                   },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditContactScreen(
   name: String, picture: Int, isNudger: Boolean, dayInterval: String?, lastContacted: LocalDate,
   nameErrorMessage: String?, nudgeErrorMessage: String?,
   onNameChange: (String) -> Unit, onDayIntervalChange: (String) -> Unit,
   onNudgeChange: (Boolean) -> Unit, onLastContactedChanged: (LocalDate) -> Unit,
   modifier: Modifier = Modifier,
   onRandomPressed: () -> Unit, onSavePressed: () -> Unit, onCancelPressed: () -> Unit,
) {
   val keyboardController = LocalSoftwareKeyboardController.current

   Column(
      modifier = modifier
         .padding(40.dp)
         .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
   ) {
      Column(Modifier.width(intrinsicSize = IntrinsicSize.Max)) {
         //pfp settings
         Row(verticalAlignment = Alignment.CenterVertically) {
            ContactImage(modifier = Modifier.width(96.dp), imgId = picture, name = "Edit Screen")
            Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = { onRandomPressed() }) {
               Text(text = "Shuffle Profile Pic")
            }
         }

         //general settings
         Spacer(modifier = Modifier.size(16.dp))
         OutlinedTextField(
            value = name,
            leadingIcon = { Icon(Icons.Filled.Person, null)},
            onValueChange = { onNameChange(it) },
            label = { Text("Contact Name") },
            singleLine = true,
            isError = (nameErrorMessage != null),
            keyboardActions = KeyboardActions(onDone = {keyboardController?.hide()})
         )
            Text(
               text = nameErrorMessage ?: "",
               color = MaterialTheme.colorScheme.error,
               style = MaterialTheme.typography.labelSmall
            )

         //last messaged date picker
         val myYear: Int = lastContacted.year
         val myMonth: Int = lastContacted.monthValue - 1
         val myDay: Int = lastContacted.dayOfMonth

         val datePickerDialog = DatePickerDialog(
            LocalContext.current,
            { _: DatePicker, year: Int, month: Int, day: Int ->
               onLastContactedChanged(LocalDate.of(year, month + 1, day))
            }, myYear, myMonth, myDay
         ).also {
            it.datePicker.maxDate = System.currentTimeMillis()
         }

         OutlinedTextField(
            value = lastContacted.toString().replace('-','/'),
            enabled = false,
            leadingIcon = {Icon(Icons.Filled.DateRange,null)},
            onValueChange = {},
            label = { Text("Last Messaged On") },
            modifier = Modifier.clickable { datePickerDialog.show() },
            colors = TextFieldDefaults.outlinedTextFieldColors(
               disabledTextColor = MaterialTheme.colorScheme.onSurface,
               disabledBorderColor = MaterialTheme.colorScheme.outline,
               disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
               disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
         )

         //nudge settings
         Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 20.dp))
         Text(text = "Nudge Settings:")
         Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isNudger, onCheckedChange = { onNudgeChange(it) })
            Text(text = "Nudge me to message them")
         }

         OutlinedTextField(
            isError = (nudgeErrorMessage != null),
            value = dayInterval ?: "",
            leadingIcon = {Icon(Icons.Filled.Refresh, "How often should you be reminded")},
            onValueChange = { onDayIntervalChange(it) },
            label = { Text("How Often (In Days)") },
            enabled = isNudger,
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = {keyboardController?.hide()}),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
         )


            Text(
               text = nudgeErrorMessage ?: "",
               color = MaterialTheme.colorScheme.error,
               style = MaterialTheme.typography.labelSmall
            )

         //save and cancel buttons
         Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
               .fillMaxWidth()
               .padding(vertical = 24.dp)
         ) {
            val buttonModifier = Modifier.width(102.dp)
            Button(modifier = buttonModifier,
               onClick = { onSavePressed() }) {
               Text(text = "Save")
            }
            OutlinedButton(modifier = buttonModifier,
               onClick = { onCancelPressed() }) {
               Text(text = "Cancel")
            }
         }
      }
   }
}

@Composable
fun DatePickerButton() {

}

////@Preview
//@Composable
//fun TestContactsScreen() {
//   val vm: HeartsViewModel = viewModel()
//   ContactsScreen(contacts = vm.dummyContacts(4))
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
   contacts: List<Contact>, modifier: Modifier = Modifier,
   contactListItemMessage: (LocalDate) -> String,
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
         headlineText = { Text(
            text = name,
         )},
         supportingText = { Text(text = dateMessage,
            color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground) },
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

//@Preview(showBackground = true, device = Devices.AUTOMOTIVE_1024p, widthDp = 1024)
//@Preview(showSystemUi = true, device = Devices.PIXEL_2)
@Composable
fun SuggestionTest() {
   val contact = Contact(
      contactId = 0,
      name = "Noah Ortega",
      picture = R.drawable.hearties_64,
      lastMessageDate = LocalDate.now(),
      isNudger = false,
      nudgeDayInterval = null,
   )
   SuggestionScreen(contact = contact)
}

@Composable
fun SuggestionScreen(contact: Contact?, modifier: Modifier = Modifier,
                     launchLogic: () -> Unit = {},
                     onChat: () -> Unit = {}, onIgnore: () -> Unit = {}, onAddContact: () -> Unit = {}) {
   val configuration = LocalConfiguration.current
   val screenHeight = configuration.screenHeightDp.dp
   val screenWidth = configuration.screenWidthDp.dp
   val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

   LaunchedEffect(true) {
      launchLogic()
   }

   Row(modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center) {
      Column(
         modifier = Modifier.padding(horizontal = 30.dp),
         horizontalAlignment = Alignment.CenterHorizontally
      ) {

         val noContacts = (contact == null)

         Text(
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            text = if (noContacts) "Why not create a new contact" else "Why not have a chat with",
         )

         Spacer(modifier = Modifier.padding(vertical = if(isPortrait) 20.dp else 2.dp))

         ContactImage(
            modifier = Modifier
               .size(min(screenHeight, screenWidth) / 2),
            imgId = if (noContacts) R.drawable.hearties_q else contact!!.picture,
            name = if (noContacts) "Missing Contact" else contact!!.name
         )

         if (noContacts) {
            Spacer(modifier = Modifier.padding(vertical = if(isPortrait) 20.dp else 2.dp))

            OutlinedButton(modifier = Modifier.padding(horizontal = 5.dp),
               onClick = { onAddContact() }) {
               Icon(imageVector = Icons.Filled.Favorite, null)
               Spacer(Modifier.size(ButtonDefaults.IconSpacing))
               Text(text = "Add Contact")
            }
         } else {
            Spacer(modifier = Modifier.padding(vertical = if(isPortrait) 10.dp else 1.dp))

            Text(style = MaterialTheme.typography.headlineLarge, text = contact!!.name)

            Spacer(modifier = Modifier.padding(vertical = if(isPortrait) 10.dp else 1.dp))

            if(isPortrait) {
               Row {
                  SuggestionButtons(modifier = Modifier.padding(5.dp), onChat = onChat, onIgnore = onIgnore)
               }
            }
         }
      }
      if(!isPortrait) {
         Column {
            SuggestionButtons(modifier = Modifier.padding(5.dp), onChat = onChat, onIgnore = onIgnore)
         }
      }
   }
}

@Composable
fun SuggestionButtons(modifier: Modifier, onChat: () -> Unit, onIgnore: () -> Unit) {
   Button(modifier = modifier,
      onClick = { onChat() }) {
      Icon(imageVector = Icons.Filled.Favorite, null)
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text(text = "Will Do")
   }
   OutlinedButton(modifier = modifier,
      onClick = { onIgnore() }) {
      Icon(imageVector = Icons.Filled.Close, null)
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text(text = "Not Now")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeScreen(nudgeContacts : List<Contact>, modifier : Modifier = Modifier,
                contactListItemMessage: (nextNudgeDay : LocalDate) -> String,
                checkIfExpired: (LocalDate) -> Boolean,
                onItemTapped: (Contact) -> Unit,
                onTrashTapped: (Contact) -> Unit, onHeartTapped: (contactId: Long) -> Unit) {
   Box(modifier = modifier) {
      LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
         items(items = nudgeContacts, key = {it.contactId})
         { contact ->
            ContactItem(
               isExpired = checkIfExpired(contact.nextNudgeDate!!),
               imgId = contact.picture,
               name = contact.name,
               dateMessage = contactListItemMessage(contact.nextNudgeDate),
               onItemTapped = { onItemTapped(contact) },
               onTrashTapped = {onTrashTapped(contact)},
               onIconTap = {onHeartTapped(contact.contactId)}
            )
         }
      }
   }
}
