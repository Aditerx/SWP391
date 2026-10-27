package com.sportscenter.holiday;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayService {
    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public List<HolidayResponse> findAll() {
        return holidayRepository.findAllByOrderByHolidayDateAsc().stream()
                .map(HolidayResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> findActive() {
        return holidayRepository.findByIsActiveTrue().stream()
                .map(HolidayResponse::from)
                .toList();
    }

}
