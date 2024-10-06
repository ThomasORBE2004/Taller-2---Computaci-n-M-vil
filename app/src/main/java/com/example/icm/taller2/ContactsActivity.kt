package com.example.icm.taller2

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ContactsActivity : AppCompatActivity() {

    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        mlista = findViewById<ListView>(R.id.contactsList)
        mProjection =
            arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
        mContactsAdapter = ContactsAdapter(this, null, 0)
        mlista?.adapter = mContactsAdapter

        initView()
    }

    fun initView() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            mCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
            )
            mContactsAdapter?.changeCursor(mCursor)
        }
    }


}
