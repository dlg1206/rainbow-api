package com.uh.rainbow.dto.response;

import com.uh.rainbow.dto.schedule.ScheduleDTO;

import java.util.List;

/**
 * <b>File:</b> ScheduleResponseDTO.java
 * <p>
 * <b>Description:</b>
 *
 * @author Derek Garcia
 */
public class ScheduleResponseDTO extends ResponseDTO {
    public final List<ScheduleDTO> schedules;

    /**
     * Create new Schedule response with list of valid schedules
     *
     */
    public ScheduleResponseDTO(List<ScheduleDTO> schedules) {
        this.schedules = schedules;
    }
}
