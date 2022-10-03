package info.noahortega.heartsmonitor


import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.time.*
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
   //Contacts Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   fun onFabPressed() {
      setEditState(null)
   }
   fun onContactPressed(contact: Contact) {
      setEditState(contact)
   }


   //Edit Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   data class EditUIState(
      val name: MutableState<String> = mutableStateOf(""),
      val imgId: MutableState<Int> = mutableStateOf(0),
      val isNudger: MutableState<Boolean>  = mutableStateOf(false),
      val nudgeDayInterval: MutableState<String?> = mutableStateOf(null),
   )
   val myEditState : EditUIState = EditUIState()
   fun setEditState(contact: Contact?) {
      if(contact == null) {
         myEditState.apply {
            this.name.value = ""
            this.imgId.value = defaultProfilePics.random()
            this.isNudger.value = false
            this.nudgeDayInterval.value = ""
         }
      }
      else {
         myEditState.apply {
            this.name.value = contact.name
            this.imgId.value = contact.picture
            this.isNudger.value = contact.isNudger
            this.nudgeDayInterval.value = contact.nudgeDayInterval.toString()
         }
      }
   }
   fun onRandomPicPress() {
      myEditState.imgId.value = defaultProfilePics.filter { it != myEditState.imgId.value}.random()
   }
   fun tryToChangeInterval(interval: String) {
      if(interval.toIntOrNull() != null || interval == "") myEditState.nudgeDayInterval.value = interval
   }

   //Suggest Screen State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   val suggestedContact = mutableStateOf(newRandomContact() as Contact?)
   fun newSuggestionPressed() {
      suggestedContact.value = newRandomContact()
   }
   fun contactSuggestionPressed() {
      suggestedContact.value = newRandomContact()
   }
   private fun newRandomContact() : Contact?{
      return dummyContacts(4).random()
   }

   //Test Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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



