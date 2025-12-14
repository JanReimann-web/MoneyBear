package com.jan.moneybear.domain

import java.util.UUID

fun newTxId(): String = "tx_${UUID.randomUUID()}"

