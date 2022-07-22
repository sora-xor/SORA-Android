/**
* Copyright Soramitsu Co., Ltd. All Rights Reserved.
* SPDX-License-Identifier: GPL-3.0
*/

package jp.co.soramitsu.feature_wallet_impl.presentation.polkaswap.swap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.soramitsu.common.domain.Asset
import jp.co.soramitsu.common.domain.AssetHolder
import jp.co.soramitsu.common.domain.SuspendableProperty
import jp.co.soramitsu.common.domain.Token
import jp.co.soramitsu.common.presentation.AssetBalanceStyle
import jp.co.soramitsu.common.presentation.SingleLiveEvent
import jp.co.soramitsu.common.presentation.view.assetselectbottomsheet.adapter.AssetListItemModel
import jp.co.soramitsu.common.presentation.viewmodel.BaseViewModel
import jp.co.soramitsu.common.resourses.ResourceManager
import jp.co.soramitsu.common.util.NumbersFormatter
import jp.co.soramitsu.common.util.ext.zipWith
import jp.co.soramitsu.common.view.ViewHelper
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.PolkaswapInteractor
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletInteractor
import jp.co.soramitsu.feature_wallet_api.domain.model.Market
import jp.co.soramitsu.feature_wallet_api.domain.model.SwapDetails
import jp.co.soramitsu.feature_wallet_api.launcher.WalletRouter
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.feature_wallet_impl.presentation.polkaswap.liquidity.model.ButtonState
import jp.co.soramitsu.feature_wallet_impl.presentation.util.mapAssetToAssetModel
import jp.co.soramitsu.feature_wallet_impl.util.PolkaswapMath.isZero
import jp.co.soramitsu.sora.substrate.models.WithDesired
import jp.co.soramitsu.sora.substrate.runtime.SubstrateOptionsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
class SwapViewModel @Inject constructor(
    private val router: WalletRouter,
    private val walletInteractor: WalletInteractor,
    private val polkaswapInteractor: PolkaswapInteractor,
    private val numbersFormatter: NumbersFormatter,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    companion object {
        const val ROUNDING_SWAP = 7
    }

    private val assetsList = mutableListOf<Asset>()
    private var feeAsset: Asset? = null

    private class PropertyValue(var reloadMarkets: Boolean = false) {
        @Synchronized
        fun reset() {
            reloadMarkets = false
        }

        @Synchronized
        fun newReloadMarkets(v: Boolean): PropertyValue {
            reloadMarkets = reloadMarkets or v
            return this
        }
    }

    private val property = PropertyValue()

    private val onChangedProperty = SuspendableProperty<PropertyValue>(1)

    private val _showFromAssetSelectBottomSheet = SingleLiveEvent<List<AssetListItemModel>>()
    val showFromAssetSelectBottomSheet: LiveData<List<AssetListItemModel>> =
        _showFromAssetSelectBottomSheet

    private val _showToAssetSelectBottomSheet = SingleLiveEvent<List<AssetListItemModel>>()
    val showToAssetSelectBottomSheet: LiveData<List<AssetListItemModel>> =
        _showToAssetSelectBottomSheet

    private val _showSlippageToleranceBottomSheet = SingleLiveEvent<Float>()
    val showSlippageToleranceBottomSheet: LiveData<Float> = _showSlippageToleranceBottomSheet

    private val _fromAssetLiveData = MutableLiveData<Asset>()
    val fromAssetLiveData: LiveData<Asset> = _fromAssetLiveData

    private val _toAssetLiveData = MutableLiveData<Asset>()
    val toAssetLiveData: LiveData<Asset> = _toAssetLiveData

    val fromAndToAssetLiveData = _fromAssetLiveData.zipWith(_toAssetLiveData)

    private val _slippageToleranceLiveData = MutableLiveData<Float>()
    val slippageToleranceLiveData: LiveData<Float> = _slippageToleranceLiveData

    private val _detailsShowLiveData = MutableLiveData(false)
    val detailsShowLiveData: LiveData<Boolean> = _detailsShowLiveData

    private val _swapButtonState: MutableStateFlow<ButtonState> = MutableStateFlow(ButtonState())
    val swapButtonState: StateFlow<ButtonState> = _swapButtonState

    private val _toEnabledLiveData = MutableLiveData<Boolean>()
    val toEnabledLiveData: LiveData<Boolean> = _toEnabledLiveData

    private val _fromEnabledLiveData = MutableLiveData<Boolean>()
    val fromEnabledLiveData: LiveData<Boolean> = _fromEnabledLiveData

    private val _detailsEnabledLiveData = MutableLiveData(false)
    val detailsEnabledLiveData: LiveData<Boolean> = _detailsEnabledLiveData

    private val _fromAmountLiveData = MutableLiveData<String>()
    val fromAmountLiveData: LiveData<String> = _fromAmountLiveData

    private val _toAmountLiveData = MutableLiveData<String>()
    val toAmountLiveData: LiveData<String> = _toAmountLiveData

    private val _fromBalanceLiveData = MutableLiveData<Pair<String, String>>()
    val fromBalanceLiveData: LiveData<Pair<String, String>> = _fromBalanceLiveData

    private val _toBalanceLiveData = MutableLiveData<Pair<String, String>>()
    val toBalanceLiveData: LiveData<Pair<String, String>> = _toBalanceLiveData

    private val _detailsPriceValue = MutableLiveData<Pair<String?, String?>>()
    val detailsPriceValue: LiveData<Pair<String?, String?>> = _detailsPriceValue

    private val _minmaxLiveData = MutableLiveData<Pair<Pair<String, String>?, String>>()
    val minmaxLiveData: LiveData<Pair<Pair<String, String>?, String>> = _minmaxLiveData

    private val _liquidityFeeLiveData = MutableLiveData<Pair<String, String>?>()
    val liquidityFeeLiveData: LiveData<Pair<String, String>?> = _liquidityFeeLiveData

    private val _networkFeeLiveData = MutableLiveData<Pair<String, String>?>()
    val networkFeeLiveData: LiveData<Pair<String, String>?> = _networkFeeLiveData

    private val _minmaxClickLiveData = SingleLiveEvent<Pair<Int, Int>>()
    val minmaxClickLiveData: LiveData<Pair<Int, Int>> = _minmaxClickLiveData

//    private val _preloaderEventLiveData = SingleLiveEvent<Boolean>()
//    val preloaderEventLiveData: LiveData<Boolean> = _preloaderEventLiveData

    private val _disclaimerVisibilityLiveData = MutableLiveData<Boolean>()
    val disclaimerVisibilityLiveData: LiveData<Boolean> = _disclaimerVisibilityLiveData

    private val _dataInitiatedEvent = SingleLiveEvent<Unit>()
    val dataInitiatedEvent = _dataInitiatedEvent

    private val availableMarkets = mutableListOf<Market>()
    private var amountFrom: BigDecimal = BigDecimal.ZERO
    private var amountTo: BigDecimal = BigDecimal.ZERO
    private var desired: WithDesired = WithDesired.INPUT
    private var swapDetails: SwapDetails? = null
    private var networkFee: BigDecimal? = null
    private val balanceStyle = AssetBalanceStyle(
        R.style.TextAppearance_Soramitsu_Neu_Bold_15,
        R.style.TextAppearance_Soramitsu_Neu_Bold_11
    )

    init {
        _slippageToleranceLiveData.value = 0.5f
        _swapButtonState.value = _swapButtonState.value.copy(
            text = resourceManager.getString(R.string.choose_tokens)
        )
        polkaswapInteractor.getPolkaswapDisclaimerVisibility()
            .catch {
                onError(it)
            }
            .onEach {
                _disclaimerVisibilityLiveData.value = it
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            polkaswapInteractor.observeSelectedMarket()
                .catch {
                    onError(it)
                }
                .collectLatest {
                    onChangedProperty.set(property.newReloadMarkets(false))
                }
        }
        polkaswapInteractor.observeSwap()
            .catch {
                onError(it)
            }
            .onEach {
                amountFrom = BigDecimal.ZERO
                amountTo = BigDecimal.ZERO
                _fromAmountLiveData.value = ""
                _toAmountLiveData.value = ""
                swapDetails = null
                updateDetailsView()
                onChangedProperty.set(property.newReloadMarkets(false))
            }
            .launchIn(viewModelScope)
        walletInteractor.subscribeActiveAssetsOfCurAccount()
            .catch { onError(it) }
            .distinctUntilChanged()
            .onEach { assets ->
                assetsList.clear()
                assetsList.addAll(assets)

                assets.find { it.token.id == SubstrateOptionsProvider.feeAssetId }?.let {
                    feeAsset = it
                    if (_fromAssetLiveData.value == null) {
                        _fromAssetLiveData.value = it
                    }
                }

                _fromAssetLiveData.value?.let { asset ->
                    assets.find { it.token.id == asset.token.id }?.let {
                        _fromBalanceLiveData.value = numbersFormatter.formatBigDecimal(
                            it.balance.transferable,
                            AssetHolder.ROUNDING
                        ) to it.token.symbol
                    }
                }

                _toAssetLiveData.value?.let { asset ->
                    assets.find { it.token.id == asset.token.id }?.let {
                        _toBalanceLiveData.value = numbersFormatter.formatBigDecimal(
                            it.balance.transferable,
                            AssetHolder.ROUNDING
                        ) to it.token.symbol
                    }
                }

                if (networkFee == null) {
                    assets.find { it.token.id == SubstrateOptionsProvider.feeAssetId }?.let {
                        networkFee = polkaswapInteractor.fetchSwapNetworkFee(it.token)
                    }
                }

                onChangedProperty.set(property.newReloadMarkets(false))
                _dataInitiatedEvent.call()
            }
            .launchIn(viewModelScope)

        polkaswapInteractor.observePoolReserves()
            .catch {
                onError(it)
            }
            .onEach {
                onChangedProperty.set(property.newReloadMarkets(false))
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            onChangedProperty.observe()
                .debounce(ViewHelper.debounce)
                .catch {
                    onError(it)
                }
                .collectLatest {
                    if (it.reloadMarkets) getMarkets()
                    recalcDetails()
                    toggleSwapButtonStatus()
                    toggleDetailsStatus()
                    resetLoading()
                    property.reset()
                }
        }
    }

    fun fromCardClicked() {
        if (assetsList.isNotEmpty()) {
            _showFromAssetSelectBottomSheet.value =
                assetsList.filter { it.token.id != _toAssetLiveData.value?.token?.id }
                    .map { it.mapAssetToAssetModel(numbersFormatter, balanceStyle) }
        }
    }

    fun toCardClicked() {
        if (assetsList.isNotEmpty()) {
            _showToAssetSelectBottomSheet.value =
                assetsList.filter { it.token.id != _fromAssetLiveData.value?.token?.id }
                    .map { it.mapAssetToAssetModel(numbersFormatter, balanceStyle) }
        }
    }

    fun fromAssetSelected(assetModel: AssetListItemModel) {
        assetsList.find { it.token.id == assetModel.assetId }?.let {
            toAndFromAssetsSelected(null, it)
        }
    }

    fun toAssetSelected(assetModel: AssetListItemModel) {
        assetsList.find { it.token.id == assetModel.assetId }?.let {
            toAndFromAssetsSelected(it, null)
        }
    }

    fun setSwapData(fromToken: Token, toToken: Token, inputAmount: BigDecimal) {
        viewModelScope.launch {
            if (_toAssetLiveData.value == null) {
                assetsList.find { it.token.id == fromToken.id }?.let { fromAsset ->
                    assetsList.find { it.token.id == toToken.id }?.let { toAsset ->
                        toAndFromAssetsSelected(toAsset, fromAsset)
                        _fromAmountLiveData.value =
                            numbersFormatter.formatBigDecimal(
                                inputAmount,
                                fromToken.precision
                            )

                        delay(800L)
                        fromAmountChanged(inputAmount)
                        onChangedProperty.set(property.newReloadMarkets(true))
                    }
                }
            }
        }
    }

    private fun toAndFromAssetsSelected(to: Asset?, from: Asset?) {
        to?.let {
            _toAssetLiveData.value = it
            _toBalanceLiveData.value =
                numbersFormatter.formatBigDecimal(
                it.balance.transferable,
                AssetHolder.ROUNDING
            ) to it.token.symbol
        }
        from?.let {
            _fromAssetLiveData.value = it
            _fromBalanceLiveData.value =
                numbersFormatter.formatBigDecimal(
                it.balance.transferable,
                AssetHolder.ROUNDING
            ) to it.token.symbol
        }
        onChangedProperty.set(property.newReloadMarkets(true))
    }

    private fun toggleSwapButtonStatus() {
        val ok = isBalanceOk()
        val (text, enabled) = when {
            _fromAssetLiveData.value == null || _toAssetLiveData.value == null -> {
                resourceManager.getString(R.string.choose_tokens) to false
            }
            availableMarkets.isEmpty() -> {
                resourceManager.getString(R.string.polkaswap_pool_not_created) to false
            }
            _fromAssetLiveData.value != null && amountFrom.isZero() && desired == WithDesired.INPUT -> {
                resourceManager.getString(R.string.common_enter_amount) to false
            }
            _toAssetLiveData.value != null && amountTo.isZero() && desired == WithDesired.OUTPUT -> {
                resourceManager.getString(R.string.common_enter_amount) to false
            }
            ok?.isEmpty() == true -> {
                resourceManager.getString(R.string.polkaswap_swap_title) to true
            }
            ok?.isNotEmpty() == true -> {
                resourceManager.getString(R.string.polkaswap_insufficient_balance)
                    .format(ok) to false
            }
            swapDetails == null -> {
                resourceManager.getString(R.string.polkaswap_insufficient_liqudity)
                    .format("") to false
            }
            else -> {
                resourceManager.getString(R.string.choose_tokens) to false
            }
        }
        _swapButtonState.value = _swapButtonState.value.copy(
            text = text,
            enabled = enabled
        )
    }

    private fun toggleDetailsStatus() {
        _detailsEnabledLiveData.value =
            _fromAssetLiveData.value != null && _toAssetLiveData.value != null
    }

    private suspend fun getMarkets() {
        _fromAssetLiveData.value?.let { from ->
            _toAssetLiveData.value?.let { to ->
                tryCatch {
                    val m = polkaswapInteractor.fetchAvailableSources(from.token.id, to.token.id)
                    availableMarkets.clear()
                    if (!m.isNullOrEmpty()) {
                        availableMarkets.addAll(m)
                    }
                }
            }
        }
    }

    /**
     * @return null - can't calculate, empty - ok, not empty - token symbol
     */
    private fun isBalanceOk(): String? {
        return _fromAssetLiveData.value?.let { fromAsset ->
            _toAssetLiveData.value?.let { toAsset ->
                swapDetails?.let { details ->
                    feeAsset?.let { feeAsset ->
                        if (amountFrom > BigDecimal.ZERO) {
                            val result = polkaswapInteractor.checkSwapBalances(
                                fromToken = fromAsset.token,
                                fromTokenBalance = fromAsset.balance.transferable,
                                fromAmount = amountFrom,
                                swapFee = details.networkFee,
                                feeBalance = feeAsset.balance.transferable,
                                feeToken = feeAsset.token,
                                toToken = toAsset.token,
                                toTokenBalance = toAsset.balance.transferable,
                                toAmount = amountTo,
                                desired = desired,
                                swapDetails = details
                            )
                            when (result) {
                                null -> {
                                    ""
                                }
                                fromAsset.token -> {
                                    fromAsset.token.symbol
                                }
                                else -> {
                                    feeAsset.token.symbol
                                }
                            }
                        } else null
                    }
                }
            }
        }
    }

    private fun setSwapButtonLoading(loading: Boolean) {
        _swapButtonState.value = _swapButtonState.value.copy(loading = loading)
    }

    private fun resetLoading() {
        setSwapButtonLoading(false)
        _fromEnabledLiveData.value = true
        _toEnabledLiveData.value = true
    }

    private suspend fun recalcDetails() {
        feeAsset?.let { feeAsset ->
            _fromAssetLiveData.value?.let { fromAsset ->
                _toAssetLiveData.value?.let { toAsset ->
                    val amountToCalc = if (desired == WithDesired.INPUT) amountFrom else amountTo
                    if (amountToCalc > BigDecimal.ZERO) {
                        tryCatchFinally(
                            finally = {},
                            block = {
                                val details = polkaswapInteractor.calcDetails(
                                    fromAsset.token,
                                    toAsset.token,
                                    feeAsset.token,
                                    amountToCalc,
                                    desired,
                                    _slippageToleranceLiveData.value ?: 0.5f
                                )
                                swapDetails = details
                                updateDetailsView()
                                details?.amount?.let {
                                    if (desired == WithDesired.INPUT) {
                                        _toAmountLiveData.value = numbersFormatter.formatBigDecimal(
                                            it,
                                            toAsset.token.precision
                                        )
                                        amountTo = it
                                    } else {
                                        _fromAmountLiveData.value =
                                            numbersFormatter.formatBigDecimal(
                                                it,
                                                fromAsset.token.precision
                                            )
                                        amountFrom = it
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun updateDetailsView() {
        if (swapDetails == null) {
            _detailsPriceValue.value = null to null
            _minmaxLiveData.value =
                null to resourceManager.getString(R.string.polkaswap_minimum_received)
            _liquidityFeeLiveData.value = null
            _networkFeeLiveData.value = null
        } else {
            swapDetails?.let { details ->
                val per1 = numbersFormatter.formatBigDecimal(details.per1, ROUNDING_SWAP)
                val per2 = numbersFormatter.formatBigDecimal(details.per2, ROUNDING_SWAP)
                val p1: String
                val p2: String
                val minmaxTitle: String
                val minmaxSymbol: String
                if (desired == WithDesired.INPUT) {
                    p1 = per1
                    p2 = per2
                    minmaxTitle = resourceManager.getString(R.string.polkaswap_minimum_received)
                    minmaxSymbol = _toAssetLiveData.value?.token?.symbol.orEmpty()
                } else {
                    p1 = per2
                    p2 = per1
                    minmaxTitle = resourceManager.getString(R.string.polkaswap_maximum_sold)
                    minmaxSymbol = _fromAssetLiveData.value?.token?.symbol.orEmpty()
                }
                _detailsPriceValue.value = p1 to p2
                _minmaxLiveData.value = (
                    numbersFormatter.formatBigDecimal(
                        details.minmax,
                        ROUNDING_SWAP
                    ) to minmaxSymbol
                    ) to minmaxTitle
                _liquidityFeeLiveData.value = numbersFormatter.formatBigDecimal(
                    details.liquidityFee,
                    ROUNDING_SWAP
                ) to feeAsset?.token?.symbol.orEmpty()
                _networkFeeLiveData.value = numbersFormatter.formatBigDecimal(
                    details.networkFee,
                    ROUNDING_SWAP
                ) to feeAsset?.token?.symbol.orEmpty()
            }
        }
    }

    fun fromAmountOnEach() {
        setSwapButtonLoading(true)
        _toEnabledLiveData.value = false
    }

    fun fromAmountChanged(amount: BigDecimal) {
        if (amount == amountFrom && desired == WithDesired.INPUT) {
            resetLoading()
            return
        }
        amountFrom = amount
        desired = WithDesired.INPUT
        onChangedProperty.set(property.newReloadMarkets(false))
    }

    fun toAmountOnEach() {
        setSwapButtonLoading(true)
        _fromEnabledLiveData.value = false
    }

    fun toAmountChanged(amount: BigDecimal) {
        if (amount == amountTo && desired == WithDesired.OUTPUT) {
            resetLoading()
            return
        }
        amountTo = amount
        desired = WithDesired.OUTPUT
        onChangedProperty.set(property.newReloadMarkets(false))
    }

    fun fromAmountFocused() {
        if (desired != WithDesired.INPUT) {
            desired = WithDesired.INPUT
            onChangedProperty.set(property.newReloadMarkets(false))
        }
    }

    fun toAmountFocused() {
        if (desired != WithDesired.OUTPUT) {
            desired = WithDesired.OUTPUT
            onChangedProperty.set(property.newReloadMarkets(false))
        }
    }

    fun swapClicked() {
        swapDetails?.let { details ->
            _fromAssetLiveData.value?.let { fromAsset ->
                _toAssetLiveData.value?.let { toAsset ->
                    feeAsset?.let { fee ->
                        router.showSwapConfirmation(
                            fromAsset.token,
                            amountFrom,
                            toAsset.token,
                            amountTo,
                            desired,
                            details,
                            fee.token,
                            _slippageToleranceLiveData.value ?: 0.5f
                        )
                    }
                }
            }
        }
    }

    fun reverseButtonClicked() {
        _fromAssetLiveData.value?.let { fromAssetModel ->
            _toAssetLiveData.value?.let { toAssetModel ->
                if (desired == WithDesired.INPUT) {
                    amountTo = amountFrom
                    _toAmountLiveData.value = numbersFormatter.formatBigDecimal(
                        amountFrom,
                        fromAssetModel.token.precision
                    )
                    desired = WithDesired.OUTPUT
                } else {
                    amountFrom = amountTo
                    _fromAmountLiveData.value = numbersFormatter.formatBigDecimal(
                        amountTo,
                        toAssetModel.token.precision
                    )
                    desired = WithDesired.INPUT
                }
                toAndFromAssetsSelected(fromAssetModel, toAssetModel)
            }
        }
    }

    fun slippageChanged(slippageTolerance: Float) {
        _slippageToleranceLiveData.value = slippageTolerance
        onChangedProperty.set(property.newReloadMarkets(false))
    }

    fun slippageToleranceClicked() {
        _slippageToleranceLiveData.value?.let {
            _showSlippageToleranceBottomSheet.value = it
        }
    }

    fun detailsClicked() {
        detailsShowLiveData.value?.let {
            _detailsShowLiveData.value = !it && swapDetails != null
        }
    }

    fun infoClicked() {
        router.showPolkaswapInfoFragment()
    }

    fun onMinMaxClicked() {
        _minmaxClickLiveData.value =
            if (desired == WithDesired.INPUT) R.string.polkaswap_minimum_received to R.string.polkaswap_minimum_received_info else R.string.polkaswap_maximum_sold to R.string.polkaswap_maximum_sold_info
    }

    fun fromInputPercentClicked(percent: Int) {
        _fromAssetLiveData.value?.let { assetModel ->
            var amount = assetModel.balance.transferable.divide(
                BigDecimal(100)
            ) * BigDecimal(percent)

            if (assetModel.token.id == SubstrateOptionsProvider.feeAssetId && amount > BigDecimal.ZERO) {
                amount = subtractFee(amount, assetModel.balance.transferable)
            }
            val amountFormatted = numbersFormatter.formatBigDecimal(amount, AssetHolder.ROUNDING)
            _fromAmountLiveData.value = amountFormatted

            amountFrom = amount
            desired = WithDesired.INPUT

            onChangedProperty.set(property.newReloadMarkets(false))
        }
    }

    private fun subtractFee(amount: BigDecimal, balance: BigDecimal): BigDecimal {
        networkFee?.let { fee ->
            return if (amount < fee) {
                amount
            } else {
                if (amount + fee > balance) {
                    amount - fee
                } else {
                    amount
                }
            }
        }
        return amount
    }
}
