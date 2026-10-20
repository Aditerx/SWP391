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

    @Transactional
    public HolidayResponse toggleActive(Integer id, Boolean active) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
        if (active != null) {
            holiday.setActive(active);
        } else {
            holiday.setActive(!holiday.isActive());
        }
        return HolidayResponse.from(holidayRepository.save(holiday));
    }

    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        if (holidayRepository.findByHolidayDate(request.holidayDate()).isPresent()) {
            throw new BusinessException("Holiday already exists for date: " + request.holidayDate());
        }
        Holiday holiday = new Holiday();
        holiday.setHolidayDate(request.holidayDate());
        holiday.setHolidayName(request.holidayName());
        holiday.setTag(request.tag() != null ? request.tag() : "CENTER_EVENT");
        holiday.setActive(request.active() != null ? request.active() : true);
        return HolidayResponse.from(holidayRepository.save(holiday));
    }
}

