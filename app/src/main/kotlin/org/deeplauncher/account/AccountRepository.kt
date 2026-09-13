package org.deeplauncher.account

import kotlinx.serialization.Serializable
import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.core.json
import org.deeplauncher.models.Account
import java.io.File
import java.util.UUID

class AccountRepository {
    private val file = File(LauncherFiles.rootDir, "accounts.json")

    @Serializable
    private data class AccountLibrary(
        val active: String? = null,
        val accounts: List<Account> = emptyList()
    )

    fun listAccounts(): List<Account> = load().accounts

    fun getActiveAccount(): Account? {
        val active = load().active ?: return null
        return listAccounts().firstOrNull { it.uuid == active }
    }

    fun selectAccount(uuid: String?) {
        val library = load()
        save(library.copy(active = uuid))
    }

    fun createAccount(username: String): Account {
        val clean = username.trim()
        require(clean.isNotEmpty()) { "Nickname cannot be empty." }
        require(clean.length <= 16) { "Nickname must be at most 16 characters." }

        val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$clean".toByteArray()).toString()
        val library = load()
        require(library.accounts.none { it.username == clean }) {
            "An account with the nickname '$clean' already exists."
        }

        val account = Account(uuid = uuid, username = clean)
        save(library.copy(accounts = library.accounts + account, active = uuid))
        return account
    }

    fun deleteAccount(uuid: String): Boolean {
        val library = load()
        val remaining = library.accounts.filterNot { it.uuid == uuid }
        if (remaining.size == library.accounts.size) return false

        val active = if (library.active == uuid) null else library.active
        save(library.copy(accounts = remaining, active = active))
        return true
    }

    private fun load(): AccountLibrary =
        runCatching { json.decodeFromString<AccountLibrary>(file.readText()) }
            .getOrDefault(AccountLibrary())

    private fun save(library: AccountLibrary) {
        file.writeText(json.encodeToString(AccountLibrary.serializer(), library))
    }
}