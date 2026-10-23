package com.sportscenter.session;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** Existing center row used to serialize schedule mutations across classes and subscriptions. */
@Entity
@Table(name = "centers")
public class ScheduleMutex {
    @Id
    @Column(name = "center_id")
    private Integer id;
}
