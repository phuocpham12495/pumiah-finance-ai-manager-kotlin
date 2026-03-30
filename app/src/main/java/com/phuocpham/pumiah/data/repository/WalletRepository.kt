package com.phuocpham.pumiah.data.repository

import com.phuocpham.pumiah.data.model.UserProfile
import com.phuocpham.pumiah.data.model.Wallet
import com.phuocpham.pumiah.data.model.WalletParticipant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(private val client: SupabaseClient) {

    suspend fun getWallets(): Result<List<Wallet>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")

        // Get wallet IDs the user participates in
        val participations = client.postgrest["wallet_participants"]
            .select { filter { eq("user_id", userId) } }
            .decodeList<WalletParticipant>()

        val walletIds = participations.map { it.walletId }
        if (walletIds.isEmpty()) return@runCatching emptyList()

        // Collect all user IDs across wallets to batch-fetch emails
        val allUserIds = participations.map { it.userId }.toMutableSet()
        walletIds.forEach { wid ->
            val ps = client.postgrest["wallet_participants"]
                .select { filter { eq("wallet_id", wid) } }
                .decodeList<WalletParticipant>()
            allUserIds.addAll(ps.map { it.userId })
        }
        val emailMap: Map<String, String> = runCatching {
            client.postgrest["users"]
                .select()
                .decodeList<UserProfile>()
                .filter { it.id in allUserIds }
                .associate { it.id to it.email }
        }.getOrDefault(emptyMap())

        // Fetch each wallet and attach participant list with emails
        walletIds.map { wid ->
            val wallet = client.postgrest["wallets"]
                .select { filter { eq("id", wid) } }
                .decodeSingle<Wallet>()
            val participants = client.postgrest["wallet_participants"]
                .select { filter { eq("wallet_id", wid) } }
                .decodeList<WalletParticipant>()
                .map { it.copy(email = emailMap[it.userId] ?: it.userId.take(8) + "...") }
            wallet.copy(participants = participants, myRole = participations.find { it.walletId == wid }?.role ?: "co-owner")
        }
    }

    suspend fun createWallet(name: String, type: String = "personal"): Result<Wallet> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")

        // Use RPC to bypass RLS (SECURITY DEFINER function sets created_by = auth.uid() server-side)
        val result = client.postgrest.rpc("create_wallet", buildJsonObject {
            put("wallet_name", name)
            put("wallet_type", type)
        }).data

        val json = kotlinx.serialization.json.Json.parseToJsonElement(result).jsonObject
        val walletId = json["id"]?.jsonPrimitive?.content ?: error("No wallet id returned")

        Wallet(
            id = walletId,
            name = json["name"]?.jsonPrimitive?.content ?: name,
            type = json["type"]?.jsonPrimitive?.content ?: type,
            createdBy = userId,
            myRole = "owner",
            participants = listOf(WalletParticipant(walletId = walletId, userId = userId, role = "owner", email = ""))
        )
    }

    suspend fun inviteParticipant(walletId: String, email: String, role: String): Result<Unit> = runCatching {
        val targetUser = client.postgrest["users"]
            .select { filter { eq("email", email) } }
            .decodeList<com.phuocpham.pumiah.data.model.UserProfile>()
            .firstOrNull() ?: error("Không tìm thấy người dùng với email: $email")

        client.postgrest["wallet_participants"]
            .insert(buildJsonObject {
                put("wallet_id", walletId)
                put("user_id", targetUser.id)
                put("role", role)
            })
    }

    suspend fun removeParticipant(walletId: String, userId: String): Result<Unit> = runCatching {
        client.postgrest["wallet_participants"]
            .delete {
                filter {
                    eq("wallet_id", walletId)
                    eq("user_id", userId)
                }
            }
    }

    suspend fun updateParticipantRole(walletId: String, userId: String, newRole: String): Result<Unit> = runCatching {
        client.postgrest["wallet_participants"]
            .update(buildJsonObject { put("role", newRole) }) {
                filter {
                    eq("wallet_id", walletId)
                    eq("user_id", userId)
                }
            }
    }

    suspend fun deleteWallet(walletId: String): Result<Unit> = runCatching {
        client.postgrest["wallets"]
            .delete { filter { eq("id", walletId) } }
    }

    suspend fun leaveWallet(walletId: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        removeParticipant(walletId, userId).getOrThrow()
    }
}
