package com.basic.petproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.R
import com.basic.petproject.models.Message
import com.basic.petproject.models.MessageStatus
import com.basic.petproject.models.MessageType
import com.squareup.picasso.Picasso

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
        private val statusIcon: ImageView = itemView.findViewById(R.id.messageStatusIcon)
        private val imageViewMessage: ImageView = itemView.findViewById(R.id.imageViewSentImage)

        override fun bind(message: Message) {
            // Set message content based on type
            if (message.type == MessageType.TEXT) {
                textViewMessage.visibility = View.VISIBLE
                imageViewMessage.visibility = View.GONE
                textViewMessage.text = message.content
            } else {
                textViewMessage.visibility = View.GONE
                imageViewMessage.visibility = View.VISIBLE
                Picasso.get().load(message.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.placeholder_pet)
                    .into(imageViewMessage)
            }

            // Format timestamp
            textViewTime.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
            
            // Set message status indicator
            when (message.status) {
                MessageStatus.SENDING -> statusIcon.setImageResource(R.drawable.ic_message_sending)
                MessageStatus.SENT -> statusIcon.setImageResource(R.drawable.ic_message_sent)
                MessageStatus.DELIVERED -> statusIcon.setImageResource(R.drawable.ic_message_delivered)
                MessageStatus.READ -> statusIcon.setImageResource(R.drawable.ic_message_read)
            }
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : MessageViewHolder(itemView) {
        private val textViewMessage: TextView = itemView.findViewById(R.id.textViewReceivedMessage)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewReceivedTime)
        private val imageViewMessage: ImageView = itemView.findViewById(R.id.imageViewReceivedImage)

        override fun bind(message: Message) {
            // Set message content based on type
            if (message.type == MessageType.TEXT) {
                textViewMessage.visibility = View.VISIBLE
                imageViewMessage.visibility = View.GONE
                textViewMessage.text = message.content
            } else {
                textViewMessage.visibility = View.GONE
                imageViewMessage.visibility = View.VISIBLE
                Picasso.get().load(message.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.placeholder_pet)
                    .into(imageViewMessage)
            }

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