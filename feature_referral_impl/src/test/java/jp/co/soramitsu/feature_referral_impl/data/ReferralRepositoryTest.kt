/**
* Copyright Soramitsu Co., Ltd. All Rights Reserved.
* SPDX-License-Identifier: GPL-3.0
*/

package jp.co.soramitsu.feature_referral_impl.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.mockkStatic
import io.mockk.slot
import jp.co.soramitsu.common.R
import jp.co.soramitsu.core_db.AppDatabase
import jp.co.soramitsu.core_db.dao.ReferralsDao
import jp.co.soramitsu.core_db.model.ReferralLocal
import jp.co.soramitsu.feature_referral_api.data.ReferralRepository
import jp.co.soramitsu.sora.substrate.runtime.RuntimeManager
import jp.co.soramitsu.sora.substrate.substrate.ExtrinsicManager
import jp.co.soramitsu.sora.substrate.substrate.SubstrateApi
import jp.co.soramitsu.sora.substrate.substrate.SubstrateCalls
import jp.co.soramitsu.test_shared.MainCoroutineRule
import jp.co.soramitsu.xnetworking.subquery.ReferrerReward
import jp.co.soramitsu.xnetworking.subquery.ReferrerRewardsInfo
import jp.co.soramitsu.xnetworking.subquery.SubQueryClient
import jp.co.soramitsu.xnetworking.subquery.history.SubQueryHistoryItem
import jp.co.soramitsu.xnetworking.subquery.history.sora.SoraSubqueryResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReferralRepositoryTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var db: AppDatabase

    @Mock
    private lateinit var referralsDao: ReferralsDao

    @Mock
    private lateinit var subQueryClient: SubQueryClient<SoraSubqueryResponse, SubQueryHistoryItem>

    @Mock
    private lateinit var substrateApi: SubstrateApi

    @Mock
    private lateinit var runtimeManager: RuntimeManager

    @Mock
    private lateinit var extrinsicManager: ExtrinsicManager

    @Mock
    private lateinit var substrateCalls: SubstrateCalls

    private lateinit var referralRepository: ReferralRepository

    private val REFERRER_REWARDS = listOf(
        ReferrerReward(
            "address",
            "1000000000000000000"
        )
    )

    private val REFERRER_LOCAL = listOf(
        ReferralLocal(
            "address",
            "1000000000000000000"
        )
    )

    @Before
    fun setUp() = runTest {
        given(db.referralsDao()).willReturn(referralsDao)

        referralRepository = ReferralRepositoryImpl(
            db,
            extrinsicManager,
            runtimeManager,
            substrateCalls,
            subQueryClient,
        )
    }

    @Test
    fun `update referral rewards called`() = runTest {
        val address = "address"
        given(subQueryClient.getReferrerRewards(address)).willReturn(
            ReferrerRewardsInfo(
                REFERRER_REWARDS
            )
        )

        mockkStatic("androidx.room.RoomDatabaseKt")
        val lambda = slot<suspend () -> R>()
        coEvery { db.withTransaction(capture(lambda)) } coAnswers {
            lambda.captured.invoke()
        }
        referralRepository.updateReferralRewards(address)

        verify(referralsDao).insertReferrals(REFERRER_LOCAL)
    }

    @Test
    fun `get referral rewards`() = runTest {
        given(referralsDao.getReferrals()).willReturn(flow { emit(REFERRER_LOCAL) })

        val result = referralRepository.getReferralRewards()

        assertEquals(REFERRER_REWARDS, result.toList()[0])
    }

}