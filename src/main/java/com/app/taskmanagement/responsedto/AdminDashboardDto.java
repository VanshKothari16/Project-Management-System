package com.app.taskmanagement.responsedto;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDto {
    private Integer totalManager;
    private Integer totalEmployee;
    private Integer toDoTask;
    private Integer inProgressTask;
    private Integer completedTask;
    private Integer overDueTask;
    private LocalDate deadline;

}
