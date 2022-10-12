package info.noahortega.heartsmonitor

import java.time.LocalDateTime

data class Contact(
   val contactId: Long =  -1,
   val name: String = "Test Contact",
   val picture: Int = R.drawable.smiles000,
   val lastMessageDate: LocalDateTime,
   val isNudger: Boolean,
   val nudgeDayInterval: Int?,
   val nextNudgeDate: LocalDateTime? = null
)
