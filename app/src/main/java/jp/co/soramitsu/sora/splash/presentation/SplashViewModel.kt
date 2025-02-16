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

package jp.co.soramitsu.sora.splash.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.co.soramitsu.androidfoundation.coroutine.CoroutineManager
import jp.co.soramitsu.androidfoundation.fragment.SingleLiveEvent
import jp.co.soramitsu.androidfoundation.fragment.trigger
import jp.co.soramitsu.common.logger.FirebaseWrapper
import jp.co.soramitsu.common.presentation.viewmodel.BaseViewModel
import jp.co.soramitsu.feature_account_api.domain.model.OnboardingState
import jp.co.soramitsu.sora.splash.domain.SplashInteractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val interactor: SplashInteractor,
    coroutineManager: CoroutineManager,
) : BaseViewModel() {

    private val _runtimeInitiated = MutableLiveData<Boolean>()
    val runtimeInitiated: LiveData<Boolean> = _runtimeInitiated

    val loadingTextVisiblity = SingleLiveEvent<Unit>()

    val showMainScreen = SingleLiveEvent<Unit>()
    val showOnBoardingScreen = SingleLiveEvent<OnboardingState>()
    val showMainScreenFromInviteLink = SingleLiveEvent<Unit>()

    init {
        viewModelScope.launch {
            tryCatch {
                delay(500)
                _runtimeInitiated.value = true
            }
        }
        viewModelScope.launch {
            tryCatch {
                delay(5000)
                loadingTextVisiblity.trigger()
            }
        }
        viewModelScope.launch(coroutineManager.io) {
            interactor.checkMigration()
        }
    }

    fun nextScreen() {
        viewModelScope.launch {
            val migrationDone = interactor.getMigrationDoneAsync().await()
            FirebaseWrapper.log("Splash next screen $migrationDone")
            when (val state = interactor.getRegistrationState()) {
                OnboardingState.REGISTRATION_FINISHED -> {
                    showMainScreen.trigger()
                }
                OnboardingState.INITIAL -> {
                    showOnBoardingScreen.value = state
                }
            }
        }
    }
}
