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

package jp.co.soramitsu.feature_ecosystem_impl.presentation.allpools

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.co.soramitsu.common.domain.CoroutineManager
import jp.co.soramitsu.common.domain.isMatchFilter
import jp.co.soramitsu.common.presentation.viewmodel.BaseViewModel
import jp.co.soramitsu.common.util.StringPair
import jp.co.soramitsu.feature_ecosystem_impl.domain.EcoSystemMapper
import jp.co.soramitsu.feature_ecosystem_impl.domain.EcoSystemPools
import jp.co.soramitsu.feature_ecosystem_impl.domain.EcoSystemPoolsInteractor
import jp.co.soramitsu.feature_ecosystem_impl.presentation.EcoSystemPoolsState
import jp.co.soramitsu.feature_ecosystem_impl.presentation.initialEcoSystemPoolsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class AllPoolsViewModel @Inject constructor(
    ecoSystemPoolsInteractor: EcoSystemPoolsInteractor,
    coroutineManager: CoroutineManager,
    private val ecoSystemMapper: EcoSystemMapper,
) : BaseViewModel() {

    private val filter = MutableStateFlow("")
    private var positions: Map<StringPair, String>? = null

    val poolsState = ecoSystemPoolsInteractor.subscribeBasicPools()
        .onEach {
            positions = it.pools.mapIndexed { index, ecoSystemToken ->
                (ecoSystemToken.pool.baseToken.id to ecoSystemToken.pool.targetToken.id) to (index + 1).toString()
            }.toMap()
        }
        .combine(filter) { t1, t2 ->
            t1 to t2
        }
        .catch {
            onError(it)
        }
        .map { pair ->
            val filtered = pair.first.pools.filter {
                it.pool.baseToken.isMatchFilter(pair.second) || it.pool.targetToken.isMatchFilter(
                    pair.second
                )
            }
            val mapped = ecoSystemMapper.mapEcoSystemPools(EcoSystemPools(filtered))
            val mappedEnumerated = mapped.map {
                val indexInAll = positions?.get(it.ids).orEmpty()
                it.copy(number = indexInAll)
            }
            EcoSystemPoolsState(
                pools = mappedEnumerated,
            )
        }
        .flowOn(coroutineManager.io)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialEcoSystemPoolsState)

    fun onSearch(value: String) {
        filter.value = value
    }
}
