package org.deeplauncher.account

import org.deeplauncher.models.Account

class AccountManager(private val repository: AccountRepository = AccountRepository()) {
    fun listAccounts(): List<Account> = repository.listAccounts()

    fun getActiveAccount(): Account? = repository.getActiveAccount()

    fun selectAccount(uuid: String?) = repository.selectAccount(uuid)

    fun createAccount(username: String): Account = repository.createAccount(username)

    fun deleteAccount(uuid: String): Boolean = repository.deleteAccount(uuid)
}