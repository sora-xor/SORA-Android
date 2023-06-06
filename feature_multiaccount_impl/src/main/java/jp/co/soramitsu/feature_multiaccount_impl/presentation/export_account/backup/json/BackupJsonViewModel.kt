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

package jp.co.soramitsu.feature_multiaccount_impl.presentation.export_account.backup.json

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import jp.co.soramitsu.common.presentation.compose.components.initMediumTitle2
import jp.co.soramitsu.common.presentation.viewmodel.BaseViewModel
import jp.co.soramitsu.common.resourses.ResourceManager
import jp.co.soramitsu.feature_main_api.launcher.MainRouter
import jp.co.soramitsu.feature_multiaccount_impl.R
import jp.co.soramitsu.feature_multiaccount_impl.domain.MultiaccountInteractor
import jp.co.soramitsu.feature_multiaccount_impl.presentation.export_account.model.BackupJsonScreenState
import jp.co.soramitsu.ui_core.component.input.InputTextState
import kotlinx.coroutines.launch

class BackupJsonViewModel @AssistedInject constructor(
    private val interactor: MultiaccountInteractor,
    private val router: MainRouter,
    @Assisted("addresses") private val addresses: List<String>,
    resourceManager: ResourceManager,
) : BaseViewModel() {

    @AssistedFactory
    interface BackupJsonViewModelFactory {
        fun create(
            @Assisted("addresses") addresses: List<String>,
        ): BackupJsonViewModel
    }

    private val _backupJsonScreenState = MutableLiveData<BackupJsonScreenState>()
    val backupJsonScreenState: LiveData<BackupJsonScreenState> = _backupJsonScreenState

    private val _jsonTextLiveData = MutableLiveData<Uri>()
    val jsonTextLiveData: LiveData<Uri> = _jsonTextLiveData

    init {
        _toolbarState.value = initMediumTitle2(
            title = R.string.export_json_download_json,
        )
        _backupJsonScreenState.value = BackupJsonScreenState(
            state = InputTextState(
                label = resourceManager.getString(R.string.export_json_input_label)
            ),
            confirmationState = InputTextState(
                label = resourceManager.getString(R.string.export_json_input_confirmation_label)
            )
        )
    }

    fun passwordInputChanged(textFieldValue: TextFieldValue) {
        backupJsonScreenState.value?.let {
            _backupJsonScreenState.value = it.copy(
                state = it.state.copy(textFieldValue),
                buttonEnabledState = textFieldValue.text.isNotEmpty() && textFieldValue.text == it.confirmationState.value.text
            )
        }
    }

    fun confirmationInputChanged(textFieldValue: TextFieldValue) {
        backupJsonScreenState.value?.let {
            _backupJsonScreenState.value = it.copy(
                confirmationState = it.confirmationState.copy(textFieldValue),
                buttonEnabledState = textFieldValue.text.isNotEmpty() && it.state.value.text == textFieldValue.text
            )
        }
    }

    fun downloadJsonClicked() {
        viewModelScope.launch {
            _backupJsonScreenState.value?.let {
                _jsonTextLiveData.value =
                    interactor.getJsonFileUri(addresses, it.confirmationState.value.text)
            }
        }
    }

    override fun onNavIcon() {
        router.popBackStackToAccountList()
    }

    override fun onBackPressed() {
        router.popBackStackToAccountList()
    }
}
