package info.noahortega.heartsmonitor.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import info.noahortega.heartsmonitor.room.entities.Contact
import info.noahortega.heartsmonitor.room.entities.ContactDao
import java.time.LocalDate

@Database(entities = [Contact::class], version = 1)
@TypeConverters(LocalDateConverter::class)
abstract class AppDatabase : RoomDatabase() {
   abstract fun contactDao(): ContactDao
}

class LocalDateConverter {
   @TypeConverter
   fun toDate(dateString: String?): LocalDate? {
      return dateString?.let {
         LocalDate.parse(it)
      }
   }

   @TypeConverter
   fun toDateString(date: LocalDate?): String? {
      return date?.toString()
   }
}