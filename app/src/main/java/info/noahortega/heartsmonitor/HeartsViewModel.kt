package info.noahortega.heartsmonitor

import androidx.lifecycle.ViewModel
import java.time.*
import kotlin.random.Random


data class EditScreenUiState(
   var shouldExist: Boolean = false,
   val contactId: Int? = null,
   val pictureId: Int = R.drawable.smiles000,
   val contactName: String = "",
   val isNudger: Boolean = false,
   val nudgeDayInterval: Int? = null
)

class HeartsViewModel : ViewModel() {

   val defaultProfilePics = listOf(
      R.drawable.smiles000,
      R.drawable.smiles001,
      R.drawable.smiles002,
      R.drawable.smiles003,
      R.drawable.smiles004,
      R.drawable.smiles005,
   )

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
               contactId = (-i).toLong(),
               name = "Test Contact $i",
               picture = defaultProfilePics.random(),
               lastMessageDate = randomTimeBetween(
                  LocalDateTime.of(2022, 8, 15, 0, 0, 0)
                  ,LocalDateTime.now()),
               isNudger = true,
               nudgeDayInterval = 0,
               nextNudgeDate = randomTimeBetween(
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
