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

package app.ss.lessons.data.repository.quarterly

import app.ss.models.Language
import app.ss.models.PublishingInfo
import app.ss.models.QuarterlyGroup
import app.ss.models.SSQuarterly
import app.ss.models.SSQuarterlyInfo
import com.cryart.sabbathschool.core.extensions.prefs.SSPrefs
import com.cryart.sabbathschool.core.misc.DateHelper
import com.cryart.sabbathschool.core.misc.DeviceHelper
import com.cryart.sabbathschool.core.response.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import org.joda.time.DateTime
import org.joda.time.Interval
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class QuarterliesRepositoryImpl @Inject constructor(
    private val ssPrefs: SSPrefs,
    private val languagesSource: LanguagesDataSource,
    private val quarterliesDataSource: QuarterliesDataSource,
    private val quarterlyInfoDataSource: QuarterlyInfoDataSource,
    private val publishingInfoDataSource: PublishingInfoDataSource,
    private val deviceHelper: DeviceHelper
) : QuarterliesRepository {

    override fun getLanguages(): Flow<Resource<List<Language>>> = languagesSource.getAsFlow(LanguagesDataSource.Request())

    override fun getQuarterlies(
        languageCode: String?,
        group: QuarterlyGroup?
    ): Flow<Resource<List<SSQuarterly>>> {
        val code = languageCode ?: ssPrefs.getLanguageCode()
        return quarterliesDataSource.getAsFlow(QuarterliesDataSource.Request(code, group))
    }

    override fun getQuarterlyInfo(index: String): Flow<Resource<SSQuarterlyInfo>> =
        quarterlyInfoDataSource.getItemAsFlow(QuarterlyInfoDataSource.Request(index))
            .filter { it.data?.lessons?.isNotEmpty() == true }
            .onEach { resource ->
                if (resource.isSuccessFul) {
                    ssPrefs.setLastQuarterlyIndex(index)

                    resource.data?.run {
                        ssPrefs.setThemeColor(
                            primary = quarterly.color_primary,
                            primaryDark = quarterly.color_primary_dark
                        )
                        ssPrefs.setReadingLatestQuarterly(quarterly.isLatest)
                    }
                }
            }

    override fun getPublishingInfo(languageCode: String?): Flow<Resource<PublishingInfo>> {
        val language = languageCode ?: ssPrefs.getLanguageCode()
        val country = deviceHelper.country()

        return publishingInfoDataSource.getItemAsFlow(PublishingInfoDataSource.Request(country, language))
    }

    private val SSQuarterly.isLatest: Boolean
        get() {
            val today = DateTime.now().withTimeAtStartOfDay()

            val startDate = DateHelper.parseDate(start_date)
            val endDate = DateHelper.parseDate(end_date)

            return Interval(startDate, endDate?.plusDays(1)).contains(today)
        }
}
