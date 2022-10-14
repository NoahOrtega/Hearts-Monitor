package info.noahortega.heartsmonitor.room.entities

import androidx.room.*
import info.noahortega.heartsmonitor.R
import java.time.LocalDateTime

@Entity
data class Contact(
   @PrimaryKey(autoGenerate = true) val contactId: Long = 0,
   @ColumnInfo val name: String = "Test Contact",
   @ColumnInfo val picture: Int = R.drawable.smiles000,
   @ColumnInfo val lastMessageDate: LocalDateTime = LocalDateTime.now(),
   @ColumnInfo val isNudger: Boolean = false,
   @ColumnInfo val nudgeDayInterval: Int? = null,
   @ColumnInfo val nextNudgeDate: LocalDateTime? = null,
)

@Dao
interface ContactDao {
   @Query("SELECT * FROM contact")
   fun getAll(): List<Contact>

   @Insert()
   fun insert(contact: Contact) : Long


   @Query("DELETE FROM contact WHERE contactId = :contactId")
   fun deleteById(contactId: Long)

   @Delete
   fun delete(contact: Contact)

   @Update
   fun update(contact: Contact)
}