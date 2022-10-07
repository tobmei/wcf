package de.tob.wcf.db

import android.content.Context
import android.graphics.BitmapFactory
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import de.tob.wcf.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "input_table")
data class Input(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val x: Int,
    val y: Int,
    val pixels: List<Int>
)

@Entity(tableName = "output_table",
    foreignKeys = [ForeignKey(
        entity = Input::class,
        childColumns = ["inputId"],
        parentColumns = ["id"]
    )])
data class Output(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val inputId: Int,
    val x: Int,
    val y: Int,
    val pixels: List<Int>
)

@Dao
interface InputDao {
    @Query("SELECT * FROM input_table")
    fun getAll(): Flow<List<Input>>

    @Query("DELETE FROM input_table WHERE id=:id")
    fun delete(id: Int)

    @Insert
    suspend fun insert(input: Input)
}

@Dao
interface OutputDao {
    @Query("SELECT * FROM output_table")
    fun getAll(): Flow<List<Input>>

    @Query("SELECT * FROM output_table WHERE id=:id")
    fun getOutputsByInputId(id: Int): Flow<List<Output>>

    @Query("DELETE FROM output_table WHERE id=:id")
    fun delete(id: Int)

    @Insert
    suspend fun insert(output: Output)
}

@Database(entities = arrayOf(Input::class, Output::class), version = 1, exportSchema = false)
@TypeConverters(PixelsTypeConverter::class)
abstract class WCFDatabase : RoomDatabase() {

    abstract fun inputDao(): InputDao
    abstract fun outputDao(): OutputDao

    private class WCFDatabaseCallback(
        private val scope: CoroutineScope,
        private val context: Context
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    val inputDao = database.inputDao()
                    val bitmap1 = BitmapFactory.decodeResource(context.resources, R.drawable.test15)
                    val bitmap2 = BitmapFactory.decodeResource(context.resources, R.drawable.test12)
                    val bitmap3 = BitmapFactory.decodeResource(context.resources, R.drawable.test13)
                    val bitmap4 = BitmapFactory.decodeResource(context.resources, R.drawable.test18)
                    val bitmap5 = BitmapFactory.decodeResource(context.resources, R.drawable.test19)

                    listOf(bitmap1, bitmap2, bitmap3, bitmap4, bitmap5).forEach { bitmap ->
                        val pixels = IntArray(bitmap.height * bitmap.width)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        val input = Input(
                            x = bitmap.width,
                            y = bitmap.height,
                            pixels = pixels.toList()
                        )
                        inputDao.insert(input)
                    }

                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WCFDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): WCFDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WCFDatabase::class.java,
                    "wcf_database"
                )
                 .addCallback(WCFDatabaseCallback(scope, context))
                 .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

class PixelsTypeConverter {
    @TypeConverter
    fun savePixels(list: List<Int>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun getPixels(string: String): List<Int> {
        return string.split(",").map { it.toInt() }
    }
}