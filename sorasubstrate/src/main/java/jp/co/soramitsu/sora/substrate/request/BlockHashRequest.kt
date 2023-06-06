/**
* Copyright Soramitsu Co., Ltd. All Rights Reserved.
* SPDX-License-Identifier: GPL-3.0
*/

package jp.co.soramitsu.sora.substrate.request

import jp.co.soramitsu.common.util.ext.removeHexPrefix
import jp.co.soramitsu.shared_utils.wsrpc.request.runtime.RuntimeRequest

class BlockHashRequest(number: Int) : RuntimeRequest("chain_getBlockHash", listOf(number)) {
    constructor(n: String) : this(n.removeHexPrefix().toInt(16))
}
