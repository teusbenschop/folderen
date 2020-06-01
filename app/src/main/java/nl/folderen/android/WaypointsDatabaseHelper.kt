package nl.folderen.android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.google.android.gms.maps.model.LatLng


class WaypointsDatabaseHelper (context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION),
    // By implementing the BaseColumns interface,
    // the class can inherit a primary key field called _ID
    // that some Android classes such as CursorAdapter expect to have.
    BaseColumns
{
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "waypoints.db"
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
    object WaypointsContract {

        object WaypointEntry : BaseColumns {
            const val TABLE_NAME = "waypoints"
            const val COLUMN_NAME_LATITUDE = "latitude"
            const val COLUMN_NAME_lONGITUDE = "longitude"
        }

        const val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${WaypointEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${WaypointEntry.COLUMN_NAME_LATITUDE} DOUBLE," +
                    "${WaypointEntry.COLUMN_NAME_lONGITUDE} DOUBLE)"

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${WaypointEntry.TABLE_NAME}"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        if (db != null) {
            db.execSQL(WaypointsContract.SQL_CREATE_ENTRIES)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // The upgrade policy is to simply to discard the data and start over.
        if (db != null) {
            db.execSQL(WaypointsContract.SQL_DELETE_ENTRIES)
        }
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun savePoint (latlng: LatLng)
    {
        // Get the data repository in write mode.
        // This is an expensive call.
        // Todo Another option would be to leave the database open for the duration of recording the waypoints.
        val db = this.writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            put(WaypointsContract.WaypointEntry.COLUMN_NAME_LATITUDE, latlng.latitude)
            put(WaypointsContract.WaypointEntry.COLUMN_NAME_lONGITUDE, latlng.longitude)
        }

        // Insert the new row, returning the primary key value of the new row
        db?.insert(WaypointsContract.WaypointEntry.TABLE_NAME, null, values)

        // Close the database connection.
        this.close()
    }


    fun getPoints () : List<LatLng>
    {
        // Storage for the waypoints that are going to be read from the database.
        var points : MutableList<LatLng> = mutableListOf<LatLng>()

        // Get a connection to the database in read mode.
        val db = this.readableDatabase

        val cursor = db.query (
            WaypointsContract.WaypointEntry.TABLE_NAME, // The table to query.
            null, // The columns to return, or null to get them all.
            null, // The columns for the WHERE clause.
            null, // The values for the WHERE clause.
            null, // Grouping.
            null, // Filtering by row groups.
            null // Ordering clause.
        )

        while (cursor.moveToNext()) {

            val latitude = cursor.getDouble(cursor.getColumnIndex(WaypointsContract.WaypointEntry.COLUMN_NAME_LATITUDE))
            val longitude = cursor.getDouble(cursor.getColumnIndex(WaypointsContract.WaypointEntry.COLUMN_NAME_lONGITUDE))
            val point = LatLng (latitude, longitude)
            points.add(point)
        }

        // Close the database connection.
        this.close()

        // Hand the points to the caller.
        return points
    }

}
