package com.sportscenter.feedback;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.sportclass.*;
import com.sportscenter.user.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.validation.Validation;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassFeedbackAuditTest {
    @Mock ClassFeedbackRepository feedbackRepository;
    @Mock SportsClassRepository sportsClassRepository;
    @Mock ClassEnrollmentRepository enrollmentRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @InjectMocks ClassFeedbackService service;
    User member,coach; SportsClass cls;
    final ClassFeedbackRequest request=new ClassFeedbackRequest(1,2,(short)5,(short)4,"Good class");
    final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("audit@example.com","n/a");
    @BeforeEach void setup() {
        member=new User(); member.setId(7); coach=new User(); coach.setId(2);
        cls=new SportsClass(); cls.setId(1); cls.setCoach(coach); cls.setStatus("Ongoing");
    }
    void classLookup() {
        when(userRepository.findByEmailIgnoreCase(auth.getName())).thenReturn(Optional.of(member));
        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(cls));
    }
    void eligible() {
        classLookup();
        when(enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatusIn(7,1,List.of("Registered","Completed"))).thenReturn(true);
        when(feedbackRepository.hasPresentAttendance(1,7)).thenReturn(true);
    }
    @Test void eligibleMemberCreatesFeedbackAndAudit() {
        eligible();
        when(feedbackRepository.saveAndFlush(any())).thenAnswer(i -> { ClassFeedback f=i.getArgument(0); f.setFeedbackId(9); return f; });
        var result=service.create(1,request,auth);
        assertEquals(9,result.feedbackId());
        ArgumentCaptor<ClassFeedback> captor=ArgumentCaptor.forClass(ClassFeedback.class);
        verify(feedbackRepository).saveAndFlush(captor.capture());
        assertEquals(7,captor.getValue().getMember().getId());
        verify(auditService).log(eq(7),eq("CREATE_CLASS_FEEDBACK"),eq("class_feedbacks"),eq(9),anyString());
    }
    @Test void rejectsPathMismatch() { assertThrows(BusinessException.class,()->service.create(3,request,auth)); verifyNoInteractions(userRepository); }
    @Test void rejectsWrongCoach() { classLookup(); cls.setCoach(member); assertThrows(BusinessException.class,()->service.create(1,request,auth)); }
    @Test void rejectsOpenClass() { classLookup(); cls.setStatus("Open"); assertThrows(AccessDeniedException.class,()->service.create(1,request,auth)); }
    @Test void rejectsMissingRegisteredEnrollment() { classLookup(); assertThrows(AccessDeniedException.class,()->service.create(1,request,auth)); }
    @Test void rejectsMissingPresentAttendance() {
        classLookup(); when(enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatusIn(7,1,List.of("Registered","Completed"))).thenReturn(true);
        assertThrows(AccessDeniedException.class,()->service.create(1,request,auth));
    }
    @Test void duplicateFeedbackIsConflict() { eligible(); when(feedbackRepository.existsBySportsClassIdAndMemberId(1,7)).thenReturn(true); assertThrows(ClassFeedbackConflictException.class,()->service.create(1,request,auth)); }
    @Test void validatesRatingRangeAndCommentLength() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
            assertEquals(3,factory.getValidator().validate(new ClassFeedbackRequest(1,2,(short)0,(short)6,"x".repeat(1001))).size());
        }
    }
    @Test void coachReviewDoesNotExposeMemberIdentityFields() {
        Set<String> fields=new HashSet<>();
        for(var c:CoachFeedbackReviewResponse.class.getRecordComponents()) fields.add(c.getName());
        assertFalse(fields.contains("memberId")); assertFalse(fields.contains("memberName")); assertFalse(fields.contains("email"));
    }
}
