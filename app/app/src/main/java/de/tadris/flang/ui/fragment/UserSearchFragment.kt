package de.tadris.flang.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.UserInfo
import de.tadris.flang.ui.adapter.UserAdapter
import de.tadris.flang.ui.view.addBottomPadding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class UserSearchFragment : Fragment(R.layout.fragment_user_search), UserAdapter.UserAdapterListener {

    private lateinit var searchEditText: EditText
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    
    private val userAdapter = UserAdapter(this)
    private var searchJob: Job? = null
    
    private companion object {
        const val SEARCH_DELAY_MS = 300L
        const val MIN_SEARCH_LENGTH = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        
        root.addBottomPadding()
        
        searchEditText = root.findViewById(R.id.searchEditText)
        searchRecyclerView = root.findViewById(R.id.searchRecyclerView)
        progressBar = root.findViewById(R.id.progressBar)
        emptyStateText = root.findViewById(R.id.emptyStateText)
        
        setupRecyclerView()
        setupSearch()
        updateActionBarTitle()
        
        return root
    }
    
    private fun setupRecyclerView() {
        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchRecyclerView.adapter = userAdapter
    }
    
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                handleSearchQuery(query)
            }
        })
    }
    
    private fun handleSearchQuery(query: String) {
        searchJob?.cancel()
        
        if (query.length < MIN_SEARCH_LENGTH) {
            showEmptyState(getString(R.string.userSearchHint))
            userAdapter.updateList(de.tadris.flang.network_api.model.UserResult(emptyList()))
            return
        }
        
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(SEARCH_DELAY_MS)
            performSearch(query)
        }
    }
    
    private suspend fun performSearch(query: String) {
        try {
            showLoading(true)
            val result = searchUsers(query)
            
            if (result.users.isEmpty()) {
                showEmptyState(getString(R.string.noUsersFound))
            } else {
                showResults()
            }
            
            userAdapter.updateList(result)
        } catch (e: CancellationException) {
            // Don't show error toast for cancellation - this is expected behavior
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            showEmptyState(getString(R.string.searchError))
        } finally {
            showLoading(false)
        }
    }
    
    @WorkerThread
    private suspend fun searchUsers(query: String) = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessOpenAPI().search(query)
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            searchRecyclerView.visibility = View.GONE
            emptyStateText.visibility = View.GONE
        }
        // Don't change other views when loading finishes - let showResults/showEmptyState handle it
    }
    
    private fun showResults() {
        progressBar.visibility = View.GONE
        searchRecyclerView.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE
    }
    
    private fun showEmptyState(message: String) {
        progressBar.visibility = View.GONE
        searchRecyclerView.visibility = View.GONE
        emptyStateText.visibility = View.VISIBLE
        emptyStateText.text = message
    }
    
    private fun updateActionBarTitle() {
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.searchUsers)
    }
    
    override fun onClick(user: UserInfo) {
        showProfile(user.username)
    }
    
    private fun showProfile(username: String) {
        val bundle = Bundle()
        bundle.putString(ProfileFragment.ARGUMENT_USERNAME, username)
        findNavController().navigate(R.id.action_nav_user_search_to_nav_profile, bundle)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }
}