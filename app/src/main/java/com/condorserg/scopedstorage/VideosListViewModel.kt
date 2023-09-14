package com.condorserg.scopedstorage

import android.app.RecoverableSecurityException
import android.app.RemoteAction
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideosListViewModel : ViewModel() {
    private val repository = VideoRepository()
    private val videosMutableLiveData = MutableLiveData<List<Video>>()
    private val recoverableActionMutableLiveData = MutableLiveData<RemoteAction>()
    private val saveFileStateMutableLiveData = MutableLiveData<Boolean>()
    private val isLoadingLiveData = MutableLiveData(false)


    private var isObservingStarted = false
    private var pendingDeleteId: Long? = null

    val videosLiveData: LiveData<List<Video>>
        get() = videosMutableLiveData

    private val showToastLiveData = SingleLiveEvent<String>()

    val showToast: LiveData<String>
        get() = showToastLiveData

    val recoverableActionLiveData: LiveData<RemoteAction>
        get() = recoverableActionMutableLiveData

    val isLoading: LiveData<Boolean>
        get() = isLoadingLiveData

    val saveFileState: MutableLiveData<Boolean>
        get() = saveFileStateMutableLiveData

    fun loadList() {
        viewModelScope.launch {
            try {
                videosMutableLiveData.postValue(repository.getVideos())
            } catch (t: Throwable) {
                Log.e("VideosListViewModel", "Load List error", t)
                videosMutableLiveData.postValue(emptyList())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.unregObserver()
    }

    fun updatePermissionState(isGranted: Boolean) {
        if (isGranted) {
            permissionsGranted()
        } else {
            permissionsDenied()
        }
    }

    fun permissionsGranted() {
        loadList()
        if (isObservingStarted.not()) {
            repository.observeVideos { loadList() }
            isObservingStarted = true
        }
    }

    fun permissionsDenied() {
        videosMutableLiveData.postValue(emptyList())
    }

    fun saveVideo(url: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isLoadingLiveData.postValue(true)
                repository.saveVideo(url, name)
                showToastLiveData.postValue(App.appContext.getString(R.string.download_successful))
                saveFileStateMutableLiveData.postValue(true)
            } catch (t: Throwable) {
                showToastLiveData.postValue(
                    App.appContext.getString(
                        R.string.download_error,
                        t.message
                    )
                )
                Log.e("Downloading Error", "Downloading error = ${t.message}")
                saveFileStateMutableLiveData.postValue(false)
            } finally {
                isLoadingLiveData.postValue(false)
            }
        }
    }

    fun deleteVideo(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteVideo(id)
                pendingDeleteId = null
                showToastLiveData.postValue(App.appContext.getString(R.string.delete_success))
            } catch (t: Throwable) {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && (t is RecoverableSecurityException)) {
                    pendingDeleteId = id
                    recoverableActionMutableLiveData.postValue(t.userAction)
                } else {
                    showToastLiveData.postValue(
                        App.appContext.getString(
                            R.string.delete_error,
                            t.message
                        )
                    )
                    Log.e("Delete Error", "Delete error = ${t.message}")
                }
            }
        }
    }

    fun confirmDelete() {
        pendingDeleteId?.let {
            deleteVideo(it)
        }
    }

    fun declineDelete() {
        pendingDeleteId = null
    }


    fun handleCreateFile(url: String, uri: Uri?) {
        if (uri == null) {
            showToastLiveData.postValue(App.appContext.getString(R.string.directory_not_selected))
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    isLoadingLiveData.postValue(true)
                    repository.downloadVideo(url, uri)
                    showToastLiveData.postValue(App.appContext.getString(R.string.download_successful))
                    saveFileStateMutableLiveData.postValue(true)
                } catch (t: Throwable) {
                    showToastLiveData.postValue(
                        App.appContext.getString(
                            R.string.download_error,
                            t.message
                        )
                    )
                    Log.e("Download Error", "Download error = ${t.message}")
                    saveFileStateMutableLiveData.postValue(false)
                } finally {
                    isLoadingLiveData.postValue(false)
                }
            }
        }
    }
}
