package info.noahortega.heartsmonitor

import android.app.Application
import android.content.res.TypedArray
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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HeartsViewModel(application: Application) : AndroidViewModel(application) {
   //General data state functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   private val _contactList = mutableStateListOf<Contact>()
   val contactList: List<Contact> = _contactList
   var nudgesExist = _contactList.find { it.isNudger } != null

   var suggestedContact by mutableStateOf(null as Contact?)
      private set

   private var contactPictures : TypedArray

   private var dao: ContactDao
   init {
      contactPictures = application.resources.obtainTypedArray(R.array.hearties);

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
         val contactIndex = _contactList.indexOfFirst { it.contactId == contactId}
         val contact = _contactList[contactIndex]
         val newNextNudgeDate = calcNudgeDate(lastMessageDate = LocalDate.now(), nudgeInterval = contact.nudgeDayInterval)
         val updatedContact = contact.copy(lastMessageDate = LocalDate.now(), nextNudgeDate = newNextNudgeDate)

         dao.update(updatedContact)
         _contactList[contactIndex] = updatedContact
         Log.i("<3 Room", "> Room: marked '${updatedContact.name}' as contacted with id ${updatedContact.contactId}")
      }
   }

   private fun updateContact(editedContact: Contact) {
      viewModelScope.launch(Dispatchers.IO) {
         val itemIndex = _contactList.indexOfFirst { it.contactId == editedContact.contactId}
         val successful = dao.update(editedContact)
         if (successful == 1) {
            _contactList[itemIndex] = editedContact
            Log.i("<3 Room", "> Room: edited contact '${editedContact.name}' with id ${editedContact.contactId}")
         }
      }
   }

   //Edit Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   val myEditState = EditUIState().apply { this.setState(null)}
   class EditUIState {
      var contact: Contact = Contact()
      var name by mutableStateOf("default")
      var nameError by mutableStateOf(null as String?)
      var imgId by mutableStateOf(R.drawable.hearties_q)
      var isNudger by mutableStateOf(false)
      var lastMessagedDate by mutableStateOf(LocalDate.now())
      var nudgeDayInterval by mutableStateOf(null as String?)
      var nudgeError by mutableStateOf(null as String?)
   }
   private fun EditUIState.setState(contact: Contact?) {
      this.apply {
         this.contact = contact ?: Contact()
         this.name = contact?.name ?: ""
         this.imgId = contact?.picture ?: randomContactPicture()
         this.isNudger = contact?.isNudger ?: false
         this.nudgeDayInterval = contact?.nudgeDayInterval?.toString() ?: ""
         this.lastMessagedDate = contact?.lastMessageDate ?: LocalDate.now()
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
      myEditState.imgId = randomContactPicture()
   }

   fun tryToChangeInterval(interval: String) {
      if(interval.toIntOrNull()?.takeIf { it > 0 } != null || interval == "") myEditState.nudgeDayInterval = interval
   }

   fun onSavePressed() {
      myEditState.clearErrors()
      if(myEditState.name != "" && myEditState.name.length < 40) {
         if (!myEditState.isNudger || myEditState.nudgeDayInterval?.toIntOrNull() != null) {
            val nudgeDayInterval: Int? = myEditState.nudgeDayInterval?.toIntOrNull()
            val newContact = myEditState.contact.copy(
               name = myEditState.name.trim().replace("\n", "").replace("\r", ""),
               picture = myEditState.imgId,
               lastMessageDate = myEditState.lastMessagedDate,
               isNudger = myEditState.isNudger,
               nudgeDayInterval = nudgeDayInterval,
               nextNudgeDate = calcNudgeDate(myEditState.lastMessagedDate,nudgeDayInterval)
            )
            if (newContact.contactId == 0L) { addContact(newContact) }
            else { updateContact(newContact) }
         }
         else { myEditState.nudgeError = "* Nudge interval must be greater than zero." }
      }
      else { myEditState.nameError = "* Names must be between 1 and 40 characters" }
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
   fun contactListItemMessage(lastContacted: LocalDate) : String {
      val curTime = LocalDate.now()
      lastContacted.until(curTime, ChronoUnit.DAYS).run {
         if (this == 0L) return "Contacted Today!"
         if (this < 365) return "It's been $this days..."
      }
      lastContacted.until(curTime, ChronoUnit.MONTHS).run {
         return "It's been $this months..."
      }
   }

   // Nudge Screen Helpers
   private fun daysUntilNudge(nextNudgeDay : LocalDate) : Long {
      return ChronoUnit.DAYS.between(LocalDate.now(), nextNudgeDay)
   }

   fun nudgeIsOverdue(nextNudgeDay : LocalDate) : Boolean{
      return LocalDate.now().isBefore(nextNudgeDay).not()
   }

   fun nudgeListItemMessage(nextNudgeDay : LocalDate) : String {
      val daysBetween = daysUntilNudge(nextNudgeDay)
      daysBetween.let {
         return if(it < 0L)  "${if(it == -1L) "1 day" else "${-(it)} days"} late..."
         else if(nextNudgeDay.isEqual(LocalDate.now())) "Nudge! Message them today"
         else "${if(it == 1L) "1 day" else "$it days"} until next nudge"
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

   private fun randomContactPicture() : Int {
      return contactPictures.getResourceId((0..120).random(),-1)
   }

   private fun calcNudgeDate(lastMessageDate: LocalDate, nudgeInterval: Int?) : LocalDate? {
      return if(nudgeInterval == null) null
      else lastMessageDate.plusDays(nudgeInterval.toLong())
   }

}




