/**
* Copyright Soramitsu Co., Ltd. All Rights Reserved.
* SPDX-License-Identifier: GPL-3.0
*/

package jp.co.soramitsu.sora.substrate

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKException
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import jp.co.soramitsu.shared_utils.extensions.fromHex
import jp.co.soramitsu.shared_utils.wsrpc.SocketService
import jp.co.soramitsu.sora.substrate.runtime.RuntimeManager
import jp.co.soramitsu.sora.substrate.substrate.SubstrateApiImpl
import jp.co.soramitsu.test_shared.RealRuntimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class SubstrateApiTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    lateinit var socket: SocketService

    @MockK
    lateinit var runtimeManager: RuntimeManager

    private lateinit var api: SubstrateApiImpl

    private fun setUpApi() {
        api = SubstrateApiImpl(socket, runtimeManager)
    }

    @Test(expected = MockKException::class)
    fun `dev env subscribe getPoolReserveAccount`() = runTest {
        val n = RealRuntimeProvider.buildRuntime(networkName = "sora2", suffix = "_dev")
        coEvery { runtimeManager.getRuntimeSnapshot() } returns n
        setUpApi()

        val baseTokenId = "0x0200000000000000000000000000000000000000000000000000000000000000"
        val tokenId = "0x0200050000000000000000000000000000000000000000000000000000000000"
        val t = api.getPoolReserveAccount(baseTokenId, tokenId.fromHex())
        assertEquals(byteArrayOf(12, 12, 14), t)
    }

    @Test(expected = MockKException::class)
    fun `soralution env subscribe getPoolReserveAccount`() = runTest {
        val n = RealRuntimeProvider.buildRuntime(networkName = "sora2", suffix = "_soralution")
        coEvery { runtimeManager.getRuntimeSnapshot() } returns n
        setUpApi()

        val baseTokenId = "0x0200000000000000000000000000000000000000000000000000000000000000"
        val tokenId = "0x0200050000000000000000000000000000000000000000000000000000000000"
        val t = api.getPoolReserveAccount(baseTokenId, tokenId.fromHex())
        assertEquals(byteArrayOf(12, 12, 14), t)
    }
}
