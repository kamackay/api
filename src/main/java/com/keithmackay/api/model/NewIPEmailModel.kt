package com.keithmackay.api.model

import com.keithmackay.api.utils.IPInfo

data class NewIPEmailModel(val info: IPInfo) {
    override fun toString(): String {
        return info.toString()
    }
}

