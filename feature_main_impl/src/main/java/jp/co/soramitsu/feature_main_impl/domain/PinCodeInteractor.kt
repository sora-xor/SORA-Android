/**
* Copyright Soramitsu Co., Ltd. All Rights Reserved.
* SPDX-License-Identifier: GPL-3.0
*/

package jp.co.soramitsu.feature_main_impl.domain

import jp.co.soramitsu.common.domain.credentials.CredentialsRepository
import jp.co.soramitsu.feature_account_api.domain.interfaces.UserRepository
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletRepository
import javax.inject.Inject

class PinCodeInteractor @Inject constructor(
    private val userRepository: UserRepository,
    private val credentialsRepository: CredentialsRepository,
    private val walletRepository: WalletRepository
) {

    suspend fun savePin(pin: String) {
        return userRepository.savePin(pin)
    }

    suspend fun checkPin(code: String) = userRepository.retrievePin() == code

    suspend fun isCodeSet(): Boolean {
        return userRepository.retrievePin().isNotEmpty()
    }

    suspend fun resetUser() {
        userRepository.clearUserData()
    }

    suspend fun setBiometryAvailable(isBiometryAvailable: Boolean) {
        userRepository.setBiometryAvailable(isBiometryAvailable)
    }

    suspend fun isBiometryAvailable(): Boolean {
        return userRepository.isBiometryAvailable()
    }

    suspend fun isBiometryEnabled(): Boolean {
        return userRepository.isBiometryEnabled()
    }

    suspend fun needsMigration(): Boolean {
        val soraAccount = userRepository.getCurSoraAccount()
        val isFetched = userRepository.isMigrationFetched(soraAccount)
        return if (isFetched) {
            userRepository.needsMigration(soraAccount)
        } else {
            val irohaData = credentialsRepository.getIrohaData(soraAccount)
            val needs = walletRepository.needsMigration(irohaData.address)
            userRepository.saveNeedsMigration(needs, soraAccount)
            userRepository.saveIsMigrationFetched(true, soraAccount)
            userRepository.needsMigration(soraAccount)
        }
    }

    suspend fun setBiometryEnabled(isEnabled: Boolean) {
        userRepository.setBiometryEnabled(isEnabled)
    }
}
