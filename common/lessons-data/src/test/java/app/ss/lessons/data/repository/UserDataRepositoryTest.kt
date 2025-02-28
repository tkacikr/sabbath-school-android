/*
 * Copyright (c) 2022. Adventech <info@adventech.io>
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

package app.ss.lessons.data.repository

import app.ss.storage.db.dao.PdfAnnotationsDao
import app.ss.storage.db.dao.ReadCommentsDao
import app.ss.storage.db.dao.ReadHighlightsDao
import com.cryart.sabbathschool.core.extensions.prefs.SSPrefs
import com.cryart.sabbathschool.test.coroutines.TestDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UserDataRepositoryTest {

    private val readHighlightsDao: ReadHighlightsDao = mockk()
    private val readCommentsDao: ReadCommentsDao = mockk()
    private val pdfAnnotationsDao: PdfAnnotationsDao = mockk()
    private val ssPrefs: SSPrefs = mockk()

    private lateinit var repository: UserDataRepository

    @Before
    fun setup() {
        repository = UserDataRepository(
            readHighlightsDao = readHighlightsDao,
            readCommentsDao = readCommentsDao,
            pdfAnnotationsDao = pdfAnnotationsDao,
            ssPrefs = ssPrefs,
            dispatcherProvider = TestDispatcherProvider()
        )
    }

    @Test
    fun clear() = runTest {
        coEvery { readHighlightsDao.clear() }.returns(Unit)
        coEvery { readCommentsDao.clear() }.returns(Unit)
        coEvery { pdfAnnotationsDao.clear() }.returns(Unit)
        coEvery { ssPrefs.clear() }.returns(Unit)

        repository.clear()

        coVerify {
            readHighlightsDao.clear()
            readCommentsDao.clear()
            pdfAnnotationsDao.clear()
        }
        verify { ssPrefs.clear() }
    }
}
