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

package jp.co.soramitsu.feature_wallet_impl.presentation.cardshub

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import jp.co.soramitsu.common.R
import jp.co.soramitsu.common.presentation.compose.extension.noRippleClickable
import jp.co.soramitsu.ui_core.component.button.BleachedButton
import jp.co.soramitsu.ui_core.component.button.TextButton
import jp.co.soramitsu.ui_core.component.button.properties.Order
import jp.co.soramitsu.ui_core.component.button.properties.Size
import jp.co.soramitsu.ui_core.component.card.ContentCard
import jp.co.soramitsu.ui_core.resources.Dimens
import jp.co.soramitsu.ui_core.theme.customColors
import jp.co.soramitsu.ui_core.theme.customTypography

@Preview
@Composable
private fun PreviewTopBar() {
    Box(modifier = Modifier.background(Color.Red)) {
        TopBar(account = "cnJkhsdlf...sdlndskf", {}, {})
    }
}

@Composable
fun TopBar(
    account: String,
    onAccountClick: () -> Unit,
    onQrClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = Dimens.x3, end = Dimens.x2, top = Dimens.x3)
            .height(Size.Small)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onAccountClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = account,
                style = MaterialTheme.customTypography.displayS,
                color = MaterialTheme.customColors.fgPrimary,
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrows_chevron_right_rounded_24),
                tint = MaterialTheme.customColors.fgPrimary,
                contentDescription = null
            )
        }
        Box(modifier = Modifier.size(Size.Small)) {
            BleachedButton(
                modifier = Modifier,
                shape = CircleShape,
                size = Size.Small,
                order = Order.TERTIARY,
                leftIcon = painterResource(R.drawable.ic_scan_qr),
                onClick = onQrClick,
            )
        }
    }
}

@Composable
fun CommonHubCard(
    @StringRes title: Int,
    amount: String,
    collapseState: Boolean,
    onExpandClick: (() -> Unit)? = null,
    onCollapseClick: () -> Unit,
    content: @Composable (ColumnScope) -> Unit
) {
    ContentCard(
        modifier = Modifier.padding(top = Dimens.x1_5),
        onClick = onExpandClick
    ) {
        Column(
            modifier = Modifier
                .padding(top = Dimens.x3, bottom = Dimens.x4)
                .animateContentSize(),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = Dimens.x3)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .noRippleClickable(onClick = onCollapseClick),
                        text = stringResource(id = title),
                        style = MaterialTheme.customTypography.headline2,
                        color = MaterialTheme.customColors.fgPrimary,
                    )
                    Icon(
                        painter = painterResource(if (collapseState) R.drawable.ic_chevron_down_rounded_16 else R.drawable.ic_chevron_up_rounded_16),
                        tint = MaterialTheme.customColors.fgPrimary,
                        contentDescription = null
                    )
                }
                Text(
                    text = amount,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.customTypography.headline2,
                    color = MaterialTheme.customColors.fgPrimary,
                )
            }
            if (!collapseState) {
                Spacer(modifier = Modifier.size(size = Dimens.x2))
                content.invoke(this)
                TextButton(
                    modifier = Modifier
                        .padding(start = Dimens.x1, top = Dimens.x3),
                    text = stringResource(id = R.string.common_expand),
                    size = Size.ExtraSmall,
                    order = Order.PRIMARY,
                ) {
                    onExpandClick?.invoke()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCommonHubCard() {
    CommonHubCard(
        title = R.string.common_ok,
        amount = "123.123",
        collapseState = false,
        onCollapseClick = {}
    ) {
        Text(text = "text1")
        Text(text = "text2")
    }
}
