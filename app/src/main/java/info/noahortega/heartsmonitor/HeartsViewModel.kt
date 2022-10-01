package info.noahortega.heartsmonitor


import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.time.*
import kotlin.random.Random


data class EditScreenUiState(
   val contactId: Int? = null,
   var editContact: Contact
)

val defaultProfilePics = listOf(
   R.drawable.smiles000,
   R.drawable.smiles001,
   R.drawable.smiles002,
   R.drawable.smiles003,
   R.drawable.smiles004,
   R.drawable.smiles005,
)

class HeartsViewModel : ViewModel() {

   val blankContact = Contact(
      contactId = null,
      name = "",
      picture = defaultProfilePics.random(),
      lastMessageDate = null,
      isNudger = false,
      nudgeDayInterval = 0,
      nextNudgeDate = null
   )

   val currentEdit = mutableStateOf(blankContact)
   fun onRandomPicPress() {
      currentEdit.value.picture =  defaultProfilePics.random()
   }

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

   private fun contactContacted(contactId : Long) {
      //TODO: find contact, set contact date to now
   }



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



