/**
* Copyright Soramitsu Co., Ltd. All Rights Reserved.
* SPDX-License-Identifier: GPL-3.0
*/

package jp.co.soramitsu.common.account

import android.graphics.drawable.PictureDrawable
import com.caverock.androidsvg.SVG
import javax.inject.Singleton
import jdenticon.Jdenticon
import jp.co.soramitsu.common.resourses.ResourceManager
import jp.co.soramitsu.shared_utils.extensions.toHexString
import jp.co.soramitsu.shared_utils.ss58.SS58Encoder.toAccountId


@Singleton
class AccountAvatarGenerator(
    private val resourceManager: ResourceManager,
) {
    private fun createAvatarFromKey(publicKey: String, sizeInDp: Int): PictureDrawable {
        val icon = Jdenticon.toSvg(publicKey, resourceManager.dp2px(sizeInDp))
        val svg = SVG.getFromString(icon)
        return PictureDrawable(svg.renderToPicture())
    }

    fun createAvatar(address: String, sizeInDp: Int): PictureDrawable =
        createAvatarFromKey(publicKey = address.toAccountId().toHexString(false), sizeInDp = sizeInDp)
}
