package com.sportscenter.session;

import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.user.*;
import com.sportscenter.sportclass.SportsClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SessionPrivacyTest {
    @org.mockito.Mock com.sportscenter.membership.CoachCapacityService phaseCapacity;
    @org.mockito.Mock com.sportscenter.session.SessionChangeRepository phaseChanges;
    @org.mockito.Mock com.sportscenter.session.SessionAbsenceService phaseAbsence;
    @org.mockito.Mock com.sportscenter.session.CenterIncidentService phaseIncidents;
    @Mock UserRepository users;
    @Mock SessionRepository sessions;
    @Mock ClassEnrollmentRepository enrollments;
    @Mock com.sportscenter.sportclass.SportsClassRepository classes;
    @InjectMocks SessionService service;
    Authentication auth(String role) { return new UsernamePasswordAuthenticationToken("actor", "", List.of(new SimpleGrantedAuthority(role))); }
    User actor() { var user = new User(); user.setId(7); user.setCenterId(1); when(users.findByEmailIgnoreCase("actor")).thenReturn(Optional.of(user)); return user; }
    Session session(int center, int memberId) {
        var c = new SportsClass(); c.setId(1); c.setCenterId(center);
        var s = new Session(); s.setId(4); s.setSportsClass(c);
        var m = new User(); m.setId(memberId); s.setBookedMember(m);
        when(sessions.findByIdWithDetails(4)).thenReturn(Optional.of(s)); return s;
    }
    @Test void memberCannotSelectAnotherMembersSchedule() {
        actor(); assertThrows(AccessDeniedException.class, () -> service.findAll(null,null,null,8,null,null,null,auth("ROLE_MEMBER"))); verifyNoInteractions(sessions);
    }
    @Test void memberListAlwaysUsesAuthenticatedMemberId() {
        actor(); service.findAll(null,null,null,null,null,null,null,auth("ROLE_MEMBER")); verify(sessions).findByEnrolledMember(7,null,null,null); verify(sessions,never()).findFiltered(any(),any(),any(),any(),any(),any());
    }
    @Test void memberCannotReadAnotherPrivateSession() {
        actor(); session(1,8); assertThrows(AccessDeniedException.class, () -> service.findById(4,auth("ROLE_MEMBER")));
    }
    @Test void coachCannotReadAnotherCoachsSession() {
        actor(); var s = session(1,8); var coach = new User(); coach.setId(9); s.setCoach(coach);
        assertThrows(AccessDeniedException.class, () -> service.findById(4,auth("ROLE_COACH")));
    }
    @Test void staffCannotReadAnotherCenter() {
        actor(); session(2,8); assertThrows(AccessDeniedException.class, () -> service.findById(4,auth("MANAGE_CLASSES")));
    }
    @Test void ownerCanReadOwnPrivateSession() {
        actor(); session(1,7); assertEquals(7,service.findById(4,auth("ROLE_MEMBER")).bookedMemberId());
    }
    @Test void privateClassCannotBeReadThroughTimetableCatalog() {
        var c = new SportsClass(); c.setId(1); c.setClassType("Advanced"); c.setStatus("Open");
        when(classes.findById(1)).thenReturn(Optional.of(c));
        assertThrows(com.sportscenter.common.exception.ResourceNotFoundException.class, () -> service.publicTimetable(1));
    }
}
