package de.tadris.flang.ui.adapter

import android.graphics.Typeface
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.ChatMessages
import de.tadris.flang.network_api.model.GameAttachment
import de.tadris.flang.network_api.model.Message
import de.tadris.flang.ui.board.BoardView
import de.tadris.flang.util.applyTo
import de.tadris.flang.util.formatChatTextColor
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(val myUsername: String, val listener: MessageAdapterListener)
    : RecyclerView.Adapter<MessageAdapter.AbstractMessageViewHolder>() {

    val handler = Handler()

    companion object {

        private const val TYPE_SERVICE = 0
        private const val TYPE_MY_MESSAGE = 1
        private const val TYPE_OTHER_MESSAGE = 2

    }

    open class AbstractMessageViewHolder(val root: View) : RecyclerView.ViewHolder(root) {

        val textView = root.findViewById<TextView>(R.id.chatMessageText)!!

    }

    open class MyMessageViewHolder(root: View) : AbstractMessageViewHolder(root) {

        val dateView = root.findViewById<TextView>(R.id.chatMessageDate)!!
        val boardView = root.findViewById<FrameLayout>(R.id.chatMessageBoard)!!

    }

    class TextMessageViewHolder(root: View) : MyMessageViewHolder(root) {

        val senderTitleView = root.findViewById<TextView>(R.id.chatMessageSenderTitle)!!
        val senderUsernameView = root.findViewById<TextView>(R.id.chatMessageSenderUsername)!!

    }

    private val messages = mutableListOf<Message>()

    fun appendMessage(message: Message){
        messages.add(0, message)
        notifyItemInserted(0)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isSystemMessage -> TYPE_SERVICE
            message.sender.username == myUsername -> TYPE_MY_MESSAGE
            else -> TYPE_OTHER_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractMessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType){
            TYPE_SERVICE -> AbstractMessageViewHolder(inflater.inflate(R.layout.view_chat_service_message, parent, false))
            TYPE_MY_MESSAGE -> MyMessageViewHolder(inflater.inflate(R.layout.view_chat_my_message, parent, false))
            TYPE_OTHER_MESSAGE -> TextMessageViewHolder(inflater.inflate(R.layout.view_chat_message, parent, false))
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: AbstractMessageViewHolder, position: Int) {
        val context = holder.root.context
        val message = messages[position]
        holder.textView.text = message.text
        holder.textView.visibility =  View.VISIBLE
        holder.textView.typeface = Typeface.DEFAULT
        holder.root.setOnClickListener(null)
        if(holder is MyMessageViewHolder){
            holder.boardView.visibility = View.GONE
            holder.dateView.text = SimpleDateFormat.getTimeInstance().format(Date(message.date))
            message.game?.let { attachment ->
                try{
                    val board = attachment.game
                    holder.boardView.visibility = View.VISIBLE
                    holder.textView.visibility =  View.GONE
                    val boardView = BoardView(holder.boardView, board.currentState, isClickable = false, animate = false)
                    handler.postDelayed(100){
                        boardView.refresh()
                    }
                    holder.root.setOnClickListener {
                        listener.openAttachment(attachment)
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                    holder.textView.text = context.getString(R.string.cannotDecryptBoard)
                    holder.textView.typeface = Typeface.SERIF
                }
            }
        }
        if(holder is TextMessageViewHolder){
            message.sender.applyTo(holder.senderTitleView, holder.senderUsernameView)
            message.sender.formatChatTextColor(holder.senderUsernameView)
            
            // Make username clickable unless it's a system message
            if (!message.isSystemMessage) {
                holder.senderUsernameView.setOnClickListener {
                    listener.onUsernameClick(message.sender.username)
                }
            }
        }
    }

    override fun getItemCount() = messages.size

    interface MessageAdapterListener {

        fun openAttachment(game: GameAttachment)
        fun onUsernameClick(username: String)

    }

}