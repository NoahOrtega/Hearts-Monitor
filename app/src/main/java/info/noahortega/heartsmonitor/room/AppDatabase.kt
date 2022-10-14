package info.noahortega.heartsmonitor.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import info.noahortega.heartsmonitor.room.entities.Contact
import info.noahortega.heartsmonitor.room.entities.ContactDao
import java.time.LocalDateTime

@Database(entities = [Contact::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
   abstract fun contactDao(): ContactDao
}

class LocalDateTimeConverter {
   @TypeConverter
   fun toDate(dateString: String?): LocalDateTime? {
      return dateString?.let {
         LocalDateTime.parse(it)
      }
   }

   @TypeConverter
   fun toDateString(date: LocalDateTime?): String? {
      return date?.toString()
   }
}