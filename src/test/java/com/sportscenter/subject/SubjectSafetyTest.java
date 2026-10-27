package com.sportscenter.subject;

import com.sportscenter.audit.AuditService;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.specialization.SpecializationRepository;
import com.sportscenter.common.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectSafetyTest {
    @Mock SubjectRepository repository;
    @Mock AuditService audit;
    @Mock SessionRepository sessions;
    @Mock SportsClassRepository classes;
    @Mock SpecializationRepository specializations;
    @org.mockito.Spy java.time.Clock clock = java.time.Clock.system(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    @InjectMocks SubjectService service;
    @Test void approvedFootballNameProducesItsCanonicalSlugWithoutClientSlug() {
        when(repository.save(any())).thenAnswer(c->c.getArgument(0));
        var response=service.create(new SubjectRequest("Bóng đá",null,"Active",null,new java.math.BigDecimal("1000000"),new java.math.BigDecimal("3500000"),true));
        assertEquals("bong-da",response.slug());assertTrue(response.outdoor());
    }
    @Test void nonCatalogSportIsRejectedByD02() {
        assertThrows(BusinessException.class, () -> service.create(new SubjectRequest("A new sport",null,"Inactive",null,java.math.BigDecimal.TEN,java.math.BigDecimal.TEN,true)));
        verify(repository,never()).save(any());
    }
    @Test void missingIndoorOutdoorConfigurationIsRejected() {
        assertThrows(BusinessException.class, () -> service.create(new SubjectRequest("A new sport",null,"Inactive",null)));
        verify(repository,never()).save(any());
    }
    @Test void deletingLinkedSubjectDoesNotDeleteHistory() {
        when(classes.existsBySubjectId(3)).thenReturn(true);
        assertThrows(BusinessConflictException.class, () -> service.delete(3)); verify(repository,never()).delete(any());
    }
    @Test void unusedSubjectCanBeDeleted() {
        var subject = new Subject(); subject.setId(3);
        when(repository.findById(3)).thenReturn(Optional.of(subject));
        service.delete(3); verify(repository).delete(subject);
    }
}
