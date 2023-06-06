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

package jp.co.soramitsu.common.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SoraPreferences(
    context: Context,
) {

    companion object {
        private const val SHARED_PREFERENCES_FILE = "sora_prefs"
        private const val SORA_COMMON_PREFS = "sora_prefs_datastore"
    }

    private val Context.dataStorePreferences: DataStore<Preferences> by preferencesDataStore(
        name = SORA_COMMON_PREFS,
        produceMigrations = {
            listOf(
                SharedPreferencesMigration(it, SHARED_PREFERENCES_FILE)
            )
        }
    )

    private val dataStore = context.dataStorePreferences

    suspend fun putString(field: String, value: String) {
        dataStore.edit {
            it[stringPreferencesKey(field)] = value
        }
    }

    suspend fun clear(field: String) {
        dataStore.edit {
            it.remove(stringPreferencesKey(field))
        }
    }

    suspend fun clear(fields: List<String>) {
        dataStore.edit {
            fields.forEach { field ->
                it.remove(stringPreferencesKey(field))
            }
        }
    }

    suspend fun getOrPutInt(field: String, value: Int): Int =
        dataStore.data.map {
            val key = intPreferencesKey(field)
            val result = if (it.contains(key)) {
                it[key] ?: value
            } else {
                it.toMutablePreferences()[key] = value
                value
            }
            result
        }.first()

    suspend fun getString(field: String): String =
        dataStore.data.map {
            it[stringPreferencesKey(field)] ?: ""
        }.first()

    fun getBooleanFlow(field: String, defaultValue: Boolean): Flow<Boolean> =
        dataStore.data.map {
            it[booleanPreferencesKey(field)] ?: defaultValue
        }

    suspend fun getBoolean(field: String, defaultValue: Boolean = false): Boolean =
        dataStore.data.map {
            it[booleanPreferencesKey(field)] ?: defaultValue
        }.first()

    suspend fun putBoolean(field: String, value: Boolean) {
        dataStore.edit {
            it[booleanPreferencesKey(field)] = value
        }
    }

    suspend fun getInt(field: String, defaultValue: Int): Int =
        dataStore.data.map {
            it[intPreferencesKey(field)] ?: defaultValue
        }.first()

    fun getIntFlow(field: String, defaultValue: Int): Flow<Int> =
        dataStore.data.map {
            it[intPreferencesKey(field)] ?: defaultValue
        }

    suspend fun putInt(field: String, value: Int) {
        dataStore.edit {
            it[intPreferencesKey(field)] = value
        }
    }

    suspend fun getLong(field: String, defaultValue: Long): Long =
        dataStore.data.map {
            it[longPreferencesKey(field)] ?: defaultValue
        }.first()

    suspend fun putLong(field: String, value: Long) {
        dataStore.edit {
            it[longPreferencesKey(field)] = value
        }
    }

    suspend fun getFloat(field: String, defaultValue: Float): Float =
        dataStore.data.map {
            it[floatPreferencesKey(field)] ?: defaultValue
        }.first()

    suspend fun putFloat(field: String, value: Float) {
        dataStore.edit {
            it[floatPreferencesKey(field)] = value
        }
    }

    suspend fun getDouble(field: String, defaultValue: Double): Double =
        dataStore.data.map {
            java.lang.Double.longBitsToDouble(
                it[longPreferencesKey(field)] ?: java.lang.Double.doubleToLongBits(defaultValue)
            )
        }.first()

    suspend fun putDouble(field: String, value: Double) {
        dataStore.edit {
            it[longPreferencesKey(field)] = java.lang.Double.doubleToRawLongBits(value)
        }
    }

    suspend fun clearAll() {
        dataStore.edit {
            it.clear()
        }
    }
}
