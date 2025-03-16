package com.devmh.model;

import lombok.Data;
import java.time.LocalTime;

@Data
class Timetable {
    private int recurrenceDays;
    private LocalTime reviewTime;

    public Timetable(int recurrenceDays, LocalTime reviewTime) {
        this.recurrenceDays = recurrenceDays;
        this.reviewTime = reviewTime;
    }
}
