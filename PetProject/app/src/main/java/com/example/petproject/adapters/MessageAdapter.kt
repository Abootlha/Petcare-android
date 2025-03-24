package com.example.petproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petproject.R
import com.example.petproject.models.Message

class MessageAdapter(
    private var messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    abstract class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(message: Message)
    }

    class SentMessageViewHolder(itemView: View) : MessageViewHolder(itemView) {
        private val textViewMessage: TextView = itemView.findViewById(R.id.textViewSentMessage)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewSentTime)

        override fun bind(message: Message) {
            textViewMessage.text = message.content
            // Format timestamp
            textViewTime.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : MessageViewHolder(itemView) {
        private val textViewMessage: TextView = itemView.findViewById(R.id.textViewReceivedMessage)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewReceivedTime)

        override fun bind(message: Message) {
            textViewMessage.text = message.content
            // Format timestamp
            textViewTime.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    fun updateMessages(newMessages: List<Message>) {
        this.messages = newMessages
        notifyDataSetChanged()
    }
}