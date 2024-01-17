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

package jp.co.soramitsu.feature_main_impl.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.co.soramitsu.common.R
import jp.co.soramitsu.common.domain.IbanInfo
import jp.co.soramitsu.common.presentation.SingleLiveEvent
import jp.co.soramitsu.common.presentation.viewmodel.BaseViewModel
import jp.co.soramitsu.common.util.BuildUtils
import jp.co.soramitsu.feature_assets_api.presentation.AssetsRouter
import jp.co.soramitsu.feature_blockexplorer_api.data.SoraConfigManager
import jp.co.soramitsu.feature_main_api.launcher.MainRouter
import jp.co.soramitsu.feature_main_impl.domain.MainInteractor
import jp.co.soramitsu.feature_polkaswap_api.launcher.PolkaswapRouter
import jp.co.soramitsu.feature_referral_api.ReferralRouter
import jp.co.soramitsu.feature_select_node_api.NodeManager
import jp.co.soramitsu.feature_select_node_api.SelectNodeRouter
import jp.co.soramitsu.feature_sora_card_api.domain.SoraCardInteractor
import jp.co.soramitsu.feature_sora_card_api.util.createSoraCardContract
import jp.co.soramitsu.feature_wallet_api.launcher.WalletRouter
import jp.co.soramitsu.oauth.base.sdk.contract.OutwardsScreen
import jp.co.soramitsu.oauth.base.sdk.contract.SoraCardCommonVerification
import jp.co.soramitsu.oauth.base.sdk.contract.SoraCardContractData
import jp.co.soramitsu.oauth.base.sdk.contract.SoraCardResult
import jp.co.soramitsu.sora.substrate.runtime.SubstrateOptionsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val assetsRouter: AssetsRouter,
    interactor: MainInteractor,
    private val polkaswapRouter: PolkaswapRouter,
    private val router: MainRouter,
    private val walletRouter: WalletRouter,
    private val referralRouter: ReferralRouter,
    private val selectNodeRouter: SelectNodeRouter,
    private val soraConfigManager: SoraConfigManager,
    private val soraCardInteractor: SoraCardInteractor,
    nodeManager: NodeManager,
) : BaseViewModel() {

    private val _state = MutableStateFlow(
        ProfileScreenState(
            nodeName = "",
            nodeConnected = false,
            isDebugMenuAvailable = BuildUtils.isPlayMarket().not(),
            soraCardEnabled = false,
            soraCardStatusStringRes = R.string.more_menu_sora_card_subtitle,
            soraCardStatusIconDrawableRes = null,
            soraCardNeedUpdate = false,
            soraCardIbanError = null,
        )
    )
    internal val state = _state.asStateFlow()

    private val _launchSoraCardSignIn = SingleLiveEvent<SoraCardContractData>()
    val launchSoraCardSignIn: LiveData<SoraCardContractData> = _launchSoraCardSignIn

    private var currentSoraCardContractData: SoraCardContractData? = null

    private var soraCardInfo: SoraCardCommonVerification = SoraCardCommonVerification.NotFound
    private var ibanInfo: IbanInfo? = null

    init {
        interactor.flowSelectedNode()
            .catch { onError(it) }
            .distinctUntilChanged()
            .onEach { node ->
                _state.value = _state.value.copy(nodeName = node?.name.orEmpty())
            }
            .launchIn(viewModelScope)

        nodeManager.connectionState
            .catch { onError(it) }
            .distinctUntilChanged()
            .onEach { connected ->
                _state.value = _state.value.copy(nodeConnected = connected)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _state.value = _state.value.copy(
                soraCardEnabled = soraConfigManager.getSoraCard(),
                soraCardNeedUpdate = soraCardInteractor.needInstallUpdate(),
            )
        }

        soraCardInteractor.subscribeSoraCardStatus()
            .onEach {
                soraCardInfo = it
                ibanInfo = soraCardInteractor.fetchUserIbanAccount()
                val soraCardStatusStringRes =
                    if (ibanInfo == null)
                        when (soraCardInfo) {
                            SoraCardCommonVerification.Rejected -> R.string.sora_card_verification_rejected
                            SoraCardCommonVerification.Pending -> R.string.sora_card_verification_in_progress
                            SoraCardCommonVerification.Successful -> R.string.more_menu_sora_card_subtitle
                            else -> R.string.sora_card_sign_in_required
                        } else R.string.more_menu_sora_card_subtitle

                val soraCardStatusIconDrawableRes =
                    if (ibanInfo == null)
                        when (soraCardInfo) {
                            SoraCardCommonVerification.Rejected -> R.drawable.ic_status_denied
                            SoraCardCommonVerification.Pending -> R.drawable.ic_status_pending
                            else -> null
                        } else null

                _state.value = _state.value.copy(
                    soraCardStatusStringRes = soraCardStatusStringRes,
                    soraCardIbanError = if (ibanInfo?.active == false) ibanInfo?.iban else null,
                    soraCardStatusIconDrawableRes = soraCardStatusIconDrawableRes
                )
            }
            .launchIn(viewModelScope)

        soraCardInteractor.subscribeToSoraCardAvailabilityFlow().onEach {
            currentSoraCardContractData = createSoraCardContract(
                userAvailableXorAmount = it.xorBalance.toDouble(),
                isEnoughXorAvailable = it.enoughXor
            )
        }.launchIn(viewModelScope)
    }

    fun showAccountList() {
        router.showAccountList()
    }

    fun showSoraCard() {
        if (ibanInfo?.active == true)
            router.showSoraCardDetails()
        else when (soraCardInfo) {
            SoraCardCommonVerification.NotFound -> {
                router.showGetSoraCard()
            }

            SoraCardCommonVerification.Successful -> {
                router.showSoraCardDetails()
            }

            else -> {
                currentSoraCardContractData?.let { contractData ->
                    _launchSoraCardSignIn.value = contractData
                }
            }
        }
    }

    fun handleSoraCardResult(soraCardResult: SoraCardResult) {
        when (soraCardResult) {
            is SoraCardResult.NavigateTo -> {
                when (soraCardResult.screen) {
                    OutwardsScreen.DEPOSIT -> walletRouter.openQrCodeFlow()
                    OutwardsScreen.SWAP -> polkaswapRouter.showSwap(tokenToId = SubstrateOptionsProvider.feeAssetId)
                    OutwardsScreen.BUY -> assetsRouter.showBuyCrypto()
                }
            }

            is SoraCardResult.Success -> {
                soraCardInteractor.setStatus(soraCardResult.status)
            }

            is SoraCardResult.Failure -> {
                soraCardInteractor.setStatus(soraCardResult.status)
            }

            is SoraCardResult.Canceled -> {}
            is SoraCardResult.Logout -> {
                viewModelScope.launch {
                    soraCardInteractor.setLogout()
                }
            }
        }
    }

    fun showBuyCrypto() {
        assetsRouter.showBuyCrypto()
    }

    fun showSelectNode() {
        selectNodeRouter.showSelectNode()
    }

    fun showAppSettings() {
        router.showAppSettings()
    }

    fun showLogin() {
        router.showLoginSecurity()
    }

    fun showReferral() {
        referralRouter.showReferrals()
    }

    fun showAbout() {
        router.showInformation()
    }

    fun showDebugMenu() {
        router.showDebugMenu()
    }
}
