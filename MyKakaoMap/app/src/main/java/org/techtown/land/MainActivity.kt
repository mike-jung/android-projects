package org.techtown.land

import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListAdapter
import android.widget.ListView
import com.pedro.library.AutoPermissions


class MainActivity : ListActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val adapter: ListAdapter = DemoListAdapter(this, DemoListItem.DEMO_LIST_ITEMS)
        listAdapter = adapter

        AutoPermissions.Companion.loadAllPermissions(this, 101)
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val demo = listAdapter.getItem(position) as DemoListItem
        startActivity(Intent(this, demo.activity))
    }

}