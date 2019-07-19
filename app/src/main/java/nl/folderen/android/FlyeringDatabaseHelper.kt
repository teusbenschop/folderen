package nl.folderen.android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.google.android.gms.maps.model.LatLng


// The database operations were written based on the following official documentation:
// https://developer.android.com/training/data-storage/sqlite

// The app uses SQLite directly, rather than using the Rooms persistence methods.
// The reason for this is that the Room methods are fairly expensive.
// See the Room documentation for this information, and more:
// https://developer.android.com/training/data-storage/room


class FlyeringDatabaseHelper (context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION),
    // By implementing the BaseColumns interface,
    // the class can inherit a primary key field called _ID
    // that some Android classes such as CursorAdapter expect to have.
    BaseColumns
{

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "FlyeringAreasDone.db"
    }

    // One of the main principles of SQL databases is the schema:
    // a formal declaration of how the database is organized.
    // The schema is reflected in the SQL statements that you use to create your database.
    // You may find it helpful to create a companion class,
    // known as a contract class,
    // which explicitly specifies the layout of your schema in a systematic and self-documenting way.
    // A contract class is a container for constants that define names
    // for URIs, tables, and columns.
    // The contract class allows you to use the same constants across all the other classes in the same package.
    // This lets you change a column name in one place and have it propagate throughout your code.
    object FlyeringContract {

        object AreaEntry : BaseColumns {
            const val TABLE_NAME = "areas"
            const val COLUMN_NAME_LATITUDE0 = "latitude0"
            const val COLUMN_NAME_lONGITUDE0 = "longitude0"
            const val COLUMN_NAME_LATITUDE1 = "latitude1"
            const val COLUMN_NAME_lONGITUDE1 = "longitude1"
            const val COLUMN_NAME_LATITUDE2 = "latitude2"
            const val COLUMN_NAME_lONGITUDE2 = "longitude2"
            const val COLUMN_NAME_LATITUDE3 = "latitude3"
            const val COLUMN_NAME_lONGITUDE3 = "longitude3"
            const val COLUMN_NAME_LATITUDE4 = "latitude4"
            const val COLUMN_NAME_lONGITUDE4 = "longitude4"
            const val COLUMN_NAME_LATITUDE5 = "latitude5"
            const val COLUMN_NAME_lONGITUDE5 = "longitude5"
            const val COLUMN_NAME_LATITUDE6 = "latitude6"
            const val COLUMN_NAME_lONGITUDE6 = "longitude6"
            const val COLUMN_NAME_LATITUDE7 = "latitude7"
            const val COLUMN_NAME_lONGITUDE7 = "longitude7"
        }

        const val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${AreaEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${AreaEntry.COLUMN_NAME_LATITUDE0} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_lONGITUDE0} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_LATITUDE1} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_lONGITUDE1} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_LATITUDE2} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_lONGITUDE2} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_LATITUDE3} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_lONGITUDE3} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_LATITUDE4} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_lONGITUDE4} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_LATITUDE5} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_lONGITUDE5} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_LATITUDE6} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_lONGITUDE6} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_LATITUDE7} DOUBLE," +
                    "${AreaEntry.COLUMN_NAME_lONGITUDE7} DOUBLE)"

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${AreaEntry.TABLE_NAME}"
    }


    override fun onCreate(db: SQLiteDatabase)
    {
        db.execSQL(FlyeringContract.SQL_CREATE_ENTRIES)
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    {
        // The upgrade policy is to simply to discard the data and start over.
        db.execSQL(FlyeringContract.SQL_DELETE_ENTRIES)
        onCreate(db)
    }


    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }


    fun saveArea (points : List<LatLng>)
    {
        // Get the data repository in write mode.
        // This is an expensive call.
        // But anyway, it does not happen too often.
        // Another option would be to leave the database open for the duration of the lifetime of the app.
        val db = this.writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            put(FlyeringContract.AreaEntry.COLUMN_NAME_LATITUDE0, points[0].latitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_lONGITUDE0, points[0].longitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_LATITUDE1, points[1].latitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_lONGITUDE1, points[1].longitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_LATITUDE2, points[2].latitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_lONGITUDE2, points[2].longitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_LATITUDE3, points[3].latitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_lONGITUDE3, points[3].longitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_LATITUDE4, points[4].latitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_lONGITUDE4, points[4].longitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_LATITUDE5, points[5].latitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_lONGITUDE5, points[5].longitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_LATITUDE6, points[6].latitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_lONGITUDE6, points[6].longitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_LATITUDE7, points[7].latitude)
            put(FlyeringContract.AreaEntry.COLUMN_NAME_lONGITUDE7, points[7].longitude)
        }

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db?.insert(FlyeringContract.AreaEntry.TABLE_NAME, null, values)

        // Close the database connection.
        this.close()
    }


}
