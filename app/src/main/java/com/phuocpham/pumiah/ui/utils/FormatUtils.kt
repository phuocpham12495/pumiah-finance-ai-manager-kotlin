package com.phuocpham.pumiah.ui.utils

import java.text.NumberFormat
import java.util.Locale

private val vndFormat = NumberFormat.getNumberInstance(Locale("vi", "VN")).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

fun formatVnd(amount: Double): String = "${vndFormat.format(amount.toLong())} ₫"

fun formatVndSigned(amount: Double, isIncome: Boolean): String =
    "${if (isIncome) "+" else "-"}${formatVnd(amount)}"

fun friendlyDeleteError(message: String?): String {
    val msg = message?.lowercase() ?: ""
    return when {
        "foreign key" in msg || "violates" in msg || "still referenced" in msg ->
            "Không thể xóa vì dữ liệu này đang được sử dụng ở nơi khác.\nVui lòng xóa các dữ liệu liên quan trước."
        "not found" in msg || "no rows" in msg ->
            "Không tìm thấy dữ liệu cần xóa."
        "permission" in msg || "policy" in msg || "unauthorized" in msg ->
            "Bạn không có quyền thực hiện thao tác này."
        "network" in msg || "connect" in msg || "timeout" in msg ->
            "Không có kết nối mạng. Vui lòng thử lại."
        else -> "Xóa thất bại. Vui lòng thử lại."
    }
}
