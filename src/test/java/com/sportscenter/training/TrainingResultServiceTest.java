package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingResultServiceTest {

    @Mock
    private TrainingResultRepository trainingResultRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TrainingResultService trainingResultService;

    private Session sampleSession;
    private User sampleMember;
    private User sampleCoach;

    @BeforeEach
    void setUp() {
        sampleCoach = new User();
        sampleCoach.setId(2);
        sampleCoach.setFullName("Trần Thị Hương");

        sampleMember = new User();
        sampleMember.setId(7);
        sampleMember.setFullName("Hoàng Thị Oanh");

        sampleSession = new Session();
        sampleSession.setId(1);
    }

    @Test
    @DisplayName("Record training result successfully and syncs attendance")
    void recordResult_Success() {
        TrainingResultRequest request = new TrainingResultRequest(1, 7, 2, "Tập rất tốt, đúng kỹ thuật", "Present");

        when(sessionRepository.findById(1)).thenReturn(Optional.of(sampleSession));
        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));
        when(userRepository.findById(2)).thenReturn(Optional.of(sampleCoach));
        when(trainingResultRepository.findBySessionIdAndMemberId(1, 7)).thenReturn(Optional.empty());

        TrainingResult saved = new TrainingResult();
        saved.setResultId(10);
        saved.setSession(sampleSession);
        saved.setMember(sampleMember);
        saved.setCoach(sampleCoach);
        saved.setContent("Tập rất tốt, đúng kỹ thuật");
        saved.setAttendanceStatus("Present");

        when(trainingResultRepository.save(any(TrainingResult.class))).thenReturn(saved);
        when(attendanceRepository.findBySessionIdAndMemberId(1, 7)).thenReturn(Optional.empty());

        TrainingResultResponse response = trainingResultService.recordResult(request);

        assertNotNull(response);
        assertEquals(10, response.resultId());
        assertEquals("Present", response.attendanceStatus());
        assertEquals("Hoàng Thị Oanh", response.memberName());
        verify(trainingResultRepository).save(any(TrainingResult.class));
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Bulk record results for whole session")
    void recordBulkResults_Success() {
        BulkTrainingResultRequest.Item item1 = new BulkTrainingResultRequest.Item(7, "Present", "Tốt", null);
        BulkTrainingResultRequest request = new BulkTrainingResultRequest(1, 2, List.of(item1));

        when(sessionRepository.findById(1)).thenReturn(Optional.of(sampleSession));
        when(userRepository.findById(2)).thenReturn(Optional.of(sampleCoach));
        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));
        when(trainingResultRepository.findBySessionIdAndMemberId(1, 7)).thenReturn(Optional.empty());
        when(trainingResultRepository.save(any(TrainingResult.class))).thenAnswer(i -> {
            TrainingResult tr = i.getArgument(0);
            tr.setResultId(11);
            return tr;
        });
        when(attendanceRepository.findBySessionIdAndMemberId(1, 7)).thenReturn(Optional.empty());

        List<TrainingResultResponse> responses = trainingResultService.recordBulkResults(request);

        assertEquals(1, responses.size());
        assertEquals("Present", responses.get(0).attendanceStatus());
        verify(trainingResultRepository, atLeastOnce()).save(any(TrainingResult.class));
    }
}
