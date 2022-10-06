package info.noahortega.heartsmonitor

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
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
   //General data state functions
   private fun addContact(contact : Contact) {
      contactList.add(contact)

      //todo: add to database
   }

   //Contacts Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   val contactList = mutableStateListOf<Contact>()

   fun onFabPressed() {
      myEditState.setState(null)
   }
   fun onContactPressed(contact: Contact) {
      myEditState.setState(contact)
   }
   //Contacts Screen Helpers ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   fun contactListItemMessage(lastContacted: LocalDateTime) : String {
      val curTime = LocalDateTime.now()
      lastContacted.until(curTime, ChronoUnit.HOURS).run {
         if (this < 24) return "Just Contacted!"
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
      //TODO: Fully validate
      if(myEditState.name != "") {
         if (!myEditState.isNudger || myEditState.nudgeDayInterval?.toIntOrNull() != null) {
            val nudgeDayInterval: Int? = myEditState.nudgeDayInterval?.toIntOrNull()
            val newContact = Contact(
               name = myEditState.name,
               picture = myEditState.imgId,
               lastMessageDate = LocalDateTime.now(),
               isNudger = myEditState.isNudger,
               nudgeDayInterval = nudgeDayInterval,
               nextNudgeDate = nudgeDayInterval?.let {LocalDateTime.now().plusDays(it.toLong())}
            )
            println(newContact)
            addContact(newContact)
         }
      }
      else {
         //TODO: error message
      }
   }



   //Suggest Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   var suggestedContact by mutableStateOf(newRandomContact() as Contact?)
   fun newSuggestionPressed() {
      suggestedContact = newRandomContact()
   }
   fun contactSuggestionPressed() {
      suggestedContact = newRandomContact()
   }
   private fun newRandomContact() : Contact?{
      return dummyContacts(4).random()
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




