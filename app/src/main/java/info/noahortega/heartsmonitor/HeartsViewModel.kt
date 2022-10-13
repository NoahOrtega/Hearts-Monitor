package info.noahortega.heartsmonitor

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*
import java.time.temporal.ChronoUnit
import kotlin.random.Random

val defaultProfilePics = listOf(
   R.drawable.smiles000,
   R.drawable.smiles001,
   R.drawable.smiles002,
   R.drawable.smiles003,
   R.drawable.smiles004,
   R.drawable.smiles005,
)

class HeartsViewModel : ViewModel() {
   //General data state functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   private val _contactList = mutableStateListOf<Contact>()
   val contactList: List<Contact> = _contactList

   var suggestedContact by mutableStateOf(null as Contact?)
      private set

   init {
      getFullContactList()
   }

   private fun getFullContactList() {
      viewModelScope.launch(Dispatchers.Main) {
         withContext(Dispatchers.Main) {
            //FIXME: should probably be a launched effect in the composable
            _contactList.addAll(dummyContacts(10))
            suggestedContact = getRandomContact(null)
         }
      }
   }

   private fun addContact(contact : Contact) {
      _contactList.add(contact)
      suggestedContact = getRandomContact(null)
      //todo: add to database
   }

   private fun removeContact(contact: Contact?) {
      _contactList.remove(contact)
      //todo: remove from database
      suggestedContact = getRandomContact(null)
   }

   private fun markAsContacted(contactId: Long) {
      val itemIndex = _contactList.indexOfFirst { it.contactId == contactId}
      _contactList[itemIndex] = _contactList[itemIndex].copy(lastMessageDate = LocalDateTime.now())
      //todo: update database
   }

   //Contacts Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   var expandedItemId by mutableStateOf(null as Long?)
      private set

   fun onFabPressed() {
      myEditState.setState(null)
   }
   fun onContactPressed(contact: Contact) {
      myEditState.setState(contact)
   }
   fun onHeartPressed(contactId: Long) {
      markAsContacted(contactId)
   }
   fun onTrashPressed(contact: Contact) {
      removeContact(contact)
   }

   //Contacts Screen Helpers ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   fun contactListItemMessage(lastContacted: LocalDateTime) : String {
      val curTime = LocalDateTime.now()
      lastContacted.until(curTime, ChronoUnit.HOURS).run {
         if (this < 24) return "Contacted Today!"
      }
      lastContacted.until(curTime, ChronoUnit.DAYS).run {
         if (this < 365) return "It's been $this days..."
      }
      lastContacted.until(curTime, ChronoUnit.DAYS).run {
         return "It's been $this months..."
      }
   }

   //Edit Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   val myEditState = EditUIState().apply { this.setState(null)}
   class EditUIState() {
      var name by mutableStateOf("default")
      var imgId by mutableStateOf(defaultProfilePics.first())
      var isNudger by mutableStateOf(false)
      var nudgeDayInterval by mutableStateOf(null as String?)
   }
   private fun EditUIState.setState(contact: Contact?) {
         this.apply {
            this.name = contact?.name ?: ""
            this.imgId = contact?.picture ?: defaultProfilePics.random()
            this.isNudger = contact?.isNudger ?: false
            this.nudgeDayInterval = contact?.nudgeDayInterval?.toString() ?: ""
         }
   }

   fun onRandomPicPress() {
      myEditState.imgId = defaultProfilePics.filter { it != myEditState.imgId}.random()
   }
   fun tryToChangeInterval(interval: String) {
      if(interval.toIntOrNull() != null || interval == "") myEditState.nudgeDayInterval = interval
   }
   fun onSavePressed() {
      if(myEditState.name != "") {
         if (!myEditState.isNudger || myEditState.nudgeDayInterval?.toIntOrNull() != null) {
            val nudgeDayInterval: Int? = myEditState.nudgeDayInterval?.toIntOrNull()
            val newContact = Contact(
               //FIXME: just for testing until the database is set up
               contactId = (0..50000).random().toLong(),
               name = myEditState.name,
               picture = myEditState.imgId,
               lastMessageDate = LocalDateTime.now(),
               isNudger = myEditState.isNudger,
               nudgeDayInterval = nudgeDayInterval,
               nextNudgeDate = nudgeDayInterval?.let {LocalDateTime.now().plusDays(it.toLong())}
            )
            addContact(newContact)
         }
         else {
            //TODO: nudge interval error message
         }
      }
      else {
         //TODO: name error message
      }
   }

   //Suggest Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   fun newSuggestionPressed() {
      suggestedContact = getRandomContact(suggestedContact)
   }
   fun contactSuggestionPressed() {
      suggestedContact?.let {
         markAsContacted(it.contactId) //this button is hidden when null
         suggestedContact = getRandomContact(it)
      }
   }

   //General Helper Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   private fun getRandomContact(oldContact: Contact?) : Contact? {
      val strippedList = contactList.filter { it.contactId != oldContact?.contactId}
      return if(strippedList.isEmpty()) {
         null
      } else {
         strippedList.random()
      }
   }

   //Test Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   fun dummyContact() : Contact {
      return Contact(
         contactId = -100,
         name = "Noah Ortega",
         picture = R.drawable.smiles000,
         lastMessageDate = LocalDateTime.of(2022, 8, 15, 0, 0, 0),
         isNudger = true,
         nudgeDayInterval = 30,
         nextNudgeDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
      )
   }

   fun dummyContacts(numContacts: Int) : List<Contact> {
      val contacts = mutableListOf<Contact>()
      for(i in 1..numContacts) {
         contacts.add(
            Contact(
               contactId = -i.toLong(),
               name = "test contact $i",
               picture = defaultProfilePics.random(),
               lastMessageDate =randomTimeBetween(
                  LocalDateTime.of(2022, 8, 15, 0, 0, 0)
                  ,LocalDateTime.now()),
               isNudger = false,
               nudgeDayInterval = 0,
               nextNudgeDate =randomTimeBetween(
                  LocalDateTime.now(),
                  LocalDateTime.of(2023, 1, 1, 0, 0, 0))
            )
         )
      }
      return contacts
   }

   private fun randomTimeBetween(startInclusive: LocalDateTime, endExclusive: LocalDateTime): LocalDateTime {
      val startSeconds: Long = startInclusive.toEpochSecond(ZoneOffset.UTC)
      val endSeconds: Long = endExclusive.toEpochSecond(ZoneOffset.UTC)
      val random: Long = Random.nextLong(startSeconds, endSeconds)
      return LocalDateTime.ofEpochSecond(random, 0, ZoneOffset.UTC)
   }
}




