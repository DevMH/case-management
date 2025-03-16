package com.devmh.model;

import lombok.Data;
import java.time.LocalDate;

@Data
class Finding {
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean sensitive;

    public Finding(LocalDate startDate, LocalDate endDate, boolean sensitive) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.sensitive = sensitive;
    }
}
