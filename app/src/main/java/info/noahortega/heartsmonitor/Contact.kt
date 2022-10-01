package info.noahortega.heartsmonitor

import java.time.LocalDateTime

data class Contact(
   val contactId: Long?,
   var name: String = "Test Contact",
   var picture: Int = R.drawable.smiles000,
   var lastMessageDate: LocalDateTime?,
   var isNudger: Boolean,
   var nudgeDayInterval: Int,
   var nextNudgeDate: LocalDateTime? = LocalDateTime.now(),
)
