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

package jp.co.soramitsu.common.presentation.compose.components

import androidx.compose.foundation.ScrollState
import androidx.compose.material.AppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import jp.co.soramitsu.common.R
import jp.co.soramitsu.common.util.PACKAGE_ID
import jp.co.soramitsu.ui_core.component.toolbar.Action
import jp.co.soramitsu.ui_core.component.toolbar.BasicToolbarState
import jp.co.soramitsu.ui_core.component.toolbar.SoramitsuToolbar
import jp.co.soramitsu.ui_core.component.toolbar.SoramitsuToolbarState
import jp.co.soramitsu.ui_core.component.toolbar.SoramitsuToolbarType

fun initNoToolbar() = SoramitsuToolbarState(
    basic = BasicToolbarState(
        title = "",
        visibility = false,
        navIcon = R.drawable.ic_arrow_left,
    ),
    type = SoramitsuToolbarType.SmallCentered(),
)

fun initSmallTitle2(title: Any) = SoramitsuToolbarState(
    basic = BasicToolbarState(
        title = title,
        visibility = true,
        navIcon = R.drawable.ic_arrow_left,
    ),
    type = SoramitsuToolbarType.Small(),
)

fun initMediumTitle2(title: Any) = SoramitsuToolbarState(
    basic = BasicToolbarState(
        title = title,
        visibility = true,
        navIcon = R.drawable.ic_arrow_left,
    ),
    type = SoramitsuToolbarType.Medium(),
)

fun initSmallTitleOnly(title: Any) = SoramitsuToolbarState(
    basic = BasicToolbarState(
        title = title,
        visibility = true,
        navIcon = null,
    ),
    type = SoramitsuToolbarType.Small(),
)

@Composable
fun Toolbar(
    toolbarState: SoramitsuToolbarState?,
    scrollState: ScrollState?,
    backgroundColor: Color,
    tintColor: Color,
    onNavClick: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    onMenuItemClick: ((Action) -> Unit)? = null,
    onSearch: ((String) -> Unit)? = null,
) {
    if (toolbarState != null && toolbarState.basic.visibility) {
        val elevation = remember(scrollState) {
            derivedStateOf {
                if (scrollState == null || scrollState.value == 0) {
                    0.dp
                } else {
                    AppBarDefaults.TopAppBarElevation
                }
            }
        }

        SoramitsuToolbar(
            state = toolbarState,
            elevation = elevation.value,
            backgroundColor = backgroundColor,
            tint = tintColor,
            onNavigate = onNavClick,
            onNavigateTestTag = "$PACKAGE_ID:id/OnNavigate",
            onAction = onActionClick,
            onActionTestTag = "$PACKAGE_ID:id/OnAction",
            onMenuItemClicked = onMenuItemClick,
            onMenuItemClickedTestTag = "$PACKAGE_ID:id/OnMenuItem",
            onSearch = onSearch,
            onSearchTestTag = "$PACKAGE_ID:id/OnSearch",
        )
    }
}
