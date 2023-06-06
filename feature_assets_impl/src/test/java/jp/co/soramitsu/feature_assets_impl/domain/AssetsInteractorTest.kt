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

package jp.co.soramitsu.feature_assets_impl.domain

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import jp.co.soramitsu.common.account.IrohaData
import jp.co.soramitsu.common.account.SoraAccount
import jp.co.soramitsu.common.domain.*
import jp.co.soramitsu.shared_utils.encrypt.keypair.substrate.Sr25519Keypair
import jp.co.soramitsu.feature_account_api.domain.interfaces.CredentialsRepository
import jp.co.soramitsu.feature_account_api.domain.interfaces.UserRepository
import jp.co.soramitsu.feature_assets_api.data.interfaces.AssetsRepository
import jp.co.soramitsu.feature_assets_api.domain.interfaces.AssetsInteractor
import jp.co.soramitsu.feature_blockexplorer_api.data.TransactionHistoryRepository
import jp.co.soramitsu.feature_blockexplorer_api.presentation.txhistory.TransactionBuilder
import jp.co.soramitsu.sora.substrate.models.ExtrinsicSubmitStatus
import jp.co.soramitsu.sora.substrate.runtime.RuntimeManager
import jp.co.soramitsu.sora.substrate.substrate.extrinsicHash
import jp.co.soramitsu.test_data.TestTokens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import java.math.BigDecimal

@RunWith(MockitoJUnitRunner::class)
@ExperimentalCoroutinesApi
class AssetsInteractorTest {
    @Mock
    private lateinit var assetsRepository: AssetsRepository

    @Mock
    private lateinit var transactionHistoryRepository: TransactionHistoryRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var credentialsRepository: CredentialsRepository

    @Mock
    private lateinit var runtimeManager: RuntimeManager

    @Mock
    private lateinit var builder: TransactionBuilder

    @Mock
    private lateinit var coroutineManager: CoroutineManager

    private lateinit var interactor: AssetsInteractor

    private val soraAccount = SoraAccount("address", "name")

    private val irohaData: IrohaData =
            IrohaData(address = "abcdef", claimSignature = "qweasdzc", publicKey = "publickey")

    @Before
    fun setUp() = runTest {
        mockkStatic(String::extrinsicHash)
        every { "0x112323345".extrinsicHash() } returns "blake2b"
        every { "0x35456472".extrinsicHash() } returns "blake2b"
        mockkObject(OptionsProvider)
        BDDMockito.given(userRepository.getCurSoraAccount()).willReturn(soraAccount)
        interactor = AssetsInteractorImpl(
                assetsRepository = assetsRepository,
                credentialsRepository = credentialsRepository,
                coroutineManager = coroutineManager,
                transactionBuilder = builder,
                transactionHistoryRepository = transactionHistoryRepository,
                userRepository = userRepository,
        )
    }

    @Test
    fun `get assets`() = runTest {
        BDDMockito.given(
                assetsRepository.getAssetsFavorite(
                        "address",
                )
        ).willReturn(assetList())
        Assert.assertEquals(assetList(), interactor.getVisibleAssets())
    }

    @Test
    fun `just transfer`() = runTest {
        val kp = Sr25519Keypair(ByteArray(32), ByteArray(32), ByteArray(32))
        BDDMockito.given(credentialsRepository.retrieveKeyPair(soraAccount)).willReturn(kp)
        BDDMockito.given(
                assetsRepository.transfer(
                        kp,
                        "address",
                        "to",
                        TestTokens.xorToken,
                        BigDecimal.ONE
                )
        ).willReturn(
                Result.success("")
        )
        Assert.assertEquals(
                Result.success(""),
                interactor.transfer("to", TestTokens.xorToken, BigDecimal.ONE)
        )
    }

    @Test
    fun `calc transaction fee`() = runTest {
        BDDMockito.given(
                assetsRepository.calcTransactionFee(
                        "address",
                        "to",
                        TestTokens.xorToken,
                        BigDecimal.ONE
                )
        ).willReturn(BigDecimal.TEN)
        Assert.assertEquals(
                BigDecimal.TEN,
                interactor.calcTransactionFee("to", TestTokens.xorToken, BigDecimal.ONE)
        )
    }

    @Test
    fun `observe transfer`() = runTest(UnconfinedTestDispatcher()) {
        val kp = Sr25519Keypair(ByteArray(32), ByteArray(32), ByteArray(32))
        BDDMockito.given(credentialsRepository.retrieveKeyPair(soraAccount)).willReturn(kp)
        BDDMockito.given(
                assetsRepository.observeTransfer(
                        any(),
                        BDDMockito.anyString(),
                        BDDMockito.anyString(),
                        any(),
                        any(),
                        any(),
                )
        ).willReturn(
                ExtrinsicSubmitStatus(true, "txhash", "")
        )
        Assert.assertEquals(
                "txhash",
                interactor.observeTransfer("to", TestTokens.xorToken, BigDecimal.ONE, BigDecimal.ONE)
        )
    }

    @Test
    fun `hide assets`() = runTest {
        val assets = listOf("id1", "id2")
        BDDMockito.given(assetsRepository.hideAssets(assets, soraAccount)).willReturn(Unit)
        Assert.assertEquals(Unit, interactor.tokenFavoriteOff(assets))
    }

    @Test
    fun `display assets`() = runTest {
        val assets = listOf("id1", "id2")
        BDDMockito.given(assetsRepository.displayAssets(assets, soraAccount)).willReturn(Unit)
        Assert.assertEquals(Unit, interactor.tokenFavoriteOn(assets))
    }

    @Test
    fun `update assets position`() = runTest {
        val assets = mapOf("id1" to 1, "id2" to 2)
        BDDMockito.given(assetsRepository.updateAssetPositions(assets, soraAccount)).willReturn(Unit)
        Assert.assertEquals(Unit, interactor.updateAssetPositions(assets))
    }

    @Test
    fun `get account id called`() = runTest {
        Assert.assertEquals(soraAccount.substrateAddress, interactor.getCurSoraAccount().substrateAddress)
    }

    private fun accountList() = listOf(
            "use","contact1","contact2",
    )

    private fun assetList() = listOf(
            Asset(oneToken(), true, 1, assetBalance(), true),
    )

    private fun oneToken() = Token(
            "token_id",
            "token name",
            "token symbol",
            18,
            true,
            null,
            null,
            null,
            null,
    )

    private fun assetBalance() = AssetBalance(
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
    )
}
