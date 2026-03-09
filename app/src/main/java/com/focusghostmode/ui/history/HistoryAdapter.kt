package com.focusghostmode.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusghostmode.R
import com.focusghostmode.data.model.FocusSession
import com.focusghostmode.utils.TimeUtils

class HistoryAdapter(
    private val onSessionClick: (FocusSession) -> Unit,
    private val onDeleteClick: (FocusSession) -> Unit
) : ListAdapter<FocusSession, HistoryAdapter.SessionViewHolder>(SessionDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session_card, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate: TextView = view.findViewById(R.id.tvSessionDate)
        private val tvDuration: TextView = view.findViewById(R.id.tvSessionDuration)
        private val tvNotifs: TextView = view.findViewById(R.id.tvSessionNotifs)
        private val tvCalls: TextView = view.findViewById(R.id.tvSessionCalls)
        private val tvMostActive: TextView = view.findViewById(R.id.tvMostActiveApp)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteSession)

        fun bind(session: FocusSession) {
            tvDate.text = TimeUtils.formatFull(session.startTime)
            tvDuration.text = session.durationFormatted()
            tvNotifs.text = "${session.notificationCount} notifications"
            tvCalls.text = "${session.callCount} calls"
            tvMostActive.text = if (session.mostActiveApp != null)
                "Most active: ${session.mostActiveApp}" else ""

            itemView.setOnClickListener { onSessionClick(session) }
            btnDelete.setOnClickListener { onDeleteClick(session) }
        }
    }

    class SessionDiff : DiffUtil.ItemCallback<FocusSession>() {
        override fun areItemsTheSame(o: FocusSession, n: FocusSession) = o.id == n.id
        override fun areContentsTheSame(o: FocusSession, n: FocusSession) = o == n
    }
}
