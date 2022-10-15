package info.noahortega.heartsmonitor

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import info.noahortega.heartsmonitor.room.AppDatabase
import info.noahortega.heartsmonitor.room.entities.Contact
import info.noahortega.heartsmonitor.room.entities.ContactDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
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

class HeartsViewModel(application: Application) : AndroidViewModel(application) {
   //General data state functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   private val _contactList = mutableStateListOf<Contact>()
   val contactList: List<Contact> = _contactList

   var suggestedContact by mutableStateOf(null as Contact?)
      private set

   private var dao: ContactDao
   init {
      val db = Room.databaseBuilder(
         application,
         AppDatabase::class.java, "database-name"
      ).build()
      dao = db.contactDao()
   }

   fun composeLateInit() {
      Log.i("<3", "Launched Effect late init")
      viewModelScope.launch(Dispatchers.IO) {
            if(_contactList.isEmpty()) _contactList.addAll(dao.getAll())
      }
   }

   private fun addContact(contact : Contact) {
      viewModelScope.launch(Dispatchers.IO) {
            dao.insert(contact).also { dbRowId ->
               _contactList.add(contact.copy(contactId = dbRowId))
               Log.i("<3 Room", "+ Room: added contact '${contact.name}' with id $dbRowId")
            }
      }
   }

   private fun removeContact(contact: Contact) {
      viewModelScope.launch(Dispatchers.IO) {
         dao.delete(contact)
         _contactList.remove(contact)
         Log.i("<3 Room", "- Room: removed contact '${contact.name}' with id ${contact.contactId}")
      }
   }

   private fun markAsContacted(contactId: Long) {
      viewModelScope.launch(Dispatchers.IO) {
         val itemIndex = _contactList.indexOfFirst { it.contactId == contactId}
         val updatedContact = _contactList[itemIndex].copy(lastMessageDate = LocalDateTime.now())
         dao.update(updatedContact)
         _contactList[itemIndex] = updatedContact
         Log.i("<3 Room", "> Room: marked '${updatedContact.name}' as contacted with id ${updatedContact.contactId}")
      }
   }

   private fun updateContact(editedContact: Contact) {
      viewModelScope.launch(Dispatchers.IO) {
         val itemIndex = _contactList.indexOfFirst { it.contactId == editedContact.contactId}
         dao.update(editedContact)
         _contactList[itemIndex] = editedContact
         Log.i("<3 Room", "> Room: edited contact '${editedContact.name}' with id ${editedContact.contactId}")
      }
   }

   //Contacts Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
   class EditUIState {
      var contact: Contact = Contact()
      var name by mutableStateOf("default")
      var nameError by mutableStateOf(null as String?)
      var imgId by mutableStateOf(defaultProfilePics.first())
      var isNudger by mutableStateOf(false)
      var nudgeDayInterval by mutableStateOf(null as String?)
      var nudgeError by mutableStateOf(null as String?)
   }
   private fun EditUIState.setState(contact: Contact?) {
         this.apply {
            this.contact = contact ?: Contact()
            this.name = contact?.name ?: ""

            this.imgId = contact?.picture ?: defaultProfilePics.random()
            this.isNudger = contact?.isNudger ?: false
            this.nudgeDayInterval = contact?.nudgeDayInterval?.toString() ?: ""
            this.clearErrors()
         }
   }
   private fun EditUIState.clearErrors() {
      this.apply {
         this.nameError = null
         this.nudgeError = null
      }
   }

   fun onRandomPicPress() {
      myEditState.imgId = defaultProfilePics.filter { it != myEditState.imgId}.random()
   }
   fun tryToChangeInterval(interval: String) {
      if(interval.toIntOrNull() != null || interval == "") myEditState.nudgeDayInterval = interval
   }
   fun onSavePressed() {
      //todo: clear errors
      myEditState.clearErrors()
      if(myEditState.name != "" && myEditState.name.length < 40) {
         if (!myEditState.isNudger || myEditState.nudgeDayInterval?.toIntOrNull() != null) {
            val nudgeDayInterval: Int? = myEditState.nudgeDayInterval?.toIntOrNull()
            val newContact = myEditState.contact.copy(
               name = myEditState.name.replace("\n", "").replace("\r", ""),
               picture = myEditState.imgId,
               lastMessageDate = LocalDateTime.now(),
               isNudger = myEditState.isNudger,
               nudgeDayInterval = nudgeDayInterval,
               nextNudgeDate = nudgeDayInterval?.let {LocalDateTime.now().plusDays(it.toLong())}
            )
            if (newContact.contactId == 0L) {
               addContact(newContact)
            }
            else {
               updateContact(newContact)
            }
         }
         else {
            myEditState.nudgeError = "* Nudge interval must be greater than zero."
         }
      }
      else {
         myEditState.nameError = "* Names must be between 1 and 40 characters"
      }
   }

   //Suggest Screen Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   fun suggestLaunchLogic() {
      val updatedContact = contactList.find { it.contactId == suggestedContact?.contactId }
      suggestedContact = updatedContact ?: getRandomContact(null)
   }

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




