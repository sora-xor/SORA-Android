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

package jp.co.soramitsu.feature_sora_card_impl.presentation.get.card

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockkObject
import jp.co.soramitsu.common.R
import jp.co.soramitsu.common.domain.OptionsProvider
import jp.co.soramitsu.common.resourses.ResourceManager
import jp.co.soramitsu.feature_assets_api.presentation.launcher.AssetsRouter
import jp.co.soramitsu.feature_main_api.launcher.MainRouter
import jp.co.soramitsu.feature_polkaswap_api.launcher.PolkaswapRouter
import jp.co.soramitsu.feature_sora_card_api.domain.SoraCardInteractor
import jp.co.soramitsu.feature_sora_card_api.domain.models.SoraCardAvailabilityInfo
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletInteractor
import jp.co.soramitsu.feature_wallet_api.launcher.WalletRouter
import jp.co.soramitsu.sora.substrate.runtime.SubstrateOptionsProvider
import jp.co.soramitsu.sora.substrate.substrate.ConnectionManager
import jp.co.soramitsu.test_data.SoraCardTestData
import jp.co.soramitsu.test_shared.MainCoroutineRule
import jp.co.soramitsu.test_shared.getOrAwaitValue
import jp.co.soramitsu.ui_core.component.toolbar.SoramitsuToolbarType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import java.math.BigDecimal

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetSoraCardViewModelTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var soraCardInteractor: SoraCardInteractor

    @Mock
    private lateinit var walletInteractor: WalletInteractor

    @Mock
    private lateinit var assetsRouter: AssetsRouter

    @Mock
    private lateinit var polkaswapRouter: PolkaswapRouter

    @Mock
    private lateinit var walletRouter: WalletRouter

    @Mock
    private lateinit var mainRouter: MainRouter

    @Mock
    private lateinit var resourceManager: ResourceManager

    @Mock
    private lateinit var viewModel: GetSoraCardViewModel

    @Mock
    private lateinit var connectionManager: ConnectionManager

    @Before
     fun setUp() = runTest {
        given(walletInteractor.subscribeSoraCardInfo()).willReturn(flowOf(SoraCardTestData.SORA_CARD_INFO))
        given(connectionManager.connectionState).willReturn(flowOf(true))

        mockkObject(OptionsProvider)
        every { OptionsProvider.header } returns "test android client"

        given(soraCardInteractor.subscribeToSoraCardAvailabilityFlow()).willReturn(flowOf(
            SoraCardAvailabilityInfo(
                xorBalance = BigDecimal.ONE,
                enoughXor = true,
            )
        ))

        viewModel = GetSoraCardViewModel(
            assetsRouter,
            walletInteractor,
            walletRouter,
            mainRouter,
            polkaswapRouter,
            resourceManager,
            connectionManager,
            soraCardInteractor,
            shouldStartSignIn = false,
            shouldStartSignUp = false
        )
    }

    @Test
    fun `init EXPECT toolbar title`() {
        val s = viewModel.toolbarState.getOrAwaitValue()
        assertTrue(s.type is SoramitsuToolbarType.Small)
        assertEquals(R.string.get_sora_card_title, s.basic.title)
    }

    @Test
    fun `init EXPECT subscribe fee asset balance`() = runTest {
        advanceUntilIdle()

        verify(soraCardInteractor).subscribeToSoraCardAvailabilityFlow()
    }

    @Test
    fun `init EXPECT subscribe sora card info`() = runTest {
        advanceUntilIdle()

        verify(walletInteractor).subscribeSoraCardInfo()
    }

    @Test
    fun `enable sora card EXPECT set up launcher`() = runTest{
        advanceUntilIdle()

        viewModel.onEnableCard()

        assertNotNull(viewModel.launchSoraCardRegistration.value)
    }

    @Test
    fun `on buy crypto EXPECT navigate to buy crypto`() {
        viewModel.onBuyCrypto()

        verify(assetsRouter).showBuyCrypto()
    }

    @Test
    fun `on swap EXPECT navigate swap`() {
        viewModel.onSwap()

        verify(polkaswapRouter).showSwap(tokenToId = SubstrateOptionsProvider.feeAssetId)
    }

    @Test
    fun `updateSoraCardInfo EXPECT update data`() = runTest {
        viewModel.updateSoraCardInfo(
            accessToken = "accessToken",
            accessTokenExpirationTime = Long.MAX_VALUE,
            kycStatus = "kycStatus"
        )
        advanceUntilIdle()

        verify(walletInteractor).updateSoraCardInfo(
            accessToken = "accessToken",
            accessTokenExpirationTime = Long.MAX_VALUE,
            kycStatus = "kycStatus"
        )
    }

    @Test
    fun `onSeeBlacklist EXPECT open web view`() = runTest {
        given(resourceManager.getString(R.string.sora_card_blacklisted_countires_title))
            .willReturn("Title")

        viewModel.onSeeBlacklist()

        verify(mainRouter).showWebView("Title", "https://soracard.com/blacklist/")
    }
}
