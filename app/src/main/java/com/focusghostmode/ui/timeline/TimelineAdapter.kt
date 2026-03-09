package com.focusghostmode.ui.timeline

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusghostmode.R
import com.focusghostmode.data.model.CapturedEvent
import com.focusghostmode.utils.TimeUtils

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_EVENT = 1

class TimelineAdapter : ListAdapter<TimelineItem, RecyclerView.ViewHolder>(TimelineDiff()) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TimelineItem.Header -> VIEW_TYPE_HEADER
        is TimelineItem.Event -> VIEW_TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_timeline_header, parent, false)
            )
            else -> EventViewHolder(
                inflater.inflate(R.layout.item_timeline_event, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TimelineItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TimelineItem.Event -> (holder as EventViewHolder).bind(item.event)
        }
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvTimeHeader)
        fun bind(header: TimelineItem.Header) {
            tvHeader.text = header.label
        }
    }

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvEventSubtitle)
        private val tvTime: TextView = view.findViewById(R.id.tvEventTime)
        private val viewDot: View = view.findViewById(R.id.viewTimelineDot)

        fun bind(event: CapturedEvent) {
            tvTime.text = TimeUtils.formatTime(event.timestamp)

            when (event.eventType) {
                CapturedEvent.EventType.NOTIFICATION -> {
                    tvTitle.text = event.appName ?: event.appPackage ?: "Unknown App"
                    tvSubtitle.text = buildString {
                        if (!event.notifTitle.isNullOrBlank()) append(event.notifTitle)
                        if (!event.notifText.isNullOrBlank() && event.notifText != event.notifTitle) {
                            if (isNotEmpty()) append("\n")
                            append(event.notifText.take(80))
                        }
                    }
                    // Load app icon
                    val icon = getAppIcon(event.appPackage)
                    if (icon != null) {
                        ivIcon.setImageDrawable(icon)
                    } else {
                        ivIcon.setImageResource(R.drawable.ic_notification)
                    }
                    viewDot.setBackgroundResource(R.drawable.dot_notification)
                }
                CapturedEvent.EventType.CALL -> {
                    val caller = event.callerName?.takeIf { it.isNotBlank() }
                        ?: event.callerNumber ?: "Unknown"
                    tvTitle.text = when (event.callType) {
                        CapturedEvent.CallType.MISSED -> "Missed Call"
                        CapturedEvent.CallType.INCOMING -> "Incoming Call"
                        CapturedEvent.CallType.REJECTED -> "Rejected Call"
                        null -> "Call"
                    }
                    tvSubtitle.text = caller
                    ivIcon.setImageResource(when (event.callType) {
                        CapturedEvent.CallType.MISSED -> R.drawable.ic_call_missed
                        else -> R.drawable.ic_call_incoming
                    })
                    viewDot.setBackgroundResource(R.drawable.dot_call)
                }
            }

            // Fade-in animation
            itemView.alpha = 0f
            itemView.animate().alpha(1f).setDuration(300).start()
        }

        private fun getAppIcon(packageName: String?): Drawable? {
            if (packageName == null) return null
            return try {
                itemView.context.packageManager.getApplicationIcon(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    class TimelineDiff : DiffUtil.ItemCallback<TimelineItem>() {
        override fun areItemsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean =
            oldItem == newItem
        override fun areContentsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean =
            oldItem == newItem
    }
}
