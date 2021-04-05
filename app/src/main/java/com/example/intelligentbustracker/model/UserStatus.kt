package com.example.intelligentbustracker.model

class UserStatus(
    var tracking: Boolean = false,
    var busNumber: Int = 0,
    var status: Status = Status.WAITING_FOR_BUS,
    var direction: Direction = Direction.DIRECTION_1
)