package com.basic.petproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.R
import com.basic.petproject.models.Conversation
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private var conversations: List<Conversation>,
    private val onItemClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    class ConversationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivUserProfile: ImageView = view.findViewById(R.id.imageViewUserProfile)
        val tvUserName: TextView = view.findViewById(R.id.textViewUserName)
        val tvLastMessage: TextView = view.findViewById(R.id.textViewLastMessage)
        val tvTimestamp: TextView = view.findViewById(R.id.textViewTimestamp)
        val tvUnreadCount: TextView = view.findViewById(R.id.textViewUnreadCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]
        
        // Set user name
        holder.tvUserName.text = conversation.otherUserName
        
        // Set last message
        val lastMessage = if (conversation.lastMessage.length > 30) {
            "${conversation.lastMessage.substring(0, 27)}..."
        } else {
            conversation.lastMessage
        }
        holder.tvLastMessage.text = lastMessage
        
        // Set timestamp
        holder.tvTimestamp.text = formatTimestamp(conversation.timestamp)
        
        // Set unread count
        if (conversation.unreadCount > 0) {
            holder.tvUnreadCount.visibility = View.VISIBLE
            holder.tvUnreadCount.text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString()
        } else {
            holder.tvUnreadCount.visibility = View.GONE
        }
        
        // Load user profile image - rewritten to fix the "into" issue
        if (conversation.otherUserProfilePic.isNotEmpty()) {
            try {
                val picasso = Picasso.get()
                picasso.load(conversation.otherUserProfilePic)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(holder.ivUserProfile)
            } catch (e: Exception) {
                // Fallback to placeholder if Picasso fails
                holder.ivUserProfile.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else {
            holder.ivUserProfile.setImageResource(R.drawable.ic_profile_placeholder)
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(conversation)
        }
    }

    override fun getItemCount(): Int = conversations.size

    fun updateConversations(newConversations: List<Conversation>) {
        this.conversations = newConversations
        notifyDataSetChanged()
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        
        // If message is from today, show time (e.g., "3:30 PM")
        // If message is older, show date (e.g., "Mar 15")
        val dayDiff = (now - timestamp) / (24 * 60 * 60 * 1000)
        
        return when {
            dayDiff < 1 -> formatter.format(date)
            dayDiff < 7 -> {
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
                dayOfWeek
            }
            else -> {
                val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
                dateFormatter.format(date)
            }
        }
    }
} 