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

package app.ss.storage.db.dao

import androidx.room.Dao
import androidx.room.Query
import app.ss.models.LessonPdf
import app.ss.models.SSDay
import app.ss.storage.db.entity.LessonEntity

@Dao
interface LessonsDao : BaseDao<LessonEntity> {

    @Query("SELECT * FROM lessons WHERE `index` = :lessonIndex")
    fun get(lessonIndex: String): LessonEntity?

    @Query("UPDATE lessons SET days = :days, pdfs = :pdfs WHERE `index` = :lessonIndex")
    fun updateInfo(
        lessonIndex: String,
        days: List<SSDay>,
        pdfs: List<LessonPdf>
    )
}
