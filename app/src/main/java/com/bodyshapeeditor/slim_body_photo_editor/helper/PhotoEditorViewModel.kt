package com.bodyshapeeditor.slim_body_photo_editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {
    // Public immutable LiveData
    val imageUris: LiveData<List<Uri>> get() = _imageUris
    private val _imageUris = MutableLiveData<List<Uri>>()
    val photoEditorHelper = PhotoEditorHelper()

    // Load images using MediaStore
    fun loadImagesFromDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val uris = photoEditorHelper.loadImagesFromDevice(context =getApplication() )
            _imageUris.postValue(uris)
        }
    }

}