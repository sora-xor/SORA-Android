/*
This file is part of the SORA network and Polkaswap app.

Copyright (c) 2020, 2021, Polka Biome Ltd. All rights reserved.
SPDX-License-Identifier: BSD-4-Clause

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or other
materials provided with the distribution.

All advertising materials mentioning features or use of this software must display
the following acknowledgement: This product includes software developed by Polka Biome
Ltd., SORA, and Polkaswap.

Neither the name of the Polka Biome Ltd. nor the names of its contributors may be used
to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY Polka Biome Ltd. AS IS AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Polka Biome Ltd. BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package jp.co.soramitsu.common.domain

import java.net.HttpURLConnection
import java.net.URL
import jp.co.soramitsu.common.BuildConfig

object OptionsProvider {
    var CURRENT_VERSION_CODE: Int = 0
    var CURRENT_VERSION_NAME: String = ""
    var APPLICATION_ID: String = ""
    val configCommon: String by lazy {
        if (isUrlAccessible(configIpfsCommon)) configIpfsCommon else configGithubCommon
    }
    val configMobile: String by lazy {
        if (isUrlAccessible(configIpfsMobile)) configIpfsCommon else configGithubMobile
    }
    val configXn: String by lazy {
        if (isUrlAccessible(configIpfsXn)) configIpfsCommon else configGithubXn
    }
    private const val configIpfsCommon = "https://config.polkaswap2.io/${FlavorOptionsProvider.typesFilePath}/common.json"
    private const val configGithubCommon = "https://www.arvifox.com/soramitsu/sora2-config/${FlavorOptionsProvider.typesFilePath}/common.json"
    private const val configIpfsMobile = "https://config.polkaswap2.io/${FlavorOptionsProvider.typesFilePath}/mobile.json"
    private const val configGithubMobile = "https://www.arvifox.com/soramitsu/sora2-config/${FlavorOptionsProvider.typesFilePath}/mobile.json"
    private const val configIpfsXn = "https://config.polkaswap2.io/${FlavorOptionsProvider.typesFilePath}/xn.json"
    private const val configGithubXn = "https://www.arvifox.com/soramitsu/sora2-config/${FlavorOptionsProvider.typesFilePath}/xn.json"

    val fileProviderAuthority: String get() = "$APPLICATION_ID.soraFileProvider"
    val header: String by lazy {
        "$APPLICATION_ID/$CURRENT_VERSION_NAME/$CURRENT_VERSION_CODE/${BuildConfig.BUILD_TYPE}/${BuildConfig.FLAVOR}"
    }

    private fun isUrlAccessible(url: String, maxAttempts: Int = 2): Boolean {
        repeat(maxAttempts) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "HEAD"
                    connectTimeout = 3000
                    readTimeout = 3000
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }
        return false
    }

    const val substrate = "substrate"
    const val defaultScale = 18
    const val nameByteLimit = 32
    const val fiatSymbol = "$"
    const val nbspace = Typography.nbsp
    const val euroSign = '€'
    const val soracard = "2.2.4"
    const val soracardFiatPackageTest = "com.soracard.wallet.iban.test"
    const val soracardFiatPackageProd = "com.soracard.wallet.iban"

    const val website = "https://sora.org"
    const val sourceLink = "https://github.com/sora-xor/Sora-Android"
    const val telegramLink = "https://t.me/sora_xor"
    const val telegramAnnouncementsLink = "https://t.me/sora_announcements"
    const val telegramHappinessLink = "https://t.me/sora_happy"
    const val email = "support@sora.org"
    const val twitterLink = "https://twitter.com/sora_xor"
    const val youtubeLink = "https://youtube.com/sora_xor"
    const val instagramLink = "https://instagram.com/sora_xor"
    const val mediumLink = "https://medium.com/sora-xor"
    const val wikiLink = "https://wiki.sora.org"
    const val soraCardBlackList = "https://soracard.com/blacklist/"
}
