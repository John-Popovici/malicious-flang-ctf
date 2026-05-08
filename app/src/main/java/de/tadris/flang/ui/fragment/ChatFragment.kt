package de.tadris.flang.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network.CredentialsStorage
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.GameAttachment
import de.tadris.flang.network_api.model.Message
import de.tadris.flang.network_api.model.UserInfo
import de.tadris.flang.ui.adapter.MessageAdapter
import de.tadris.flang.ui.board.BoardView
import de.tadris.flang.ui.dialog.openImportDialog
import de.tadris.flang.ui.view.HorizontalSquareLayout
import de.tadris.flang.ui.view.addBottomPadding
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment(R.layout.fragment_chat), MessageAdapter.MessageAdapterListener {

    companion object {

        const val ARGUMENT_GAME_ID = "game_id"
        const val ARGUMENT_FMN = "fmn"

    }

    private val handler = Handler()

    private var fragmentPaused = true
    private var sending = false
    private var lastReceivedMessageTime = 0L

    lateinit var messageAdapter: MessageAdapter

    lateinit var recyclerView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager
    lateinit var input: EditText
    lateinit var sendButton: View

    var actionsHandled = false

    var resumeCount = 0

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageAdapter = MessageAdapter(CredentialsStorage(requireContext()).getUsername(), this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        root.addBottomPadding()

        fragmentPaused = false

        recyclerView = root.findViewById(R.id.messageRecyclerView)
        input = root.findViewById(R.id.chatInput)
        sendButton = root.findViewById(R.id.chatSend)

        sendButton.setOnClickListener {
            sendMessage()
        }

        input.addTextChangedListener {
            updateSendButtonEnabled()
        }

        recyclerView.adapter = messageAdapter
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        recyclerView.layoutManager = layoutManager

        updateSendButtonEnabled()

        return root
    }

    override fun onResume() {
        super.onResume()
        if(!actionsHandled){
            handleActions()
        }
        resumeCount++
        startUpdateThread()
    }

    override fun onPause() {
        resumeCount++
        super.onPause()
    }

    override fun onDestroyView() {
        fragmentPaused = true
        super.onDestroyView()
    }

    private fun handleActions(){
        arguments?.let { args ->
            val fmn = args.getString(ARGUMENT_FMN) ?: return
            val gameId = args.getString(ARGUMENT_GAME_ID)
            showSendGameDialog(gameId ?: fmn, Game.fromFMN(fmn))
        }
        actionsHandled = true
    }

    private fun startUpdateThread(){
        val oldResumeCount = resumeCount
        viewLifecycleOwner.lifecycleScope.launch {
            while (oldResumeCount == resumeCount){
                try {
                    val messages = fetchNewMessages()
                    val isAtBottom = recyclerIsAtBottom
                    messages.messages.forEach {
                        onNewMessage(it)
                    }
                    if (isAtBottom && messages.messages.isNotEmpty()) {
                        scrollDown()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                    delay(5000)
                }
                delay(500)
            }
        }
    }

    private fun onNewMessage(message: Message){
        checkDay(message.date)
        messageAdapter.appendMessage(message)
        lastReceivedMessageTime = message.date
    }

    private fun checkDay(nextTime: Long) {
        val lastDay = Calendar.getInstance()
        lastDay.timeInMillis = lastReceivedMessageTime

        val day = lastDay.get(Calendar.DAY_OF_YEAR)
        lastDay.timeInMillis = nextTime
        val newDay = lastDay.get(Calendar.DAY_OF_YEAR)
        if (day != newDay) {
            lastReceivedMessageTime = nextTime
            onNewMessage(Message(
                UserInfo(Message.SYSTEM_SENDER, -1f, false, ""),
                nextTime,
                SimpleDateFormat.getDateInstance().format(Date(nextTime)),
                null
            ))
        }
    }

    private fun sendMessage(){
        scrollDown()
        val text = input.text.toString()
        if(text.isEmpty())
            return
        val parsed = tryParseGameNotation(text)
        if(parsed != null){
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.sendAsTextOrBoard)
                .setMessage(R.string.sendAsTextOrBoardMessage)
                .setPositiveButton(R.string.sendAsBoard){ _, _ ->
                    input.setText("")
                    showSendGameDialog(text, parsed)
                }
                .setNegativeButton(R.string.sendAsText){ _, _ ->
                    doSendMessage(text)
                }
                .show()
            return
        }
        doSendMessage(text)
    }

    private fun doSendMessage(text: String){
        input.setText("")
        sending = true
        updateSendButtonEnabled()
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                sendMessage(text)
            }catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(requireContext(), getString(R.string.sendingFailed, e.localizedMessage), Toast.LENGTH_SHORT).show()
            } finally {
                sending = false
                updateSendButtonEnabled()
            }
        }
    }

    private fun tryParseGameNotation(text: String): Game? {
        val trimmed = text.trim()
        return try { Game.fromFMN(trimmed) } catch (_: Exception) {
            try { Game(initialBoard = Board.fromFBN(trimmed)) } catch (_: Exception) { null }
        }
    }

    private fun updateSendButtonEnabled(){
        sendButton.isEnabled = !sending && input.text.isNotEmpty()
    }

    private fun scrollDown(){
        if(messageAdapter.itemCount > 0){
            recyclerView.smoothScrollToPosition(0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_chat, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.actionSendGame){
            openImportDialog { gameString, board, _ ->
                showSendGameDialog(gameString, board)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun openAttachment(game: GameAttachment) {
        if(game.isOnlineGame){
            val bundle = Bundle()
            bundle.putLong(OnlineGameFragment.EXTRA_GAME_ID, game.id.toLong())
            findNavController().navigate(R.id.action_nav_chat_to_nav_game, bundle)
        }else{
            val bundle = Bundle()
            if(game.fmn.isNotEmpty()){
                bundle.putString(AbstractAnalysisGameFragment.ARGUMENT_BOARD_FMN, game.fmn)
            }else{
                bundle.putString(AbstractAnalysisGameFragment.ARGUMENT_BOARD_FBN, game.fbn)
            }
            findNavController().navigate(R.id.action_nav_chat_to_nav_analysis, bundle)
        }
    }

    override fun onUsernameClick(username: String) {
        val bundle = Bundle()
        bundle.putString(ProfileFragment.ARGUMENT_USERNAME, username)
        findNavController().navigate(R.id.action_nav_chat_to_nav_profile, bundle)
    }

    private fun showSendGameDialog(gameString: String, board: Game){
        val boardViewParent = HorizontalSquareLayout(context)
        boardViewParent.setBackgroundResource(R.drawable.ic_brown)
        val boardView = BoardView(boardViewParent, board.currentState, isClickable = false, animate = false)
        AlertDialog.Builder(requireActivity())
            .setView(boardViewParent)
            .setPositiveButton(R.string.actionSend){ _, _ ->
                sendGame(gameString)
            }
            .show()
        handler.postDelayed({
            boardView.refresh()
        }, 100)
    }

    private fun sendGame(gameString: String){
        scrollDown()
        lifecycleScope.launch {
            try{
                sendMessage("", gameString)
            }catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(requireContext(), getString(R.string.sendingFailed, e.localizedMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun sendMessage(text: String, attachedGame: String = "") = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessRestrictedAPI(requireContext()).sendMessage(
            Base64.encodeToString(text.encodeToByteArray(), Base64.URL_SAFE),
            Base64.encodeToString(attachedGame.encodeToByteArray(), Base64.URL_SAFE)
        )
    }

    private suspend fun fetchNewMessages() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessRestrictedAPI(requireContext()).getGlobalChatMessages(lastReceivedMessageTime)
    }

    private val recyclerIsAtBottom get() = layoutManager.findFirstVisibleItemPosition() == 0

}