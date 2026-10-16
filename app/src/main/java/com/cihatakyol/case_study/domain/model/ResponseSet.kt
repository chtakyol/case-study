package com.cihatakyol.case_study.domain.model

data class ResponseSet(val id: Int, val multipleSelection: Boolean, val responses: List<Response>)

data class Response(val id: Int, val label: String, val score: Int?)
