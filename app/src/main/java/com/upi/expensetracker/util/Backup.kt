package com.upi.expensetracker.util

import com.upi.expensetracker.data.Budget
import com.upi.expensetracker.data.CategoryIcon
import com.upi.expensetracker.data.CustomCategory
import com.upi.expensetracker.data.MonthlySetting
import com.upi.expensetracker.data.Transaction
import org.json.JSONArray
import org.json.JSONObject

/** Serializes/deserializes the whole database to a JSON backup file. */
object Backup {

    const val VERSION = 1

    data class Data(
        val transactions: List<Transaction>,
        val customCategories: List<CustomCategory>,
        val settings: List<MonthlySetting>,
        val budgets: List<Budget>,
        val categoryIcons: List<CategoryIcon>
    )

    fun toJson(data: Data): String {
        val root = JSONObject()
        root.put("version", VERSION)

        root.put("transactions", JSONArray().apply {
            data.transactions.forEach { t ->
                put(
                    JSONObject()
                        .put("amount", t.amount)
                        .put("payee", t.payee)
                        .put("type", t.type)
                        .put("category", t.category)
                        .put("note", t.note)
                        .put("timestamp", t.timestamp)
                        .put("sender", t.sender)
                        .put("receiptPath", t.receiptPath ?: JSONObject.NULL)
                        .put("receiptText", t.receiptText ?: JSONObject.NULL)
                )
            }
        })

        root.put("customCategories", JSONArray().apply {
            data.customCategories.forEach { c ->
                put(JSONObject().put("name", c.name).put("type", c.type))
            }
        })

        root.put("settings", JSONArray().apply {
            data.settings.forEach { s ->
                put(JSONObject().put("monthKey", s.monthKey).put("openingBalance", s.openingBalance))
            }
        })

        root.put("budgets", JSONArray().apply {
            data.budgets.forEach { b ->
                put(JSONObject().put("monthKey", b.monthKey).put("category", b.category).put("amount", b.amount))
            }
        })

        root.put("categoryIcons", JSONArray().apply {
            data.categoryIcons.forEach { i ->
                put(
                    JSONObject()
                        .put("category", i.category)
                        .put("iconKey", i.iconKey ?: JSONObject.NULL)
                        .put("colorHex", i.colorHex ?: JSONObject.NULL)
                )
            }
        })

        return root.toString()
    }

    fun fromJson(json: String): Data {
        val root = JSONObject(json)

        val transactions = root.optJSONArray("transactions").mapObjects {
            Transaction(
                amount = it.getDouble("amount"),
                payee = it.optString("payee"),
                type = it.optString("type", "DEBIT"),
                category = it.optString("category", "Uncategorized"),
                note = it.optString("note", ""),
                timestamp = it.getLong("timestamp"),
                sender = it.optString("sender", ""),
                receiptPath = if (it.isNull("receiptPath")) null else it.optString("receiptPath").ifBlank { null },
                receiptText = if (it.isNull("receiptText")) null else it.optString("receiptText").ifBlank { null }
            )
        }
        val customCategories = root.optJSONArray("customCategories").mapObjects {
            CustomCategory(name = it.getString("name"), type = it.getString("type"))
        }
        val settings = root.optJSONArray("settings").mapObjects {
            MonthlySetting(monthKey = it.getString("monthKey"), openingBalance = it.getDouble("openingBalance"))
        }
        val budgets = root.optJSONArray("budgets").mapObjects {
            Budget(monthKey = it.getString("monthKey"), category = it.getString("category"), amount = it.getDouble("amount"))
        }
        val categoryIcons = root.optJSONArray("categoryIcons").mapObjects {
            CategoryIcon(
                category = it.getString("category"),
                iconKey = if (it.isNull("iconKey")) null else it.optString("iconKey"),
                colorHex = if (it.isNull("colorHex")) null else it.optString("colorHex")
            )
        }

        return Data(transactions, customCategories, settings, budgets, categoryIcons)
    }

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) out.add(transform(getJSONObject(i)))
        return out
    }
}
