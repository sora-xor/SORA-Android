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

package jp.co.soramitsu.feature_select_node_impl.domain

import javax.inject.Inject
import jp.co.soramitsu.common.domain.ChainNode
import jp.co.soramitsu.common.domain.CoroutineManager
import jp.co.soramitsu.feature_select_node_api.data.SelectNodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal class SelectNodeInteractor @Inject constructor(
    private val repository: SelectNodeRepository,
    private val nodeValidator: NodeValidator,
    private val coroutineManager: CoroutineManager,
) {

    fun subscribeNodes(): Flow<List<ChainNode>> {
        return repository.getNodes()
            .flowOn(coroutineManager.io)
    }

    fun subscribeSelectedNode(): Flow<ChainNode?> {
        return repository.getSelectedNode()
    }

    fun validateNodeAddress(url: String): ValidationEvent {
        return nodeValidator.validate(url)
    }

    suspend fun addCustomNode(node: ChainNode) {
        repository.addCustomNode(node)
    }

    suspend fun updateCustomNode(previousAddress: String, node: ChainNode) {
        repository.updateCustomNode(previousAddress, node)
    }

    suspend fun removeNode(node: ChainNode) {
        repository.deleteNode(node.address)
    }
}
