/*
 * Copyright (c) 2021. Adventech <info@adventech.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package app.ss.media.playback.ui.nowPlaying

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ss.media.playback.PlaybackConnection
import app.ss.media.playback.model.AudioFile
import app.ss.media.repository.SSMediaRepository
import com.cryart.sabbathschool.core.extensions.coroutines.flow.stateIn
import com.cryart.sabbathschool.core.extensions.intent.lessonIndex
import com.cryart.sabbathschool.core.extensions.intent.readIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val repository: SSMediaRepository,
    val playbackConnection: PlaybackConnection,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val playList: StateFlow<List<AudioFile>>
        get() = flowOf(savedStateHandle.lessonIndex)
            .map { index ->
                index?.let { repository.getPlayList(it) } ?: emptyList()
            }
            .stateIn(viewModelScope, emptyList())

    init {
        println("LESSON INDEX: ${savedStateHandle.lessonIndex}")
        println("READ INDEX: ${savedStateHandle.readIndex}")
    }
}
