package com.example.canstone2

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.ImageView
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date


class NotificationFragment : Fragment() {

    data class NotificationItem(
        val category : String,
        val type : String,
        val timestamp : Date
    )
    sealed class NotificationDisplayItem {
        data class DateHeader(val date: String) : NotificationDisplayItem()
        data class NotificationData(val item: NotificationItem) : NotificationDisplayItem()
    }
    private val adapter = NotificationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        //val backButton = view.findViewById<ImageView>(R.id.ic_back)

        val db = FirebaseFirestore.getInstance()

        db.collection("alarm_data")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                val notificationList = result.mapNotNull { doc ->
                    val category = doc.getString("category")
                    val type = doc.getString("type")
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()

                    if (category == null || type == null || timestamp == null) {
                        null
                    } else {
                        NotificationItem(category, type, timestamp)
                    }
                }
                val displayList = buildDisplayList(notificationList)
                adapter.submitList(displayList)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "데이터 가져오기 실패", e)
            }
        // TODO :
            /*
         val notifications = listOf(
            NotificationItem("detect", "accept", "2024-05-26T15:10:03Z"),
            NotificationItem("call", "accept", "2024-05-26T15:10:03Z"),
            NotificationItem("call", "112", "2024-05-26T15:10:03Z")
        )
        val testList = buildDisplayList(notifications)
        adapter.submitList(testList)*/
        /////////// TODO /////////////
        return view
    }
    private fun buildDisplayList(items: List<NotificationItem>): List<NotificationDisplayItem> {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val sdfOutput = SimpleDateFormat("yyyy. MM. dd(E)", Locale.KOREAN)

        val result = mutableListOf<NotificationDisplayItem>()
        var lastDate: String? = null

        for (item in items) {
            val dateStr = sdfOutput.format(item.timestamp)

            if (dateStr != lastDate) {
                result.add(NotificationDisplayItem.DateHeader(dateStr))
                lastDate = dateStr
            }

            result.add(NotificationDisplayItem.NotificationData(item))
        }

        return result
    }
    class NotificationAdapter:
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val VIEW_TYPE_DATE = 0
            private const val VIEW_TYPE_ITEM = 1
        }

        private var items: List<NotificationDisplayItem> = emptyList()

        fun submitList(list: List<NotificationDisplayItem>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is NotificationDisplayItem.DateHeader -> VIEW_TYPE_DATE
                is NotificationDisplayItem.NotificationData -> VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_DATE -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_notification_date, parent, false)
                    DateViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_notification, parent, false)
                    NotificationViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            when (item) {
                is NotificationDisplayItem.DateHeader -> (holder as DateViewHolder).bind(item.date)
                is NotificationDisplayItem.NotificationData -> (holder as NotificationViewHolder).bind(item.item)
            }
        }
        override fun getItemCount() = items.size

        inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val dateText: TextView = view.findViewById(R.id.dateTextView)
            fun bind(date: String) {
                dateText.text = date
            }
        }

        inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val icon: ImageView = view.findViewById(R.id.notificationIcon)
            private val title: TextView = view.findViewById(R.id.notificationTitle)
            private val detail: TextView = view.findViewById(R.id.notificationDetail)
            private val time: TextView = view.findViewById(R.id.notificationTime)

            fun bind(item: NotificationItem) {
                val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault()).format(item.timestamp)
                time.text = timeOnly

                when (item.category) {
                    "detect" -> {
                        icon.setImageResource(R.drawable.ic_notification_detect)
                        title.text = "급발진 감지"
                        detail.visibility = View.GONE
                    }
                    "call" -> {
                        when (item.type) {
                            "accept", "deny" -> {
                                icon.setImageResource(R.drawable.ic_notificaiton_call)
                                title.text = "신고 전화 의사 확인"
                                detail.text = "승인"

                                if (item.type == "deny") {
                                    detail.text = "거부"
                                    detail.setTextColor(Color.RED)
                                }
                            }
                            else -> {
                                icon.setImageResource(R.drawable.ic_notificaiton_callnum)
                                title.text = "신고 전화 발신"
                                detail.text = item.type
                            }
                        }
                    }
                }
            }
        }
    }
}
